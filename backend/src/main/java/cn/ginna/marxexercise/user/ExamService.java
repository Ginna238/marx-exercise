package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.ExamResultDTO;
import cn.ginna.marxexercise.common.dto.ExamStartDTO;
import cn.ginna.marxexercise.common.dto.ExamSubmitDTO;
import cn.ginna.marxexercise.common.dto.QuestionDTO;
import cn.ginna.marxexercise.common.entity.ExamRecord;
import cn.ginna.marxexercise.common.entity.Question;
import cn.ginna.marxexercise.common.entity.QuestionBank;
import cn.ginna.marxexercise.common.mapper.ExamRecordMapper;
import cn.ginna.marxexercise.common.mapper.QuestionBankMapper;
import cn.ginna.marxexercise.common.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final QuestionMapper questionMapper;
    private final QuestionBankMapper bankMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ObjectMapper objectMapper;
    private final WrongQuestionService wrongQuestionService;

    // 考试配置
    private static final int SINGLE_COUNT = 60;
    private static final int MULTIPLE_COUNT = 10;
    private static final int JUDGMENT_COUNT = 10;
    private static final int SINGLE_SCORE = 1;
    private static final int MULTIPLE_SCORE = 2;
    private static final int JUDGMENT_SCORE = 2;
    private static final int TOTAL_SCORE = SINGLE_COUNT * SINGLE_SCORE
            + MULTIPLE_COUNT * MULTIPLE_SCORE
            + JUDGMENT_COUNT * JUDGMENT_SCORE;

    public ExamService(QuestionMapper questionMapper, QuestionBankMapper bankMapper,
                       ExamRecordMapper examRecordMapper, ObjectMapper objectMapper,
                       WrongQuestionService wrongQuestionService) {
        this.questionMapper = questionMapper;
        this.bankMapper = bankMapper;
        this.examRecordMapper = examRecordMapper;
        this.objectMapper = objectMapper;
        this.wrongQuestionService = wrongQuestionService;
    }

    /**
     * 开始模拟考试：随机抽题，创建考试记录
     */
    @Transactional
    public ExamStartDTO startExam(Long userId, Long bankId) {
        QuestionBank bank = bankMapper.selectById(bankId);
        if (bank == null) {
            throw new RuntimeException("题库不存在");
        }

        // 随机抽取题目
        List<Question> singleQuestions = randomPickQuestions(bankId, "single", SINGLE_COUNT);
        List<Question> multipleQuestions = randomPickQuestions(bankId, "multiple", MULTIPLE_COUNT);
        List<Question> judgmentQuestions = randomPickQuestions(bankId, "judgment", JUDGMENT_COUNT);

        // 检查题目数量是否足够
        int singleActual = singleQuestions.size();
        int multipleActual = multipleQuestions.size();
        int judgmentActual = judgmentQuestions.size();

        if (singleActual == 0 && multipleActual == 0 && judgmentActual == 0) {
            throw new RuntimeException("该题库暂无题目，无法创建模拟考试");
        }

        // 打乱各组题目顺序
        Collections.shuffle(singleQuestions, new Random(System.currentTimeMillis()));
        Collections.shuffle(multipleQuestions, new Random(System.currentTimeMillis()));
        Collections.shuffle(judgmentQuestions, new Random(System.currentTimeMillis()));

        // 计算总分
        int totalScore = singleActual * SINGLE_SCORE
                + multipleActual * MULTIPLE_SCORE
                + judgmentActual * JUDGMENT_SCORE;

        // 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setBankId(bankId);
        record.setScore(0);
        record.setTotalScore(totalScore);
        record.setSingleCorrect(0);
        record.setSingleTotal(singleActual);
        record.setMultipleCorrect(0);
        record.setMultipleTotal(multipleActual);
        record.setJudgmentCorrect(0);
        record.setJudgmentTotal(judgmentActual);
        record.setDurationSeconds(0);
        record.setStatus("in_progress");
        record.setStartedAt(LocalDateTime.now());
        examRecordMapper.insert(record);

        // 构建DTO
        ExamStartDTO dto = new ExamStartDTO();
        dto.setExamId(record.getId());
        dto.setBankName(bank.getName());
        dto.setSingleQuestions(singleQuestions.stream().map(this::toDTO).collect(Collectors.toList()));
        dto.setMultipleQuestions(multipleQuestions.stream().map(this::toDTO).collect(Collectors.toList()));
        dto.setJudgmentQuestions(judgmentQuestions.stream().map(this::toDTO).collect(Collectors.toList()));
        dto.setSingleTotal(singleActual);
        dto.setMultipleTotal(multipleActual);
        dto.setJudgmentTotal(judgmentActual);
        dto.setTotalScore(totalScore);

        return dto;
    }

    /**
     * 提交考试答案并评分
     */
    @Transactional
    public ExamResultDTO submitExam(Long userId, ExamSubmitDTO dto) {
        ExamRecord record = examRecordMapper.selectById(dto.getExamId());
        if (record == null) {
            throw new RuntimeException("考试记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此考试记录");
        }
        if ("completed".equals(record.getStatus())) {
            throw new RuntimeException("该考试已提交，不可重复提交");
        }

        // 获取所有题目答案
        Map<Long, String> submittedAnswers = dto.getAnswers();
        Set<Long> questionIds = submittedAnswers.keySet();

        List<Question> allQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>().in(Question::getId, questionIds));

        Map<Long, Question> questionMap = allQuestions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 按题型分组评分
        List<ExamResultDTO.QuestionResult> singleResults = new ArrayList<>();
        List<ExamResultDTO.QuestionResult> multipleResults = new ArrayList<>();
        List<ExamResultDTO.QuestionResult> judgmentResults = new ArrayList<>();

        int singleCorrect = 0;
        int multipleCorrect = 0;
        int judgmentCorrect = 0;
        int totalScore = 0;

        for (Map.Entry<Long, String> entry : submittedAnswers.entrySet()) {
            Long qid = entry.getKey();
            String userAnswer = entry.getValue() != null ? entry.getValue() : "";

            Question question = questionMap.get(qid);
            if (question == null) continue;

            boolean isCorrect = checkAnswer(question, userAnswer);
            int questionScore = getQuestionScore(question.getType());

            ExamResultDTO.QuestionResult qr = ExamResultDTO.QuestionResult.builder()
                    .questionId(question.getId())
                    .stem(question.getStem())
                    .type(question.getType())
                    .options(parseOptions(question.getOptions()))
                    .correctAnswer(question.getAnswer())
                    .userAnswer(userAnswer)
                    .isCorrect(isCorrect)
                    .score(isCorrect ? questionScore : 0)
                    .build();

            // 答错的题目加入错题本
            if (!isCorrect) {
                wrongQuestionService.addWrongQuestion(userId, question.getId(),
                        question.getBankId(), question.getType());
            }

            switch (question.getType()) {
                case "single":
                    singleResults.add(qr);
                    if (isCorrect) { singleCorrect++; totalScore += questionScore; }
                    break;
                case "multiple":
                    multipleResults.add(qr);
                    if (isCorrect) { multipleCorrect++; totalScore += questionScore; }
                    break;
                case "judgment":
                    judgmentResults.add(qr);
                    if (isCorrect) { judgmentCorrect++; totalScore += questionScore; }
                    break;
            }
        }

        // 更新考试记录
        record.setScore(totalScore);
        record.setSingleCorrect(singleCorrect);
        record.setMultipleCorrect(multipleCorrect);
        record.setJudgmentCorrect(judgmentCorrect);
        record.setDurationSeconds(dto.getDurationSeconds());
        record.setStatus("completed");
        record.setCompletedAt(LocalDateTime.now());
        examRecordMapper.updateById(record);

        // 构建返回结果
        QuestionBank bank = bankMapper.selectById(record.getBankId());
        String bankName = bank != null ? bank.getName() : "未知题库";

        return ExamResultDTO.builder()
                .examId(record.getId())
                .bankName(bankName)
                .score(totalScore)
                .totalScore(record.getTotalScore())
                .singleCorrect(singleCorrect)
                .singleTotal(record.getSingleTotal())
                .multipleCorrect(multipleCorrect)
                .multipleTotal(record.getMultipleTotal())
                .judgmentCorrect(judgmentCorrect)
                .judgmentTotal(record.getJudgmentTotal())
                .durationSeconds(dto.getDurationSeconds())
                .durationText(formatDuration(dto.getDurationSeconds()))
                .singleResults(singleResults)
                .multipleResults(multipleResults)
                .judgmentResults(judgmentResults)
                .build();
    }

    /**
     * 获取考试记录列表
     */
    public List<Map<String, Object>> getExamRecords(Long userId, Long bankId) {
        LambdaQueryWrapper<ExamRecord> query = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, "completed");
        if (bankId != null) {
            query.eq(ExamRecord::getBankId, bankId);
        }
        query.orderByDesc(ExamRecord::getCompletedAt);

        List<ExamRecord> records = examRecordMapper.selectList(query);

        // 获取题库名称
        List<QuestionBank> banks = bankMapper.selectList(null);
        Map<Long, String> bankNames = banks.stream()
                .collect(Collectors.toMap(QuestionBank::getId, QuestionBank::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("bankId", r.getBankId());
            item.put("bankName", bankNames.getOrDefault(r.getBankId(), "未知题库"));
            item.put("score", r.getScore());
            item.put("totalScore", r.getTotalScore());
            item.put("singleCorrect", r.getSingleCorrect());
            item.put("singleTotal", r.getSingleTotal());
            item.put("multipleCorrect", r.getMultipleCorrect());
            item.put("multipleTotal", r.getMultipleTotal());
            item.put("judgmentCorrect", r.getJudgmentCorrect());
            item.put("judgmentTotal", r.getJudgmentTotal());
            item.put("durationSeconds", r.getDurationSeconds());
            item.put("durationText", formatDuration(r.getDurationSeconds()));
            item.put("completedAt", r.getCompletedAt());
            // 计算正确率
            double accuracy = r.getTotalScore() > 0
                    ? (double) r.getScore() / r.getTotalScore() * 100 : 0;
            item.put("accuracy", Math.round(accuracy * 100.0) / 100.0);
            result.add(item);
        }
        return result;
    }

    /**
     * 获取考试统计（总考试次数、平均分等）
     */
    public Map<String, Object> getExamStats(Long userId) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .eq(ExamRecord::getStatus, "completed"));

        int totalExams = records.size();
        int totalScoreSum = records.stream().mapToInt(ExamRecord::getScore).sum();
        double avgScore = totalExams > 0 ? (double) totalScoreSum / totalExams : 0;
        int bestScore = records.stream().mapToInt(ExamRecord::getScore).max().orElse(0);
        int totalDuration = records.stream().mapToInt(ExamRecord::getDurationSeconds).sum();

        // 按题库分组统计
        List<QuestionBank> banks = bankMapper.selectList(null);
        Map<Long, String> bankNames = banks.stream()
                .collect(Collectors.toMap(QuestionBank::getId, QuestionBank::getName, (a, b) -> a));

        Map<Long, List<ExamRecord>> byBank = records.stream()
                .collect(Collectors.groupingBy(ExamRecord::getBankId));

        List<Map<String, Object>> bankStats = new ArrayList<>();
        for (Map.Entry<Long, List<ExamRecord>> entry : byBank.entrySet()) {
            Long bankId = entry.getKey();
            List<ExamRecord> bankRecords = entry.getValue();
            int count = bankRecords.size();
            int bankScoreSum = bankRecords.stream().mapToInt(ExamRecord::getScore).sum();
            double bankAvg = (double) bankScoreSum / count;

            Map<String, Object> bs = new HashMap<>();
            bs.put("bankId", bankId);
            bs.put("bankName", bankNames.getOrDefault(bankId, "未知题库"));
            bs.put("examCount", count);
            bs.put("avgScore", Math.round(bankAvg * 100.0) / 100.0);
            bankStats.add(bs);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExams", totalExams);
        stats.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
        stats.put("bestScore", bestScore);
        stats.put("totalDuration", totalDuration);
        stats.put("totalDurationText", formatDuration(totalDuration));
        stats.put("bankStats", bankStats);

        return stats;
    }

    // ===== 私有辅助方法 =====

    /**
     * 随机抽取指定数量题目
     */
    private List<Question> randomPickQuestions(Long bankId, String type, int count) {
        List<Question> allQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getBankId, bankId)
                        .eq(Question::getType, type));

        if (allQuestions.size() <= count) {
            return new ArrayList<>(allQuestions);
        }

        // 随机选取 count 道
        Collections.shuffle(allQuestions, new Random(System.currentTimeMillis()));
        return allQuestions.subList(0, count);
    }

    /**
     * 判断答案是否正确
     */
    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.isEmpty()) return false;
        String correctAnswer = question.getAnswer();

        switch (question.getType()) {
            case "single":
                return correctAnswer.equalsIgnoreCase(userAnswer);
            case "judgment":
                return correctAnswer.equals(userAnswer);
            case "multiple":
                String normalizedUser = normalizeMultiAnswer(userAnswer);
                String normalizedCorrect = normalizeMultiAnswer(correctAnswer);
                return normalizedUser.equals(normalizedCorrect);
            default:
                return false;
        }
    }

    private String normalizeMultiAnswer(String answer) {
        if (answer == null || answer.isEmpty()) return "";
        char[] chars = answer.toUpperCase().toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    private int getQuestionScore(String type) {
        return switch (type) {
            case "single" -> SINGLE_SCORE;
            case "multiple" -> MULTIPLE_SCORE;
            case "judgment" -> JUDGMENT_SCORE;
            default -> 0;
        };
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isEmpty()) return Collections.emptyList();
        try {
            // 先尝试解析为 List<String>（A. 文本 格式）
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e1) {
            try {
                // 再尝试解析为 List<Map>（{"label":"A","text":"..."} 格式）
                List<Map<String, String>> options = objectMapper.readValue(optionsJson,
                        new TypeReference<List<Map<String, String>>>() {});
                return options.stream()
                        .map(opt -> opt.get("label") + ". " + opt.get("text"))
                        .collect(Collectors.toList());
            } catch (Exception e2) {
                return Collections.emptyList();
            }
        }
    }

    private QuestionDTO toDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setBankId(question.getBankId());
        dto.setType(question.getType());
        dto.setStem(question.getStem());
        dto.setOptions(parseOptions(question.getOptions()));
        dto.setSource(question.getSource());
        // 考试时不返回答案
        dto.setAnswer(null);
        return dto;
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        if (h > 0) {
            return String.format("%d时%d分%d秒", h, m, s);
        } else if (m > 0) {
            return String.format("%d分%d秒", m, s);
        } else {
            return String.format("%d秒", s);
        }
    }
}
