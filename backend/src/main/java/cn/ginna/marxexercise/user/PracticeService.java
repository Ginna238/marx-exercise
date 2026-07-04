package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.AnswerSubmitDTO;
import cn.ginna.marxexercise.common.dto.QuestionDTO;
import cn.ginna.marxexercise.common.entity.*;
import cn.ginna.marxexercise.common.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PracticeService {

    private final QuestionMapper questionMapper;
    private final UserAnswerMapper userAnswerMapper;
    private final UserProgressMapper userProgressMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionBankMapper bankMapper;
    private final ObjectMapper objectMapper;
    private final WrongQuestionService wrongQuestionService;

    public PracticeService(QuestionMapper questionMapper, UserAnswerMapper userAnswerMapper,
                           UserProgressMapper userProgressMapper, WrongQuestionMapper wrongQuestionMapper,
                           QuestionBankMapper bankMapper, ObjectMapper objectMapper,
                           WrongQuestionService wrongQuestionService) {
        this.questionMapper = questionMapper;
        this.userAnswerMapper = userAnswerMapper;
        this.userProgressMapper = userProgressMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.bankMapper = bankMapper;
        this.objectMapper = objectMapper;
        this.wrongQuestionService = wrongQuestionService;
    }

    /**
     * 获取一轮练习的题目（打乱顺序）
     */
    public List<QuestionDTO> getPracticeQuestions(Long userId, Long bankId, String questionType, Integer roundNum) {
        // 获取该题型的所有题目
        List<Question> allQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getBankId, bankId)
                        .eq(Question::getType, questionType));

        // 获取该轮已答过的题目ID
        List<UserAnswer> answered = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>()
                        .eq(UserAnswer::getUserId, userId)
                        .eq(UserAnswer::getBankId, bankId)
                        .eq(UserAnswer::getQuestionType, questionType)
                        .eq(UserAnswer::getRoundNum, roundNum));
        Set<Long> answeredIds = answered.stream().map(UserAnswer::getQuestionId).collect(Collectors.toSet());

        // 过滤出未答题
        List<Question> remaining = allQuestions.stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .collect(Collectors.toList());

        // 打乱顺序
        Collections.shuffle(remaining, new Random(System.currentTimeMillis()));

        // 转换为DTO
        return remaining.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取一轮练习的开始状态
     */
    public Map<String, Object> startPractice(Long userId, Long bankId, String questionType, Integer roundNum) {
        // 获取或创建进度
        UserProgress progress = userProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getBankId, bankId)
                        .eq(UserProgress::getQuestionType, questionType)
                        .eq(UserProgress::getRoundNum, roundNum));

        if (progress == null) {
            int totalCount = questionMapper.selectCount(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getBankId, bankId)
                            .eq(Question::getType, questionType)).intValue();

            progress = new UserProgress();
            progress.setUserId(userId);
            progress.setBankId(bankId);
            progress.setQuestionType(questionType);
            progress.setRoundNum(roundNum);
            progress.setTotalCount(totalCount);
            progress.setAnsweredCount(0);
            progress.setCorrectCount(0);
            progress.setStatus("in_progress");
            userProgressMapper.insert(progress);
        }

        List<QuestionDTO> questions = getPracticeQuestions(userId, bankId, questionType, roundNum);

        // 获取题库信息
        QuestionBank bank = bankMapper.selectById(bankId);

        Map<String, Object> result = new HashMap<>();
        result.put("bank", bank);
        result.put("progress", progress);
        result.put("questions", questions);
        return result;
    }

    /**
     * 开始错题练习：获取用户的错题，可按题库筛选
     */
    public Map<String, Object> startWrongPractice(Long userId, Long bankId) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId);
        if (bankId != null) {
            wrapper.eq(WrongQuestion::getBankId, bankId);
        }
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(wrapper);

        if (wrongList.isEmpty()) {
            throw new RuntimeException("暂无错题");
        }

        List<Long> questionIds = wrongList.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toList());

        List<Question> questions = questionMapper.selectBatchIds(questionIds);

        // 清理已不存在题目的错题记录
        Set<Long> existingIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        List<Long> orphanIds = wrongList.stream()
                .map(WrongQuestion::getQuestionId)
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toList());
        if (!orphanIds.isEmpty()) {
            wrongQuestionMapper.delete(
                    new LambdaQueryWrapper<WrongQuestion>()
                            .eq(WrongQuestion::getUserId, userId)
                            .in(WrongQuestion::getQuestionId, orphanIds));
        }

        // 获取错题次数映射
        Map<Long, Integer> wrongCountMap = wrongList.stream()
                .filter(w -> existingIds.contains(w.getQuestionId()))
                .collect(Collectors.toMap(WrongQuestion::getQuestionId, WrongQuestion::getWrongCount));

        // 打乱顺序
        Collections.shuffle(questions, new Random(System.currentTimeMillis()));

        List<QuestionDTO> questionDTOs = questions.stream().map(q -> {
            QuestionDTO dto = toDTO(q);
            // 将错题次数存入 source 字段临时传递（前端解析）
            return dto;
        }).collect(Collectors.toList());

        // 获取题库信息
        QuestionBank bank = null;
        if (bankId != null) {
            bank = bankMapper.selectById(bankId);
        }
        if (bank == null && !questions.isEmpty()) {
            bank = bankMapper.selectById(questions.get(0).getBankId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bank", bank);
        result.put("questions", questionDTOs);
        result.put("wrongCountMap", wrongCountMap);
        result.put("totalCount", questions.size());
        result.put("wrongPractice", true);
        return result;
    }

    /**
     * 提交答案
     */
    @Transactional
    public Map<String, Object> submitAnswer(Long userId, AnswerSubmitDTO dto) {
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }

        // 判断答案是否正确
        String userAnswer = dto.getSelectedAnswer();
        String correctAnswer = question.getAnswer();
        boolean isCorrect = false;

        if ("judgment".equals(question.getType())) {
            isCorrect = correctAnswer.equals(userAnswer);
        } else if ("single".equals(question.getType())) {
            isCorrect = correctAnswer.equalsIgnoreCase(userAnswer);
        } else if ("multiple".equals(question.getType())) {
            // 多选题：比较排序后的字符串
            String normalizedUser = normalizeMultiAnswer(userAnswer);
            String normalizedCorrect = normalizeMultiAnswer(correctAnswer);
            isCorrect = normalizedUser.equals(normalizedCorrect);
        }

        int isCorrectInt = isCorrect ? 1 : 0;

        // 保存答题记录
        UserAnswer userAnswerRecord = new UserAnswer();
        userAnswerRecord.setUserId(userId);
        userAnswerRecord.setQuestionId(dto.getQuestionId());
        userAnswerRecord.setBankId(dto.getBankId());
        userAnswerRecord.setQuestionType(dto.getQuestionType());
        userAnswerRecord.setSelectedAnswer(userAnswer);
        userAnswerRecord.setIsCorrect(isCorrectInt);
        userAnswerRecord.setRoundNum(dto.getRoundNum());
        userAnswerRecord.setCreatedAt(LocalDateTime.now());
        userAnswerMapper.insert(userAnswerRecord);

        // 更新进度
        updateProgress(userId, dto.getBankId(), dto.getQuestionType(), dto.getRoundNum(), isCorrectInt);

        // 如果是错题，更新错题本
        if (!isCorrect) {
            updateWrongQuestion(userId, question);
        } else {
            // 如果答对了且在错题本中，减少错误计数或移除
            removeFromWrongIfCorrect(userId, question.getId());
        }

        // 获取正确答案选项文本
        String correctAnswerText = getCorrectAnswerText(question);

        // 获取该题的错题次数
        int wrongCount = wrongQuestionService.getWrongCount(userId, question.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("correctAnswer", question.getAnswer());
        result.put("correctAnswerText", correctAnswerText);
        result.put("explanation", getExplanation(question));
        result.put("wrongCount", wrongCount);
        return result;
    }

    /**
     * 检查是否已完成一轮
     */
    public Map<String, Object> checkRoundComplete(Long userId, Long bankId, String questionType, Integer roundNum) {
        UserProgress progress = userProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getBankId, bankId)
                        .eq(UserProgress::getQuestionType, questionType)
                        .eq(UserProgress::getRoundNum, roundNum));

        boolean completed = progress != null && progress.getAnsweredCount() >= progress.getTotalCount();

        Map<String, Object> result = new HashMap<>();
        result.put("completed", completed);
        result.put("progress", progress);
        return result;
    }

    /**
     * 获取建议的轮次号（如果当前轮已完成则返回下一轮）
     */
    public Integer getSuggestedRound(Long userId, Long bankId, String questionType) {
        // 查找最大轮次
        UserProgress latestProgress = userProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getBankId, bankId)
                        .eq(UserProgress::getQuestionType, questionType)
                        .orderByDesc(UserProgress::getRoundNum)
                        .last("LIMIT 1"));

        if (latestProgress == null) {
            return 1; // 还没有任何记录，从第1轮开始
        }

        // 如果最新一轮已完成，返回下一轮
        if (latestProgress.getAnsweredCount() >= latestProgress.getTotalCount()) {
            return latestProgress.getRoundNum() + 1;
        }

        // 否则继续当前轮
        return latestProgress.getRoundNum();
    }

    /**
     * 获取回答正确的题目解析（正确答案文本）
     */
    private String getCorrectAnswerText(Question question) {
        if ("judgment".equals(question.getType())) {
            return question.getAnswer();
        }
        if (question.getOptions() != null) {
            try {
                List<Map<String, String>> options = objectMapper.readValue(question.getOptions(),
                        new TypeReference<List<Map<String, String>>>() {});
                String answer = question.getAnswer();
                StringBuilder sb = new StringBuilder();
                for (char c : answer.toCharArray()) {
                    String label = String.valueOf(c);
                    for (Map<String, String> opt : options) {
                        if (opt.get("label").equals(label)) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(opt.get("text"));
                            break;
                        }
                    }
                }
                return sb.toString();
            } catch (Exception e) {
                return question.getAnswer();
            }
        }
        return question.getAnswer();
    }

    private String getExplanation(Question question) {
        StringBuilder sb = new StringBuilder("正确答案：");
        sb.append(question.getAnswer());
        String text = getCorrectAnswerText(question);
        if (!text.equals(question.getAnswer())) {
            sb.append(" (").append(text).append(")");
        }
        return sb.toString();
    }

    private String normalizeMultiAnswer(String answer) {
        if (answer == null || answer.isEmpty()) return "";
        char[] chars = answer.toUpperCase().toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    private void updateProgress(Long userId, Long bankId, String questionType, Integer roundNum, int isCorrect) {
        UserProgress progress = userProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getBankId, bankId)
                        .eq(UserProgress::getQuestionType, questionType)
                        .eq(UserProgress::getRoundNum, roundNum));

        if (progress != null) {
            progress.setAnsweredCount(progress.getAnsweredCount() + 1);
            progress.setCorrectCount(progress.getCorrectCount() + isCorrect);
            if (progress.getAnsweredCount() >= progress.getTotalCount()) {
                progress.setStatus("completed");
            }
            userProgressMapper.updateById(progress);
        }
    }

    private void updateWrongQuestion(Long userId, Question question) {
        WrongQuestion wrong = wrongQuestionMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, question.getId()));

        if (wrong == null) {
            wrong = new WrongQuestion();
            wrong.setUserId(userId);
            wrong.setQuestionId(question.getId());
            wrong.setBankId(question.getBankId());
            wrong.setQuestionType(question.getType());
            wrong.setWrongCount(1);
            wrong.setLastWrongAt(LocalDateTime.now());
            wrongQuestionMapper.insert(wrong);
        } else {
            wrong.setWrongCount(wrong.getWrongCount() + 1);
            wrong.setLastWrongAt(LocalDateTime.now());
            wrongQuestionMapper.updateById(wrong);
        }
    }

    private void removeFromWrongIfCorrect(Long userId, Long questionId) {
        WrongQuestion wrong = wrongQuestionMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, questionId));
        if (wrong != null) {
            wrongQuestionMapper.deleteById(wrong.getId());
        }
    }

    private QuestionDTO toDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setBankId(question.getBankId());
        dto.setType(question.getType());
        dto.setStem(question.getStem());
        dto.setSource(question.getSource());

        if (question.getOptions() != null) {
            try {
                List<Map<String, String>> options = objectMapper.readValue(question.getOptions(),
                        new TypeReference<List<Map<String, String>>>() {});
                List<String> optionTexts = options.stream()
                        .map(opt -> opt.get("label") + ". " + opt.get("text"))
                        .collect(Collectors.toList());
                dto.setOptions(optionTexts);
            } catch (Exception e) {
                dto.setOptions(new ArrayList<>());
            }
        } else {
            dto.setOptions(new ArrayList<>());
        }

        return dto;
    }
}
