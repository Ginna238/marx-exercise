package cn.ginna.marxexercise.admin;

import cn.ginna.marxexercise.common.dto.ApiResponse;
import cn.ginna.marxexercise.common.entity.Question;
import cn.ginna.marxexercise.common.entity.QuestionBank;
import cn.ginna.marxexercise.common.entity.User;
import cn.ginna.marxexercise.common.entity.UserAnswer;
import cn.ginna.marxexercise.common.entity.WrongQuestion;
import cn.ginna.marxexercise.common.entity.ExamRecord;
import cn.ginna.marxexercise.common.mapper.QuestionBankMapper;
import cn.ginna.marxexercise.common.mapper.QuestionMapper;
import cn.ginna.marxexercise.common.mapper.UserMapper;
import cn.ginna.marxexercise.common.mapper.UserAnswerMapper;
import cn.ginna.marxexercise.common.mapper.WrongQuestionMapper;
import cn.ginna.marxexercise.common.mapper.ExamRecordMapper;
import cn.ginna.marxexercise.user.QuestionBankLoader;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final QuestionBankMapper bankMapper;
    private final QuestionMapper questionMapper;
    private final UserAnswerMapper userAnswerMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final QuestionBankLoader questionBankLoader;
    private final PasswordEncoder passwordEncoder;
    private final GitHubBankUpdater gitHubBankUpdater;

    public AdminController(UserMapper userMapper, QuestionBankMapper bankMapper,
                           QuestionMapper questionMapper, UserAnswerMapper userAnswerMapper,
                           WrongQuestionMapper wrongQuestionMapper, ExamRecordMapper examRecordMapper,
                           QuestionBankLoader questionBankLoader,
                           PasswordEncoder passwordEncoder, GitHubBankUpdater gitHubBankUpdater) {
        this.userMapper = userMapper;
        this.bankMapper = bankMapper;
        this.questionMapper = questionMapper;
        this.userAnswerMapper = userAnswerMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.examRecordMapper = examRecordMapper;
        this.questionBankLoader = questionBankLoader;
        this.passwordEncoder = passwordEncoder;
        this.gitHubBankUpdater = gitHubBankUpdater;
    }

    /** 当前管理员信息 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userMapper.selectById(userId);
        user.setPassword(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /** 仪表盘统计 */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("bankCount", bankMapper.selectCount(null));
        data.put("questionCount", questionMapper.selectCount(null));
        data.put("answerCount", userAnswerMapper.selectCount(null));
        data.put("wrongCount", wrongQuestionMapper.selectCount(null));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 用户列表 */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> listUsers() {
        List<User> users = userMapper.selectList(null);
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /** 重置用户密码 */
    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户不存在"));
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return ResponseEntity.ok(ApiResponse.success("密码已重置为 123456", null));
    }

    /** 删除用户 */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("用户已删除", null));
    }

    /** 题库列表 */
    @GetMapping("/banks")
    public ResponseEntity<ApiResponse<List<QuestionBank>>> listBanks() {
        return ResponseEntity.ok(ApiResponse.success(bankMapper.selectList(null)));
    }

    /** 重新加载题库 */
    @PostMapping("/banks/reload")
    public ResponseEntity<ApiResponse<Void>> reloadBanks() {
        questionBankLoader.reloadAll();
        return ResponseEntity.ok(ApiResponse.success("题库已重新加载", null));
    }

    /** 按题库查询题目 */
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<Question>>> listQuestions(
            @RequestParam(required = false) Long bankId,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .orderByAsc(Question::getId);
        if (bankId != null) wrapper.eq(Question::getBankId, bankId);
        if (type != null && !type.isEmpty()) wrapper.eq(Question::getType, type);
        return ResponseEntity.ok(ApiResponse.success(questionMapper.selectList(wrapper)));
    }

    /** 获取 GitHub 可用题库列表 */
    @GetMapping("/github-banks")
    public ResponseEntity<ApiResponse<List<GitHubBankUpdater.BankFileInfo>>> listGitHubBanks() {
        try {
            List<GitHubBankUpdater.BankFileInfo> files = gitHubBankUpdater.listAvailableBanks();
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "获取失败: " + e.getMessage()));
        }
    }

    /** 从 GitHub 下载并导入指定题库 */
    @PostMapping("/github-import")
    public ResponseEntity<ApiResponse<String>> importFromGitHub(@RequestBody Map<String, String> body) {
        try {
            String path = body.get("path");
            String name = body.get("name");
            String url = body.get("downloadUrl");
            GitHubBankUpdater.BankFileInfo info = new GitHubBankUpdater.BankFileInfo();
            info.name = name;
            info.path = path;
            info.downloadUrl = url;
            String bankName = gitHubBankUpdater.downloadAndImport(info);
            return ResponseEntity.ok(ApiResponse.success("题库 " + bankName + " 导入成功", bankName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "导入失败: " + e.getMessage()));
        }
    }

    /** 同步所有 GitHub 新题库 */
    @PostMapping("/github-sync-all")
    public ResponseEntity<ApiResponse<List<String>>> syncAll() {
        try {
            List<String> imported = gitHubBankUpdater.syncAll();
            if (imported.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("没有新题库需要导入", imported));
            }
            return ResponseEntity.ok(ApiResponse.success("已导入: " + String.join(", ", imported), imported));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "同步失败: " + e.getMessage()));
        }
    }

    /** 获取用户刷题情况 */
    @GetMapping("/user-stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> userStats() {
        List<User> users = userMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Long userId = user.getId();

            long totalAnswered = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId));
            long totalCorrect = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId).eq(UserAnswer::getIsCorrect, 1));
            long wrongCount = wrongQuestionMapper.selectCount(
                    new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));

            // 各题型统计
            long singleAnswered = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId).eq(UserAnswer::getQuestionType, "single"));
            long singleCorrect = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId).eq(UserAnswer::getQuestionType, "single").eq(UserAnswer::getIsCorrect, 1));
            long multipleAnswered = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId).eq(UserAnswer::getQuestionType, "multiple"));
            long multipleCorrect = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId).eq(UserAnswer::getQuestionType, "multiple").eq(UserAnswer::getIsCorrect, 1));
            long judgmentAnswered = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId).eq(UserAnswer::getQuestionType, "judgment"));
            long judgmentCorrect = userAnswerMapper.selectCount(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId).eq(UserAnswer::getQuestionType, "judgment").eq(UserAnswer::getIsCorrect, 1));

            // 考试次数及成绩
            long examCount = examRecordMapper.selectCount(
                    new LambdaQueryWrapper<ExamRecord>()
                            .eq(ExamRecord::getUserId, userId)
                            .eq(ExamRecord::getStatus, "completed"));

            // 考试平均分和最高分
            double examAvgScore = 0;
            int examBestScore = 0;
            if (examCount > 0) {
                List<ExamRecord> examRecords = examRecordMapper.selectList(
                        new LambdaQueryWrapper<ExamRecord>()
                                .eq(ExamRecord::getUserId, userId)
                                .eq(ExamRecord::getStatus, "completed"));
                examBestScore = examRecords.stream().mapToInt(ExamRecord::getScore).max().orElse(0);
                examAvgScore = examRecords.stream().mapToInt(ExamRecord::getScore).average().orElse(0);
                examAvgScore = Math.round(examAvgScore * 100.0) / 100.0;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", userId);
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("totalAnswered", totalAnswered);
            item.put("totalCorrect", totalCorrect);
            item.put("wrongCount", wrongCount);
            item.put("accuracy", totalAnswered > 0 ? Math.round((double) totalCorrect / totalAnswered * 10000) / 100.0 : 0);
            item.put("singleAnswered", singleAnswered);
            item.put("singleCorrect", singleCorrect);
            item.put("singleAccuracy", singleAnswered > 0 ? Math.round((double) singleCorrect / singleAnswered * 10000) / 100.0 : 0);
            item.put("multipleAnswered", multipleAnswered);
            item.put("multipleCorrect", multipleCorrect);
            item.put("multipleAccuracy", multipleAnswered > 0 ? Math.round((double) multipleCorrect / multipleAnswered * 10000) / 100.0 : 0);
            item.put("judgmentAnswered", judgmentAnswered);
            item.put("judgmentCorrect", judgmentCorrect);
            item.put("judgmentAccuracy", judgmentAnswered > 0 ? Math.round((double) judgmentCorrect / judgmentAnswered * 10000) / 100.0 : 0);
            item.put("examCount", examCount);
            item.put("examAvgScore", examAvgScore);
            item.put("examBestScore", examBestScore);
            result.add(item);
        }

        // 按答题数降序排列
        result.sort((a, b) -> Long.compare((Long) b.get("totalAnswered"), (Long) a.get("totalAnswered")));

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
