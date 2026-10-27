package com.jr.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jr.dto.PlateResult;
import com.jr.service.PlateRecognitionService;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 车牌识别实现：LangChain4j + OpenAI 兼容协议的视觉模型。
 *
 * 流程：图片字节 → Base64 → UserMessage(文本提示 + ImageContent) → ChatModel → 解析 JSON → 清洗 + 正则校验。
 * 因为走的是「OpenAI 兼容」协议，换平台（智谱/豆包/中转）只改 application.yml 里的 base-url 和 model-name。
 */
@Service
public class PlateRecognitionServiceImpl implements PlateRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(PlateRecognitionServiceImpl.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 中国大陆车牌格式：
     * 省份简称 + 字母 + 4~5 位字母数字 + 1 位尾字符，
     * 同时覆盖 7 位普通车牌、8 位新能源车牌、以及挂/学/警/港/澳/领/使 等尾字。
     */
    private static final Pattern PLATE_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领]"
                    + "[A-HJ-NP-Z]"
                    + "[A-HJ-NP-Z0-9]{4,5}"
                    + "[A-HJ-NP-Z0-9挂学警港澳领使试超]$");

    /** 提示词：只让它返回一个 JSON，避免一堆解释文字影响解析 */
    private static final String PROMPT = """
            你是车牌识别引擎。请识别这张图片中最主要的机动车号牌（画面里有多辆车时，取位于中央、最大、最清晰的那一辆）。
            只输出一个 JSON 对象，不要输出任何解释，不要加 markdown 代码块，格式如下：
            {"plate":"京A12345","confidence":0.95,"note":""}
            要求：
            1. plate 只保留号牌本身的字符，去掉空格、圆点、横线等分隔符；汉字保持原样，字母统一大写。
            2. 如果图片里没有车牌，或者完全看不清，plate 返回空字符串 ""。
            3. confidence 是 0 到 1 之间的数字，表示你的把握程度。
            4. note 只在看不清时填一句简短原因，否则为空字符串。
            """;

    @Value("${vision.enabled:true}")
    private boolean enabled;

    @Value("${vision.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${vision.api-key:}")
    private String apiKey;

    @Value("${vision.model-name:qwen3-vl-plus}")
    private String modelName;

    @Value("${vision.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${vision.max-retries:1}")
    private int maxRetries;

    /** 密钥没配好时为 null，接口返回友好提示而不是报错 */
    private ChatModel chatModel;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.warn("[车牌识别] vision.enabled=false，识别接口不可用");
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[车牌识别] 未配置 vision.api-key（或环境变量 DASHSCOPE_API_KEY），识别接口不可用");
            return;
        }
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey.trim())
                .modelName(modelName)
                .temperature(0.0)
                .maxTokens(512)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                // 请求日志里是整个 base64 图片，会把控制台刷爆，必须关掉
                .logRequests(false)
                .logResponses(true)
                .build();
        log.info("[车牌识别] 已启用：baseUrl={}, model={}", baseUrl, modelName);
    }

    @Override
    public boolean isConfigured() {
        return chatModel != null;
    }

    @Override
    public PlateResult recognize(byte[] imageBytes, String contentType) {
        if (chatModel == null) {
            return PlateResult.fail("车牌识别未启用：请在 application.yml 的 vision.api-key "
                    + "或环境变量 DASHSCOPE_API_KEY 中配置大模型密钥");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return PlateResult.fail("图片内容为空，请重新拍照");
        }
        if (imageBytes.length > 8 * 1024 * 1024) {
            return PlateResult.fail("图片太大了（超过 8MB），请压缩后再识别");
        }

        String mimeType = (contentType != null && contentType.startsWith("image/")) ? contentType : "image/jpeg";
        try {
            UserMessage userMessage = UserMessage.from(
                    TextContent.from(PROMPT),
                    ImageContent.from(Base64.getEncoder().encodeToString(imageBytes), mimeType));

            ChatResponse response = chatModel.chat(userMessage);
            String text = (response == null || response.aiMessage() == null) ? null : response.aiMessage().text();
            log.info("[车牌识别] 模型原始返回：{}", text);

            return parse(text);
        } catch (Exception e) {
            log.error("[车牌识别] 调用视觉模型失败", e);
            return PlateResult.fail("识别服务调用失败（" + briefReason(e) + "），可以手动输入车牌");
        }
    }

    /** 解析模型返回的 JSON，并做清洗与格式校验 */
    private PlateResult parse(String text) {
        String json = extractJson(text);
        if (json == null) {
            return PlateResult.fail("模型没有返回可解析的结果，可以手动输入车牌");
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            String plate = normalize(node.path("plate").asText(""));
            if (plate.isEmpty()) {
                String note = node.path("note").asText("").trim();
                return PlateResult.fail(note.isEmpty()
                        ? "没有从照片里识别到车牌，请对准车牌重新拍照，或手动输入"
                        : "没有识别到车牌（" + note + "），可以手动输入");
            }
            boolean valid = PLATE_PATTERN.matcher(plate).matches();
            String message = valid
                    ? "识别成功：" + plate + "，确认无误后点「确认入场」"
                    : "识别到「" + plate + "」，但不像标准车牌，请手动核对一下";
            return PlateResult.ok(plate, valid, message);
        } catch (Exception e) {
            log.warn("[车牌识别] JSON 解析失败：{}", text);
            return PlateResult.fail("识别结果解析失败，可以手动输入车牌");
        }
    }

    /** 模型有时会包 ```json 代码块或加解释，这里只截取第一个 { ... } */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return t.substring(start, end + 1);
    }

    /**
     * 车牌清洗：全角转半角 → 转大写 → 去掉分隔符 → 去掉「车牌号」之类前缀。
     * 例如 "吉 b·44444"、"车牌号：京A12345" 都能清洗成标准形式。
     */
    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (char c : raw.toCharArray()) {
            if (c == '\u3000') {
                c = ' ';
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                c = (char) (c - 0xFEE0); // 全角 ASCII 转半角
            }
            sb.append(c);
        }
        String s = sb.toString().toUpperCase(Locale.ROOT);
        s = s.replaceAll("[\\s·.．\\-—_/:：()（）\\[\\]【】]", "");
        s = s.replaceFirst("^(车牌号码|车牌号|车牌|号牌|PLATE)", "");
        return s.trim();
    }

    /** 异常信息太长，只取前 80 字给前端看 */
    private String briefReason(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        msg = msg.replaceAll("\\s+", " ").trim();
        return msg.length() > 80 ? msg.substring(0, 80) + "…" : msg;
    }
}
