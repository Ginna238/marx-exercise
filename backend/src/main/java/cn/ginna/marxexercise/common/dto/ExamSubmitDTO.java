package cn.ginna.marxexercise.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 考试提交请求
 */
@Data
public class ExamSubmitDTO {
    @NotNull(message = "考试记录ID不能为空")
    private Long examId;

    @NotNull(message = "题库ID不能为空")
    private Long bankId;

    @NotNull(message = "用时不能为空")
    private Integer durationSeconds;

    // 题目答案: questionId -> selectedAnswer
    @NotNull(message = "答案不能为空")
    private Map<Long, String> answers;

    // 被标记的题目ID列表
    private List<Long> markedQuestionIds;
}
