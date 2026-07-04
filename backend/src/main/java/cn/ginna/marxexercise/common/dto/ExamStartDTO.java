package cn.ginna.marxexercise.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 考试开始响应
 */
@Data
public class ExamStartDTO {
    private Long examId;
    private String bankName;
    private List<QuestionDTO> singleQuestions;   // 60道单选
    private List<QuestionDTO> multipleQuestions; // 10道多选
    private List<QuestionDTO> judgmentQuestions; // 10道判断
    private Integer singleTotal;
    private Integer multipleTotal;
    private Integer judgmentTotal;
    private Integer totalScore;
}
