package com.jr.service;

import com.jr.dto.AiChatResult;
import com.jr.dto.AiHistoryItem;

import java.util.List;

/**
 * AI 业务助手：自然语言问答 + 工具调用（查库回答，而不是让模型瞎编）。
 */
public interface AiAssistantService {

    /** 文本模型是否已配置好（没配 key 时接口返回友好提示，不抛异常） */
    boolean isConfigured();

    /**
     * 与助手对话。
     *
     * @param userId  当前登录用户，工具内部只查这个人的数据
     * @param role    当前用户角色（管理员 / 会员 / 普通用户），用于控制收入类工具
     * @param message 用户问题
     */
    AiChatResult chat(int userId, String role, String message);

    /** 清空某个用户的对话记忆（登出 / 手动清空时调用） */
    void clearMemory(int userId);

    /** 取某个用户的历史对话，供前端刷新后回放 */
    List<AiHistoryItem> history(int userId);
}
