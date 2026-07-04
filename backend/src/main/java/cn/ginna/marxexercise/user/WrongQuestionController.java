package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.ApiResponse;
import cn.ginna.marxexercise.common.dto.QuestionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wrong")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    /**
     * 获取错题列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getWrongQuestions(
            Authentication authentication,
            @RequestParam(required = false) Long bankId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String keyword) {
        Long userId = (Long) authentication.getPrincipal();
        List<QuestionDTO> questions = wrongQuestionService.getWrongQuestions(userId, bankId, questionType, keyword);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    /**
     * 获取错题统计
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWrongStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = wrongQuestionService.getWrongStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 从错题本中移除题目
     */
    @DeleteMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Void>> removeWrongQuestion(
            Authentication authentication,
            @PathVariable Long questionId) {
        Long userId = (Long) authentication.getPrincipal();
        wrongQuestionService.removeWrongQuestion(userId, questionId);
        return ResponseEntity.ok(ApiResponse.success("已移出错题本", null));
    }

    /**
     * 批量从错题本中移除题目
     */
    @PostMapping("/batch-remove")
    public ResponseEntity<ApiResponse<Void>> batchRemoveWrongQuestions(
            Authentication authentication,
            @RequestBody List<Long> questionIds) {
        Long userId = (Long) authentication.getPrincipal();
        wrongQuestionService.batchRemoveWrongQuestions(userId, questionIds);
        return ResponseEntity.ok(ApiResponse.success("已批量移出错题本", null));
    }
}
