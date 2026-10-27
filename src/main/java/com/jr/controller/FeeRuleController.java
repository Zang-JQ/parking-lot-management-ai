package com.jr.controller;

import com.jr.dto.ApiResult;
import com.jr.entity.FeeRule;
import com.jr.service.FeeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 费率规则：读当前规则 / 保存新规则。
 * 保存成功后计费立刻按新规则走（服务里缓存已刷新）。
 */
@RestController
@RequestMapping("FeeRule/")
public class FeeRuleController {

    @Autowired
    private FeeRuleService feeRuleService;

    @RequestMapping("get")
    public FeeRule get() {
        return feeRuleService.current();
    }

    @RequestMapping("save")
    public ApiResult save(FeeRule rule) {
        if (rule.getRulename() == null || rule.getRulename().isBlank()) {
            rule.setRulename("标准费率规则");
        }
        boolean ok = feeRuleService.save(rule);
        if (!ok) {
            return ApiResult.fail("保存失败：请检查各项数值范围（免费时长 0~1440 分钟，每小时封顶不能低于首小时费用）");
        }
        return ApiResult.ok("费率规则已保存，下一笔出场立即按新规则计费");
    }
}
