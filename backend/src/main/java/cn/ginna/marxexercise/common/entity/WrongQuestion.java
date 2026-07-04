package cn.ginna.marxexercise.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private Long bankId;

    private String questionType;

    private Integer wrongCount;

    private LocalDateTime lastWrongAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
