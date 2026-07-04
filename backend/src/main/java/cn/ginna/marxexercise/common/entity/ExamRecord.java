package cn.ginna.marxexercise.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long bankId;
    private Integer score;
    private Integer totalScore;
    private Integer singleCorrect;
    private Integer singleTotal;
    private Integer multipleCorrect;
    private Integer multipleTotal;
    private Integer judgmentCorrect;
    private Integer judgmentTotal;
    private Integer durationSeconds;
    private String status; // in_progress / completed
    private String markedQuestionIds; // JSON: [1,2,3]
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
