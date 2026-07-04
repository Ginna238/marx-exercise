package cn.ginna.marxexercise.admin;

import cn.ginna.marxexercise.user.QuestionBankLoader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GitHubBankUpdater {

    private static final Logger log = LoggerFactory.getLogger(GitHubBankUpdater.class);

    private static final String GITHUB_API = "https://api.github.com/repos/keggin-CHN/njfu_grinding/contents/";
    private static final String RAW_BASE = "https://raw.githubusercontent.com/keggin-CHN/njfu_grinding/main/";
    private static final String BANK_DIR = "题库收集";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final QuestionBankLoader questionBankLoader;

    public GitHubBankUpdater(ObjectMapper objectMapper, QuestionBankLoader questionBankLoader) {
        this.objectMapper = objectMapper;
        this.questionBankLoader = questionBankLoader;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** 获取 GitHub 上所有可用的题库文件清单 */
    public List<BankFileInfo> listAvailableBanks() throws Exception {
        // 先获取题库收集根目录
        List<GitHubEntry> rootEntries = fetchDir(BANK_DIR);
        List<BankFileInfo> result = new ArrayList<>();

        for (GitHubEntry entry : rootEntries) {
            if ("dir".equals(entry.type)) {
                // 进入子目录（如 2025-2026-2）
                List<BankFileInfo> subFiles = listJsonFiles(entry.path);
                result.addAll(subFiles);
            } else if ("file".equals(entry.type) && entry.name.endsWith(".json")) {
                result.add(new BankFileInfo(entry.name, entry.path,
                        RAW_BASE + entry.path, null));
            }
        }
        return result;
    }

    private List<BankFileInfo> listJsonFiles(String dirPath) throws Exception {
        List<GitHubEntry> entries = fetchDir(dirPath);
        return entries.stream()
                .filter(e -> "file".equals(e.type) && e.name.endsWith(".json"))
                .map(e -> new BankFileInfo(e.name, e.path, RAW_BASE + e.path, dirPath))
                .collect(Collectors.toList());
    }

    /** 下载并导入单个题库 */
    public String downloadAndImport(BankFileInfo file) throws Exception {
        log.info("正在下载题库: {} ({})", file.name, file.downloadUrl);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(file.downloadUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("下载失败，HTTP " + resp.statusCode());
        }

        String jsonContent = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
        // 验证格式
        Map<String, Object> parsed = objectMapper.readValue(jsonContent,
                new TypeReference<Map<String, Object>>() {});
        boolean valid = parsed.containsKey("单选题") || parsed.containsKey("多选题") || parsed.containsKey("判断题");
        if (!valid) {
            throw new RuntimeException("题库格式无效，缺少题型分类");
        }

        // 提取库名
        String bankName = file.name
                .replace(".json", "")
                .replaceAll("^\\d{4}-\\d{4}-\\d+-?", "")
                .replaceAll("^\\d+-?", "");

        // 调用已有的加载逻辑导入
        questionBankLoader.loadBank(file.name, bankName, jsonContent);
        log.info("题库 {} 导入完成", file.name);
        return bankName;
    }

    /** 扫描 GitHub 并导入新题库（已存在的跳过） */
    public List<String> syncAll() throws Exception {
        List<BankFileInfo> available = listAvailableBanks();
        List<String> imported = new ArrayList<>();
        for (BankFileInfo bf : available) {
            try {
                String name = downloadAndImport(bf);
                imported.add(name);
            } catch (Exception e) {
                log.warn("跳过 {}: {}", bf.name, e.getMessage());
            }
        }
        return imported;
    }

    private List<GitHubEntry> fetchDir(String path) throws Exception {
        String url = GITHUB_API + path;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 403) {
            throw new RuntimeException("GitHub API 限流，请稍后再试");
        }
        if (resp.statusCode() != 200) {
            throw new RuntimeException("获取目录失败: HTTP " + resp.statusCode());
        }
        return objectMapper.readValue(resp.body(),
                new TypeReference<List<GitHubEntry>>() {});
    }

    // --- 内部数据结构 ---

    public static class BankFileInfo {
        public String name;
        public String path;
        public String downloadUrl;
        public String dir;

        public BankFileInfo() {}
        public BankFileInfo(String name, String path, String downloadUrl, String dir) {
            this.name = name;
            this.path = path;
            this.downloadUrl = downloadUrl;
            this.dir = dir;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GitHubEntry {
        public String name;
        public String path;
        public String type;
        public String download_url;
    }
}
