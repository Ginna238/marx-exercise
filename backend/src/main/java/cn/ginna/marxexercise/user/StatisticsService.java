package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.StatisticsDTO;
import cn.ginna.marxexercise.common.entity.QuestionBank;
import cn.ginna.marxexercise.common.entity.UserAnswer;
import cn.ginna.marxexercise.common.entity.UserProgress;
import cn.ginna.marxexercise.common.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final QuestionMapper questionMapper;
    private final UserAnswerMapper userAnswerMapper;
    private final UserProgressMapper userProgressMapper;
    private final QuestionBankMapper bankMapper;
    private final WrongQuestionMapper wrongQuestionMapper;

    public StatisticsService(QuestionMapper questionMapper, UserAnswerMapper userAnswerMapper,
                             UserProgressMapper userProgressMapper, QuestionBankMapper bankMapper,
                             WrongQuestionMapper wrongQuestionMapper) {
        this.questionMapper = questionMapper;
        this.userAnswerMapper = userAnswerMapper;
        this.userProgressMapper = userProgressMapper;
        this.bankMapper = bankMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
    }

    /**
     * 获取用户统计信息
     */
    public StatisticsDTO getStatistics(Long userId, Long bankId) {
        // 总题数（从题库表汇总，避免重复计数）
        Long totalQuestions = getTotalQuestionCount(bankId);

        // 答题统计
        LambdaQueryWrapper<UserAnswer> answerQuery = new LambdaQueryWrapper<UserAnswer>()
                .eq(UserAnswer::getUserId, userId);
        if (bankId != null) {
            answerQuery.eq(UserAnswer::getBankId, bankId);
        }
        List<UserAnswer> allAnswers = userAnswerMapper.selectList(answerQuery);

        long totalAnswered = allAnswers.size();
        long totalCorrect = allAnswers.stream().filter(a -> a.getIsCorrect() == 1).count();
        double accuracy = totalAnswered > 0 ? (double) totalCorrect / totalAnswered * 100 : 0;

        // 当前轮次
        List<UserProgress> progressList = userProgressMapper.selectList(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId));

        int currentRound = progressList.stream()
                .mapToInt(UserProgress::getRoundNum)
                .max().orElse(1);

        // 按题型统计
        List<StatisticsDTO.TypeStat> typeStats = getTypeStats(userId, bankId, allAnswers);

        // 每日统计（近7天）
        List<StatisticsDTO.DailyStat> dailyStats = getDailyStats(userId, bankId);

        return StatisticsDTO.builder()
                .totalQuestions(totalQuestions)
                .totalAnswered(totalAnswered)
                .totalCorrect(totalCorrect)
                .accuracy(Math.round(accuracy * 100.0) / 100.0)
                .currentRound(currentRound)
                .typeStats(typeStats)
                .dailyStats(dailyStats)
                .build();
    }

    private List<StatisticsDTO.TypeStat> getTypeStats(Long userId, Long bankId, List<UserAnswer> allAnswers) {
        String[] types = {"single", "multiple", "judgment"};
        String[] typeNames = {"单选题", "多选题", "判断题"};

        List<StatisticsDTO.TypeStat> stats = new ArrayList<>();

        for (int i = 0; i < types.length; i++) {
            String type = types[i];

            Long total = getTypeQuestionCount(bankId, type);

            List<UserAnswer> typeAnswers = allAnswers.stream()
                    .filter(a -> type.equals(a.getQuestionType()))
                    .collect(Collectors.toList());

            long answered = typeAnswers.size();
            long correct = typeAnswers.stream().filter(a -> a.getIsCorrect() == 1).count();
            double accuracy = answered > 0 ? (double) correct / answered * 100 : 0;

            stats.add(StatisticsDTO.TypeStat.builder()
                    .type(type)
                    .typeName(typeNames[i])
                    .total(total)
                    .answered(answered)
                    .correct(correct)
                    .accuracy(Math.round(accuracy * 100.0) / 100.0)
                    .build());
        }

        return stats;
    }

    private List<StatisticsDTO.DailyStat> getDailyStats(Long userId, Long bankId) {
        List<StatisticsDTO.DailyStat> dailyStats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        LambdaQueryWrapper<UserAnswer> query = new LambdaQueryWrapper<UserAnswer>()
                .eq(UserAnswer::getUserId, userId);
        if (bankId != null) {
            query.eq(UserAnswer::getBankId, bankId);
        }
        List<UserAnswer> allAnswers = userAnswerMapper.selectList(query);

        // 按日期分组
        Map<LocalDate, List<UserAnswer>> groupedByDate = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getCreatedAt().toLocalDate()));

        // 近7天
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<UserAnswer> dayAnswers = groupedByDate.getOrDefault(date, Collections.emptyList());
            long count = dayAnswers.size();
            long correct = dayAnswers.stream().filter(a -> a.getIsCorrect() == 1).count();

            dailyStats.add(StatisticsDTO.DailyStat.builder()
                    .date(date.format(formatter))
                    .count(count)
                    .correct(correct)
                    .build());
        }

        return dailyStats;
    }

    /**
     * 获取用户进度总览（所有题库+题型）
     */
    public List<Map<String, Object>> getAllProgress(Long userId) {
        List<UserProgress> progressList = userProgressMapper.selectList(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .orderByAsc(UserProgress::getBankId, UserProgress::getQuestionType, UserProgress::getRoundNum));

        List<QuestionBank> banks = bankMapper.selectList(null);
        Set<Long> validBankIds = banks.stream().map(QuestionBank::getId).collect(Collectors.toSet());

        // 清理已不存在题库的进度记录
        List<UserProgress> toDelete = progressList.stream()
                .filter(p -> !validBankIds.contains(p.getBankId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            List<Long> deleteIds = toDelete.stream().map(UserProgress::getId).collect(Collectors.toList());
            userProgressMapper.deleteBatchIds(deleteIds);
            progressList.removeAll(toDelete);
        }

        Map<Long, String> bankNames = banks.stream()
                .collect(Collectors.toMap(QuestionBank::getId, QuestionBank::getName, (a, b) -> a));

        Map<String, String> typeNames = Map.of(
                "single", "单选题",
                "multiple", "多选题",
                "judgment", "判断题"
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserProgress p : progressList) {
            Map<String, Object> item = new HashMap<>();
            item.put("bankId", p.getBankId());
            item.put("bankName", bankNames.getOrDefault(p.getBankId(), "未知题库"));
            item.put("questionType", p.getQuestionType());
            item.put("typeName", typeNames.getOrDefault(p.getQuestionType(), p.getQuestionType()));
            item.put("roundNum", p.getRoundNum());
            item.put("totalCount", p.getTotalCount());
            item.put("answeredCount", p.getAnsweredCount());
            item.put("correctCount", p.getCorrectCount());
            item.put("status", p.getStatus());
            double progress = p.getTotalCount() > 0 ?
                    (double) p.getAnsweredCount() / p.getTotalCount() * 100 : 0;
            item.put("progressPercent", Math.round(progress * 100.0) / 100.0);
            double accuracy = p.getAnsweredCount() > 0 ?
                    (double) p.getCorrectCount() / p.getAnsweredCount() * 100 : 0;
            item.put("accuracy", Math.round(accuracy * 100.0) / 100.0);
            result.add(item);
        }

        return result;
    }

    /**
     * 获取指定题库（或全部）的去重题目总数
     */
    private Long getTotalQuestionCount(Long bankId) {
        if (bankId != null) {
            QuestionBank bank = bankMapper.selectById(bankId);
            return bank != null ? bank.getQuestionCount().longValue() : 0L;
        }
        // 汇总所有题库的数量
        List<QuestionBank> banks = bankMapper.selectList(null);
        return banks.stream()
                .mapToLong(b -> b.getQuestionCount() != null ? b.getQuestionCount() : 0)
                .sum();
    }

    /**
     * 获取指定题库+题型的去重题目数
     */
    private Long getTypeQuestionCount(Long bankId, String type) {
        LambdaQueryWrapper<cn.ginna.marxexercise.common.entity.Question> wrapper =
                new LambdaQueryWrapper<cn.ginna.marxexercise.common.entity.Question>()
                        .eq(cn.ginna.marxexercise.common.entity.Question::getType, type);
        if (bankId != null) {
            wrapper.eq(cn.ginna.marxexercise.common.entity.Question::getBankId, bankId);
        }
        return questionMapper.selectCount(wrapper);
    }
}
