package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.ApiResponse;
import cn.ginna.marxexercise.common.dto.StatisticsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 获取统计信息
     */
    @GetMapping
    public ResponseEntity<ApiResponse<StatisticsDTO>> getStatistics(
            Authentication authentication,
            @RequestParam(required = false) Long bankId) {
        Long userId = (Long) authentication.getPrincipal();
        StatisticsDTO stats = statisticsService.getStatistics(userId, bankId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取所有进度
     */
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProgress(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> progress = statisticsService.getAllProgress(userId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }
}
