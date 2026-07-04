package cn.ginna.marxexercise.user;

import cn.ginna.marxexercise.common.entity.Question;
import cn.ginna.marxexercise.common.entity.QuestionBank;
import cn.ginna.marxexercise.common.mapper.QuestionBankMapper;
import cn.ginna.marxexercise.common.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionBankLoader {

    private static final Logger log = LoggerFactory.getLogger(QuestionBankLoader.class);
    private static final Pattern OPTION_PATTERN = Pattern.compile("^([A-Z])\\.\\s*(.*)");

    private final QuestionBankMapper bankMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.question-bank.location}")
    private String bankLocation;

    public QuestionBankLoader(QuestionBankMapper bankMapper, QuestionMapper questionMapper, ObjectMapper objectMapper) {
        this.bankMapper = bankMapper;
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadQuestionBanks() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:ques-bank/*.json");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                log.info("加载题库文件: {}", filename);
                String bankName = filename.replace(".json", "").replace("2025-2026-2", "");

                // 检查题库是否已加载
                QuestionBank existingBank = bankMapper.selectOne(
                        new LambdaQueryWrapper<QuestionBank>().eq(QuestionBank::getFileName, filename));
                if (existingBank != null) {
                    log.info("题库 {} 已加载，跳过", filename);
                    continue;
                }

                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    loadBank(filename, bankName, content);
                }
            }
        } catch (Exception e) {
            log.error("加载题库失败", e);
        }
    }

    @Transactional
    public void loadBank(String fileName, String bankName, String jsonContent) throws Exception {
        Map<String, Object> rawData = objectMapper.readValue(jsonContent,
                new TypeReference<Map<String, Object>>() {});

        // 创建题库记录
        QuestionBank bank = new QuestionBank();
        bank.setName(bankName);
        bank.setFileName(fileName);
        bank.setSourceUrl("https://github.com/keggin-CHN/njfu_grinding");
        bankMapper.insert(bank);

        int totalCount = 0;

        // 解析三种题型
        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            String typeKey = entry.getKey();
            String questionType = switch (typeKey) {
                case "单选题" -> "single";
                case "多选题" -> "multiple";
                case "判断题" -> "judgment";
                default -> null;
            };

            if (questionType == null) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> questions = (Map<String, Object>) entry.getValue();

            for (Map.Entry<String, Object> qEntry : questions.entrySet()) {
                String stem = qEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> qData = (Map<String, Object>) qEntry.getValue();

                Question question = new Question();
                question.setBankId(bank.getId());
                question.setType(questionType);
                question.setStem(stem);
                question.setAnswer(String.valueOf(qData.get("answer")));
                question.setSource(qData.get("source") != null ? String.valueOf(qData.get("source")) : null);

                // 处理选项
                if (qData.containsKey("options")) {
                    @SuppressWarnings("unchecked")
                    List<String> rawOptions = (List<String>) qData.get("options");
                    List<Map<String, String>> formattedOptions = new ArrayList<>();
                    for (String rawOpt : rawOptions) {
                        Matcher matcher = OPTION_PATTERN.matcher(rawOpt.trim());
                        if (matcher.find()) {
                            Map<String, String> opt = new LinkedHashMap<>();
                            opt.put("label", matcher.group(1));
                            opt.put("text", matcher.group(2));
                            formattedOptions.add(opt);
                        }
                    }
                    question.setOptions(objectMapper.writeValueAsString(formattedOptions));
                }

                // 生成哈希用于去重
                question.setHash(md5Hash(stem));

                questionMapper.insert(question);
                totalCount++;
            }
        }

        // 更新题库统计
        bank.setQuestionCount(totalCount);
        bankMapper.updateById(bank);

        log.info("题库 {} 加载完成，共 {} 道题", bankName, totalCount);
    }

    private String md5Hash(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 手动触发重新加载题库（仅重新加载本地 JSON 文件对应的题库，保留外部导入的题库）
     */
    @Transactional
    public void reloadAll() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:ques-bank/*.json");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                log.info("重新加载题库文件: {}", filename);
                String bankName = filename.replace(".json", "").replace("2025-2026-2", "");

                // 查找已有题库（可能已存在）
                QuestionBank bank = bankMapper.selectOne(
                        new LambdaQueryWrapper<QuestionBank>().eq(QuestionBank::getFileName, filename));

                if (bank == null) {
                    // 新题库，创建记录
                    bank = new QuestionBank();
                    bank.setName(bankName);
                    bank.setFileName(filename);
                    bank.setSourceUrl("https://github.com/keggin-CHN/njfu_grinding");
                    bankMapper.insert(bank);
                } else {
                    // 已有题库，删除该库下的旧题目，保留其他数据
                    questionMapper.delete(
                            new LambdaQueryWrapper<Question>().eq(Question::getBankId, bank.getId()));
                }

                // 重新解析并插入题目
                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, Object> rawData = objectMapper.readValue(content,
                            new TypeReference<Map<String, Object>>() {});

                    int totalCount = 0;

                    for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                        String typeKey = entry.getKey();
                        String questionType = switch (typeKey) {
                            case "单选题" -> "single";
                            case "多选题" -> "multiple";
                            case "判断题" -> "judgment";
                            default -> null;
                        };
                        if (questionType == null) continue;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> questions = (Map<String, Object>) entry.getValue();

                        for (Map.Entry<String, Object> qEntry : questions.entrySet()) {
                            String stem = qEntry.getKey();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> qData = (Map<String, Object>) qEntry.getValue();

                            Question question = new Question();
                            question.setBankId(bank.getId());
                            question.setType(questionType);
                            question.setStem(stem);
                            question.setAnswer(String.valueOf(qData.get("answer")));
                            question.setSource(qData.get("source") != null ? String.valueOf(qData.get("source")) : null);

                            if (qData.containsKey("options")) {
                                @SuppressWarnings("unchecked")
                                List<String> rawOptions = (List<String>) qData.get("options");
                                List<Map<String, String>> formattedOptions = new ArrayList<>();
                                for (String rawOpt : rawOptions) {
                                    Matcher matcher = OPTION_PATTERN.matcher(rawOpt.trim());
                                    if (matcher.find()) {
                                        Map<String, String> opt = new LinkedHashMap<>();
                                        opt.put("label", matcher.group(1));
                                        opt.put("text", matcher.group(2));
                                        formattedOptions.add(opt);
                                    }
                                }
                                question.setOptions(objectMapper.writeValueAsString(formattedOptions));
                            }

                            question.setHash(md5Hash(stem));
                            questionMapper.insert(question);
                            totalCount++;
                        }
                    }

                    bank.setQuestionCount(totalCount);
                    bankMapper.updateById(bank);
                    log.info("题库 {} 重新加载完成，共 {} 道题", bankName, totalCount);
                }
            }
        } catch (Exception e) {
            log.error("重新加载题库失败", e);
            throw new RuntimeException("重新加载题库失败: " + e.getMessage());
        }
    }
}
