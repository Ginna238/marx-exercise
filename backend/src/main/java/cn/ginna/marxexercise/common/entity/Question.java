package cn.ginna.marxexercise.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bankId;

    /**
     * 题型: single/multiple/judgment
     */
    private String type;

    private String stem;

    /**
     * 选项，JSON数组格式
     */
    private String options;

    private String answer;

    private String source;

    private String hash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
