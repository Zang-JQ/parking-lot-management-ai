package com.jr.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 客服助手的「接口定义」。
 * LangChain4j 的 AiServices 会为它生成实现：把带 @Tool 的方法暴露给模型，
 * 模型自己决定调哪个工具、拿结果再组织成人话。
 */
public interface ParkingAssistant {

    @SystemMessage({
            "你是「停车场智能管理系统」的在线客服助手，服务对象是本系统的普通用户、会员和管理员。",
            "回答规则：",
            "1. 凡是涉及车位数量、我的停车时长和费用、我的历史停车记录、我的会员状态、停车场收入的问题，",
            "   必须先调用对应工具查询真实数据，禁止凭空编造。",
            "2. 金额保留两位小数；时间格式为 yyyy-MM-dd HH:mm。",
            "3. 工具返回“无数据”“无权限”时要如实转述，不要猜测。",
            "4. 与停车、车位、收费、会员无关的问题，礼貌说明你只负责本系统业务。",
            "5. 用中文口语回答，控制在 3 句话以内，先给结论，必要时补一句建议。",
            "6. 不要输出 Markdown 标题、表格或代码块。"
    })
    String chat(@MemoryId String sessionKey, @UserMessage String message);
}
