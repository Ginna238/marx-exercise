package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.QuestionDTO;
import cn.ginna.marxexercise.common.entity.Question;
import cn.ginna.marxexercise.common.entity.WrongQuestion;
import cn.ginna.marxexercise.common.mapper.QuestionMapper;
import cn.ginna.marxexercise.common.mapper.WrongQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    public WrongQuestionService(WrongQuestionMapper wrongQuestionMapper, QuestionMapper questionMapper,
                                ObjectMapper objectMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取用户的错题列表
     */
    public List<QuestionDTO> getWrongQuestions(Long userId, Long bankId, String questionType, String keyword) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId);

        if (bankId != null) {
            wrapper.eq(WrongQuestion::getBankId, bankId);
        }
        if (questionType != null && !questionType.isEmpty()) {
            wrapper.eq(WrongQuestion::getQuestionType, questionType);
        }

        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(wrapper);

        if (wrongList.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取题目详情，并清理已不存在的题目引用
        List<Long> questionIds = wrongList.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toList());

        // 有关键字时，在题干中模糊搜索
        List<Question> questions;
        if (keyword != null && !keyword.trim().isEmpty()) {
            questions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .in(Question::getId, questionIds)
                            .like(Question::getStem, keyword.trim()));
        } else {
            questions = questionMapper.selectBatchIds(questionIds);
        }

        // 清理已不存在题目的错题记录（防止数据不一致）
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

        // 按错误次数降序排列
        Map<Long, Integer> wrongCountMap = wrongList.stream()
                .collect(Collectors.toMap(WrongQuestion::getQuestionId, WrongQuestion::getWrongCount));

        // 先打乱顺序，再按错误次数降序排（相同错误次数的保持随机顺序）
        Collections.shuffle(questions, new Random(System.currentTimeMillis()));
        questions.sort((a, b) -> Integer.compare(
                wrongCountMap.getOrDefault(b.getId(), 0),
                wrongCountMap.getOrDefault(a.getId(), 0)));

        return questions.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取错题统计
     */
    public Map<String, Object> getWrongStats(Long userId) {
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId));

        long singleCount = wrongList.stream().filter(w -> "single".equals(w.getQuestionType())).count();
        long multipleCount = wrongList.stream().filter(w -> "multiple".equals(w.getQuestionType())).count();
        long judgmentCount = wrongList.stream().filter(w -> "judgment".equals(w.getQuestionType())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", wrongList.size());
        stats.put("single", singleCount);
        stats.put("multiple", multipleCount);
        stats.put("judgment", judgmentCount);

        return stats;
    }

    /**
     * 将题目加入错题本（若已存在则增加错误次数）
     */
    public void addWrongQuestion(Long userId, Long questionId, Long bankId, String questionType) {
        WrongQuestion wrong = wrongQuestionMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, questionId));
        if (wrong == null) {
            wrong = new WrongQuestion();
            wrong.setUserId(userId);
            wrong.setQuestionId(questionId);
            wrong.setBankId(bankId);
            wrong.setQuestionType(questionType);
            wrong.setWrongCount(1);
            wrong.setLastWrongAt(LocalDateTime.now());
            wrongQuestionMapper.insert(wrong);
        } else {
            wrong.setWrongCount(wrong.getWrongCount() + 1);
            wrong.setLastWrongAt(LocalDateTime.now());
            wrongQuestionMapper.updateById(wrong);
        }
    }

    /**
     * 从错题本中移除题目（物理删除）
     */
    public void removeWrongQuestion(Long userId, Long questionId) {
        wrongQuestionMapper.delete(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, questionId));
    }

    /**
     * 批量从错题本中移除题目
     */
    public void batchRemoveWrongQuestions(Long userId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return;
        wrongQuestionMapper.delete(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .in(WrongQuestion::getQuestionId, questionIds));
    }

    /**
     * 获取某道题的答错次数
     */
    public int getWrongCount(Long userId, Long questionId) {
        WrongQuestion wq = wrongQuestionMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, questionId));
        return wq != null ? wq.getWrongCount() : 0;
    }

    private QuestionDTO toDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setBankId(question.getBankId());
        dto.setType(question.getType());
        dto.setStem(question.getStem());
        dto.setSource(question.getSource());
        dto.setAnswer(question.getAnswer());

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
