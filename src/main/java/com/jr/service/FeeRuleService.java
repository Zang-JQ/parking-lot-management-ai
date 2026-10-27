package com.jr.service;

import com.jr.entity.FeeRule;

/**
 * 费率规则服务：缓存一行规则，计费时直接取，改完立刻生效。
 */
public interface FeeRuleService {

    /** 当前生效的规则（带缓存，库为空时返回默认规则） */
    FeeRule current();

    /** 保存规则：校验 → 落库 → 刷新缓存 */
    boolean save(FeeRule rule);

    /** 清缓存，下次重新读库 */
    void refresh();
}
