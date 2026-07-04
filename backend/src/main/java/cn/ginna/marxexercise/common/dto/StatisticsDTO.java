package cn.ginna.marxexercise.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {
    private Long totalQuestions;
    private Long totalAnswered;
    private Long totalCorrect;
    private Double accuracy;
    private Integer currentRound;

    private List<TypeStat> typeStats;
    private List<DailyStat> dailyStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeStat {
        private String type;
        private String typeName;
        private Long total;
        private Long answered;
        private Long correct;
        private Double accuracy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private String date;
        private Long count;
        private Long correct;
    }
}
