package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 助手的一条历史消息，给前端回放用。
 * role: user = 用户问的，bot = 助手答的。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiHistoryItem {
    private String role;
    private String text;
}
