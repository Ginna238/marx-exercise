package cn.ginna.marxexercise.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_answer")
public class UserAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private Long bankId;

    private String questionType;

    private String selectedAnswer;

    private Integer isCorrect;

    private Integer roundNum;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
