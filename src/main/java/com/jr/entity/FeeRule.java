package com.jr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 费率规则表 feerule（全局只有一行）。
 * 以前这些值写死在 ParkingFeeUtil 里，现在从库里读，管理员页面能改。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeRule {
    private Integer id;
    private String rulename;
    /** 免费时长（分钟） */
    private Integer freeminutes;
    /** 首小时费用（元） */
    private Double firsthourfee;
    /** 每多一小时递增（元） */
    private Double incrementalfee;
    /** 每小时封顶（元） */
    private Double maxhourfee;
    /** 每日封顶（元） */
    private Double maxdayfee;
}
