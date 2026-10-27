package com.jr.service.impl;

import com.jr.ai.ParkingAssistant;
import com.jr.ai.ParkingTools;
import com.jr.dto.AiChatResult;
import com.jr.dto.AiHistoryItem;
import com.jr.mapper.IncomeMapper;
import com.jr.mapper.MemberMapper;
import com.jr.mapper.ParkingRecordMapper;
import com.jr.mapper.ParkingSpotMapper;
import com.jr.service.AiAssistantService;
import com.jr.service.FeeRuleService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 业务助手实现：LangChain4j AiServices + 工具调用。
 *
 * 每次请求按「当前用户」构造一套 tools 和 AiServices，
 * 保证模型只能查到本人的数据；对话记忆按 userId 存在内存里，多轮上下文不丢。
 *
 * 记忆的生命周期（三件套）：
 *   1. 登出即清   → SysUserController.logout 调 clearMemory()
 *   2. 空闲过期   → 下面的 sweepIdleMemories() 定时扫
 *   3. 手动清空   → 前端 🗑 调 Ai/clear
 */
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);

    /** 每个会话保留最近 12 条消息（约 6 轮问答），够用又不至于把 token 烧光 */
    private static final int MAX_MEMORY_MESSAGES = 12;

    @Autowired
    private ParkingSpotMapper parkingSpotMapper;
    @Autowired
    private ParkingRecordMapper parkingRecordMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private IncomeMapper incomeMapper;
    @Autowired
    private FeeRuleService feeRuleService;

    @Value("${ai.chat.enabled:true}")
    private boolean enabled;

    @Value("${ai.chat.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${ai.chat.api-key:}")
    private String apiKey;

    @Value("${ai.chat.model-name:qwen-plus}")
    private String modelName;

    @Value("${ai.chat.timeout-seconds:45}")
    private int timeoutSeconds;

    @Value("${ai.chat.max-retries:1}")
    private int maxRetries;

    /** 会话空闲多少分钟算过期 */
    @Value("${ai.chat.memory-idle-minutes:30}")
    private int memoryIdleMinutes;

    private ChatModel chatModel;

    /** 对话记忆存储：按 userId 隔离，进程内保存，重启即清空 */
    private final InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.warn("[AI助手] ai.chat.enabled=false，助手接口不可用");
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AI助手] 未配置 ai.chat.api-key（或环境变量 aliQwen-api），助手接口不可用");
            return;
        }
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey.trim())
                .modelName(modelName)
                .temperature(0.2)
                .maxTokens(600)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .logRequests(false)
                .logResponses(false)
                .build();
        log.info("[AI助手] 已启用：baseUrl={}, model={}", baseUrl, modelName);
    }

    @Override
    public boolean isConfigured() {
        return chatModel != null;
    }

    @Override
    public AiChatResult chat(int userId, String role, String message) {
        if (chatModel == null) {
            return AiChatResult.fail("AI 助手未启用：请在 application.yml 的 ai.chat.api-key "
                    + "或环境变量 aliQwen-api 中配置大模型密钥");
        }
        if (message == null || message.isBlank()) {
            return AiChatResult.fail("请输入你想问的问题");
        }
        if (message.length() > 500) {
            return AiChatResult.fail("问题太长了（超过 500 字），请精简一下");
        }

        try {
            ParkingTools tools = new ParkingTools(
                    parkingSpotMapper, parkingRecordMapper, memberMapper, incomeMapper,
                    feeRuleService.current(), userId, role);

            ParkingAssistant assistant = AiServices.builder(ParkingAssistant.class)
                    .chatModel(chatModel)
                    .tools(tools)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(MAX_MEMORY_MESSAGES)
                            .chatMemoryStore(memoryStore)
                            .build())
                    // 最多连续调 4 次工具，防止模型在工具之间来回打转
                    .maxSequentialToolsInvocations(4)
                    .build();

            String answer = assistant.chat(String.valueOf(userId), message);
            if (answer == null || answer.isBlank()) {
                return AiChatResult.fail("模型没有返回内容，请换个问法再试");
            }
            log.info("[AI助手] user={} 问：{}", userId, message);
            log.info("[AI助手] user={} 答：{}", userId, answer);
            return AiChatResult.ok(answer.trim());
        } catch (Exception e) {
            log.error("[AI助手] 调用失败 userId={}", userId, e);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (reason.length() > 100) {
                reason = reason.substring(0, 100) + "…";
            }
            return AiChatResult.fail("助手暂时不可用（" + reason + "），请稍后再试");
        }
    }

    @Override
    public void clearMemory(int userId) {
        memoryStore.deleteMessages(String.valueOf(userId));
        log.info("[AI助手] 已清空 user={} 的对话记忆", userId);
    }

    @Override
    public List<AiHistoryItem> history(int userId) {
        List<ChatMessage> messages = memoryStore.getMessages(String.valueOf(userId));
        List<AiHistoryItem> items = new ArrayList<>();
        for (ChatMessage message : messages) {
            items.addAll(toView(message));
        }
        return items;
    }

    /**
     * 定时清理空闲会话：默认每 10 分钟扫一次，超过 30 分钟没动静的会话整桶删掉。
     * 间隔由 ai.chat.memory-sweep-ms 控制，空闲阈值由 ai.chat.memory-idle-minutes 控制。
     */
    @Scheduled(fixedDelayString = "${ai.chat.memory-sweep-ms:600000}")
    public void sweepIdleMemories() {
        long idleMillis = Duration.ofMinutes(Math.max(1, memoryIdleMinutes)).toMillis();
        int removed = memoryStore.evictIdle(idleMillis);
        if (removed > 0) {
            log.info("[AI助手] 清理空闲会话 {} 个，当前存活会话 {} 个", removed, memoryStore.size());
        }
    }

    /** 把 LangChain4j 的消息转成前端能渲染的一条记录；系统提示词和工具结果不回显 */
    private List<AiHistoryItem> toView(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            String text = userMessage.contents().stream()
                    .filter(c -> c instanceof TextContent)
                    .map(c -> ((TextContent) c).text())
                    .collect(Collectors.joining("\n"));
            if (text == null || text.isBlank()) {
                return List.of();
            }
            return List.of(new AiHistoryItem("user", text));
        }
        if (message instanceof AiMessage aiMessage) {
            // 带工具调用的 AiMessage 是模型的「中间过程」（例如“正在查询当前车位情况……”），
            // 不是最终答复，回放时跳过，否则历史里会多出一堆过程话术
            if (aiMessage.hasToolExecutionRequests()) {
                return List.of();
            }
            String text = aiMessage.text();
            if (text == null || text.isBlank()) {
                return List.of();
            }
            return List.of(new AiHistoryItem("bot", text.trim()));
        }
        return List.of();
    }

    /**
     * 进程内的记忆存储，按 memoryId（这里是 userId）分桶。
     * 额外记录每个桶的最后活跃时间，供空闲清理使用。
     */
    private static class InMemoryChatMemoryStore implements ChatMemoryStore {

        private final Map<Object, List<ChatMessage>> store = new ConcurrentHashMap<>();
        private final Map<Object, Long> lastActive = new ConcurrentHashMap<>();

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return new ArrayList<>(store.getOrDefault(memoryId, List.of()));
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            store.put(memoryId, new ArrayList<>(messages));
            lastActive.put(memoryId, System.currentTimeMillis());
        }

        @Override
        public void deleteMessages(Object memoryId) {
            store.remove(memoryId);
            lastActive.remove(memoryId);
        }

        /** 删掉超过 idleMillis 没活跃过的会话，返回删除个数 */
        int evictIdle(long idleMillis) {
            long now = System.currentTimeMillis();
            int removed = 0;
            for (Map.Entry<Object, Long> entry : lastActive.entrySet()) {
                if (now - entry.getValue() > idleMillis) {
                    Object memoryId = entry.getKey();
                    if (lastActive.remove(memoryId) != null) {
                        store.remove(memoryId);
                        removed++;
                    }
                }
            }
            return removed;
        }

        int size() {
            return store.size();
        }
    }
}
