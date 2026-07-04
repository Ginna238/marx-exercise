package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.dto.ApiResponse;
import cn.ginna.marxexercise.common.dto.QuestionDTO;
import cn.ginna.marxexercise.common.entity.Question;
import cn.ginna.marxexercise.common.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    public ReviewController(QuestionMapper questionMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuestions(
            @RequestParam Long bankId,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getBankId, bankId);

        if (!type.isEmpty()) {
            wrapper.eq(Question::getType, type);
        }

        if (!keyword.isEmpty()) {
            wrapper.like(Question::getStem, keyword);
        }

        wrapper.orderByAsc(Question::getId);

        List<Question> questions = questionMapper.selectList(wrapper);

        // 有关键字时，额外在选项中搜索
        if (!keyword.isEmpty()) {
            List<Question> allTypeQuestions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getBankId, bankId)
                            .eq(!type.isEmpty(), Question::getType, type));
            List<Question> optionMatch = allTypeQuestions.stream()
                    .filter(q -> q.getOptions() != null && q.getOptions().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
            // 合并去重
            Set<Long> existingIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
            for (Question q : optionMatch) {
                if (!existingIds.contains(q.getId())) {
                    questions.add(q);
                }
            }
            questions.sort(Comparator.comparing(Question::getId));
        }

        List<QuestionDTO> dtoList = questions.stream().map(this::toDTO).collect(Collectors.toList());

        // 按题型统计总数
        long singleTotal = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getBankId, bankId).eq(Question::getType, "single"));
        long multipleTotal = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getBankId, bankId).eq(Question::getType, "multiple"));
        long judgmentTotal = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getBankId, bankId).eq(Question::getType, "judgment"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questions", dtoList);
        result.put("singleTotal", singleTotal);
        result.put("multipleTotal", multipleTotal);
        result.put("judgmentTotal", judgmentTotal);
        result.put("total", singleTotal + multipleTotal + judgmentTotal);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e1) {
            try {
                List<Map<String, String>> options = objectMapper.readValue(optionsJson,
                        new TypeReference<List<Map<String, String>>>() {});
                return options.stream()
                        .map(opt -> opt.get("label") + ". " + opt.get("text"))
                        .collect(Collectors.toList());
            } catch (Exception e2) {
                return Collections.emptyList();
            }
        }
    }

    private QuestionDTO toDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setBankId(question.getBankId());
        dto.setType(question.getType());
        dto.setStem(question.getStem());
        dto.setOptions(parseOptions(question.getOptions()));
        dto.setAnswer(question.getAnswer());
        dto.setSource(question.getSource());
        return dto;
    }
}
