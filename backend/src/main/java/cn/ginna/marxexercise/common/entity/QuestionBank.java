package cn.ginna.marxexercise.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_bank")
public class QuestionBank {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String fileName;

    private String sourceUrl;

    private Integer questionCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
