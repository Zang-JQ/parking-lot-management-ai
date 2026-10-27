package com.jr.controller;

import com.jr.dto.AiChatResult;
import com.jr.dto.AiHistoryItem;
import com.jr.entity.SysUser;
import com.jr.mapper.SysUserMapper;
import com.jr.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 业务助手接口。
 *
 * 安全与成本控制：
 *   1. 用户身份优先取 SecurityContext，取不到才回退到前端传的 username；
 *   2. 每人每分钟最多 15 次对话，防止有人拿你的 key 刷额度。
 */
@RestController
@RequestMapping("Ai/")
public class AiController {

    /** 每分钟每人最多问几次 */
    private static final int LIMIT_PER_MINUTE = 15;

    @Autowired
    private AiAssistantService aiAssistantService;
    @Autowired
    private SysUserMapper sysUserMapper;

    /** userId -> [窗口开始毫秒, 本轮已用次数] */
    private final Map<Integer, long[]> buckets = new ConcurrentHashMap<>();

    @RequestMapping("chat")
    public AiChatResult chat(@RequestParam(value = "message", required = false) String message,
                             @RequestParam(value = "username", required = false) String username) {
        SysUser user = resolveUser(username);
        if (user == null) {
            return AiChatResult.fail("请先登录后再使用 AI 助手");
        }
        if (!allow(user.getId())) {
            return AiChatResult.fail("问得有点快，请等一分钟再试（每分钟最多 " + LIMIT_PER_MINUTE + " 次）");
        }
        return aiAssistantService.chat(user.getId(), user.getRole(), message);
    }

    /** 刷新页面后回放历史对话；没有历史就返回空数组 */
    @RequestMapping("history")
    public List<AiHistoryItem> history(@RequestParam(value = "username", required = false) String username) {
        SysUser user = resolveUser(username);
        if (user == null) {
            return List.of();
        }
        return aiAssistantService.history(user.getId());
    }

    /** 手动清空对话记忆 */
    @RequestMapping("clear")
    public AiChatResult clear(@RequestParam(value = "username", required = false) String username) {
        SysUser user = resolveUser(username);
        if (user == null) {
            return AiChatResult.fail("请先登录");
        }
        aiAssistantService.clearMemory(user.getId());
        return AiChatResult.ok("已清空当前对话");
    }

    /** 当前登录用户：优先用 Security 认证态，避免前端随便传 username 就能冒用别人 */
    private SysUser resolveUser(String usernameParam) {
        String name = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            name = auth.getName();
        }
        if (name == null || name.isBlank()) {
            name = usernameParam;
        }
        if (name == null || name.isBlank()) {
            return null;
        }
        return sysUserMapper.selectByUsername(name);
    }

    /** 固定窗口限流：同一个用户 60 秒内最多 LIMIT_PER_MINUTE 次 */
    private boolean allow(int userId) {
        long now = System.currentTimeMillis();
        long[] bucket = buckets.computeIfAbsent(userId, k -> new long[]{now, 0});
        synchronized (bucket) {
            if (now - bucket[0] > 60_000L) {
                bucket[0] = now;
                bucket[1] = 0;
            }
            if (bucket[1] >= LIMIT_PER_MINUTE) {
                return false;
            }
            bucket[1]++;
            return true;
        }
    }
}
