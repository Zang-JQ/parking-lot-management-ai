package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 助手的对话回复。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResult {
    /** 是否成功 */
    private Boolean success;
    /** 模型的回答（成功时） */
    private String answer;
    /** 失败原因 / 提示 */
    private String message;

    public static AiChatResult ok(String answer) {
        return new AiChatResult(true, answer, null);
    }

    public static AiChatResult fail(String message) {
        return new AiChatResult(false, null, message);
    }
}
