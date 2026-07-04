package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.AnswerSubmitDTO;
import cn.ginna.marxexercise.common.dto.ApiResponse;
import cn.ginna.marxexercise.common.dto.QuestionDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    /**
     * 开始一轮练习
     */
    @GetMapping("/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startPractice(
            Authentication authentication,
            @RequestParam Long bankId,
            @RequestParam String questionType,
            @RequestParam(defaultValue = "1") Integer roundNum) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> result = practiceService.startPractice(userId, bankId, questionType, roundNum);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取剩余题目
     */
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getQuestions(
            Authentication authentication,
            @RequestParam Long bankId,
            @RequestParam String questionType,
            @RequestParam(defaultValue = "1") Integer roundNum) {
        Long userId = (Long) authentication.getPrincipal();
        List<QuestionDTO> questions = practiceService.getPracticeQuestions(userId, bankId, questionType, roundNum);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    /**
     * 提交答案
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAnswer(
            Authentication authentication,
            @Valid @RequestBody AnswerSubmitDTO dto) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> result = practiceService.submitAnswer(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 检查一轮是否完成
     */
    @GetMapping("/check-complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkComplete(
            Authentication authentication,
            @RequestParam Long bankId,
            @RequestParam String questionType,
            @RequestParam(defaultValue = "1") Integer roundNum) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> result = practiceService.checkRoundComplete(userId, bankId, questionType, roundNum);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取建议轮次（自动跳到未完成或下一轮）
     */
    @GetMapping("/suggest-round")
    public ResponseEntity<ApiResponse<Integer>> suggestRound(
            Authentication authentication,
            @RequestParam Long bankId,
            @RequestParam String questionType) {
        Long userId = (Long) authentication.getPrincipal();
        Integer round = practiceService.getSuggestedRound(userId, bankId, questionType);
        return ResponseEntity.ok(ApiResponse.success(round));
    }

    /**
     * 开始错题练习
     */
    @GetMapping("/wrong-start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startWrongPractice(
            Authentication authentication,
            @RequestParam(required = false) Long bankId) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> result = practiceService.startWrongPractice(userId, bankId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
