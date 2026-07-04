package cn.ginna.marxexercise.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 考试结果响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultDTO {
    private Long examId;
    private String bankName;
    private Integer score;
    private Integer totalScore;
    private Integer singleCorrect;
    private Integer singleTotal;
    private Integer multipleCorrect;
    private Integer multipleTotal;
    private Integer judgmentCorrect;
    private Integer judgmentTotal;
    private Integer durationSeconds;
    private String durationText;

    private List<QuestionResult> singleResults;
    private List<QuestionResult> multipleResults;
    private List<QuestionResult> judgmentResults;

    private List<Long> markedQuestionIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private Long questionId;
        private String stem;
        private String type;
        private List<String> options;
        private String correctAnswer;
        private String userAnswer;
        private boolean isCorrect;
        private Integer score;
        private boolean marked;
    }
}
