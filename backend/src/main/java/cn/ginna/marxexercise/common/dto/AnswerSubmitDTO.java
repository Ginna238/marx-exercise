package cn.ginna.marxexercise.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitDTO {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "题库ID不能为空")
    private Long bankId;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    private String selectedAnswer;

    @NotNull(message = "当前轮次不能为空")
    private Integer roundNum;
}
