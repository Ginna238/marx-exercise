package cn.ginna.marxexercise.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_progress")
public class UserProgress {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bankId;

    private String questionType;

    private Integer roundNum;

    private Integer totalCount;

    private Integer answeredCount;

    private Integer correctCount;

    /**
     * 状态: in_progress/completed
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
