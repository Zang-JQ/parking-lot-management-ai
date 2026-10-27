package com.jr.service.impl;

import com.jr.entity.FeeRule;
import com.jr.mapper.FeeRuleMapper;
import com.jr.service.FeeRuleService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 费率规则实现：库里的规则缓存一份，避免每次计费都查库。
 * 保存后立刻刷新缓存，所以改完规则马上生效，不用重启。
 */
@Service
public class FeeRuleServiceImpl implements FeeRuleService {

    private static final Logger log = LoggerFactory.getLogger(FeeRuleServiceImpl.class);

    @Autowired
    private FeeRuleMapper feeRuleMapper;

    /** 缓存的一行规则；null 表示还没加载 */
    private volatile FeeRule cached;

    @PostConstruct
    public void init() {
        try {
            FeeRule rule = feeRuleMapper.selectRule();
            if (rule != null) {
                trimName(rule);
                cached = rule;
                log.info("[费率] 已加载规则：前{}分钟免费，首小时{}元，每小时+{}元，小时封顶{}元，每日封顶{}元",
                        rule.getFreeminutes(), rule.getFirsthourfee(),
                        rule.getIncrementalfee(), rule.getMaxhourfee(), rule.getMaxdayfee());
            } else {
                log.warn("[费率] feerule 表为空，使用代码里的默认规则");
            }
        } catch (Exception e) {
            log.warn("[费率] 启动时读规则失败，先用默认规则：{}", e.getMessage());
        }
    }

    @Override
    public FeeRule current() {
        FeeRule rule = cached;
        if (rule != null) {
            return rule;
        }
        synchronized (this) {
            if (cached == null) {
                FeeRule fromDb = null;
                try {
                    fromDb = feeRuleMapper.selectRule();
                } catch (Exception e) {
                    log.warn("[费率] 读库失败，用默认规则：{}", e.getMessage());
                }
                cached = fromDb == null ? defaultRule() : trimName(fromDb);
            }
            return cached;
        }
    }

    /** 库里的 rulename 可能带换行/空格，读出来顺手清理一下 */
    private FeeRule trimName(FeeRule rule) {
        if (rule.getRulename() != null) {
            rule.setRulename(rule.getRulename().trim());
        }
        return rule;
    }

    @Override
    public boolean save(FeeRule rule) {
        if (rule == null || rule.getId() == null) {
            return false;
        }
        if (!valid(rule)) {
            return false;
        }
        int rows = feeRuleMapper.updateRule(rule);
        if (rows > 0) {
            cached = rule;
            log.info("[费率] 规则已更新：{}", rule);
            return true;
        }
        return false;
    }

    @Override
    public void refresh() {
        synchronized (this) {
            cached = null;
        }
    }

    /** 范围校验：防止把免费时长填成 -100 之类把计费搞乱 */
    private boolean valid(FeeRule rule) {
        Integer free = rule.getFreeminutes();
        Double first = rule.getFirsthourfee();
        Double inc = rule.getIncrementalfee();
        Double maxHour = rule.getMaxhourfee();
        Double maxDay = rule.getMaxdayfee();
        if (free == null || free < 0 || free > 1440) {
            return false;
        }
        if (first == null || first < 0 || first > 1000) {
            return false;
        }
        if (inc == null || inc < 0 || inc > 1000) {
            return false;
        }
        if (maxHour == null || maxHour < 0 || maxHour > 1000) {
            return false;
        }
        if (maxDay == null || maxDay < 0 || maxDay > 100000) {
            return false;
        }
        // 每小时封顶不能低于首小时，否则算出来会怪
        return maxHour >= first;
    }

    private FeeRule defaultRule() {
        return new FeeRule(1, "标准费率规则", 15, 5.0, 1.0, 10.0, 60.0);
    }
}
