package cn.ginna.marxexercise.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDTO {
    private Long id;
    private Long bankId;
    private String type;
    private String stem;
    private List<String> options;
    private String source;
    private String answer;
}
