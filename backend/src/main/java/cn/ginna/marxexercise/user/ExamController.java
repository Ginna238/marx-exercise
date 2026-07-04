package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.ApiResponse;
import cn.ginna.marxexercise.common.dto.ExamResultDTO;
import cn.ginna.marxexercise.common.dto.ExamStartDTO;
import cn.ginna.marxexercise.common.dto.ExamSubmitDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * 开始模拟考试
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<ExamStartDTO>> startExam(
            Authentication authentication,
            @RequestParam Long bankId) {
        Long userId = (Long) authentication.getPrincipal();
        ExamStartDTO result = examService.startExam(userId, bankId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 提交考试答案
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ExamResultDTO>> submitExam(
            Authentication authentication,
            @Valid @RequestBody ExamSubmitDTO dto) {
        Long userId = (Long) authentication.getPrincipal();
        ExamResultDTO result = examService.submitExam(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取考试记录列表
     */
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getExamRecords(
            Authentication authentication,
            @RequestParam(required = false) Long bankId) {
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> records = examService.getExamRecords(userId, bankId);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    /**
     * 获取考试统计
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExamStats(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = examService.getExamStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
