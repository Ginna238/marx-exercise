package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.ApiResponse;
import cn.ginna.marxexercise.common.entity.Question;
import cn.ginna.marxexercise.common.entity.QuestionBank;
import cn.ginna.marxexercise.common.mapper.QuestionBankMapper;
import cn.ginna.marxexercise.common.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class BankController {

    private final QuestionBankMapper bankMapper;
    private final QuestionMapper questionMapper;

    public BankController(QuestionBankMapper bankMapper, QuestionMapper questionMapper) {
        this.bankMapper = bankMapper;
        this.questionMapper = questionMapper;
    }

    @GetMapping("/banks")
    public ResponseEntity<ApiResponse<List<QuestionBank>>> getBanks() {
        List<QuestionBank> banks = bankMapper.selectList(null);
        return ResponseEntity.ok(ApiResponse.success(banks));
    }

    /**
     * 获取题库各题型题目数量
     */
    @GetMapping("/banks/{bankId}/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getQuestionCounts(@PathVariable Long bankId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        String[] types = {"single", "multiple", "judgment"};
        for (String type : types) {
            long count = questionMapper.selectCount(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getBankId, bankId)
                            .eq(Question::getType, type));
            counts.put(type, count);
        }
        return ResponseEntity.ok(ApiResponse.success(counts));
    }
}
