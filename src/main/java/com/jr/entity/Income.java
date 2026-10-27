package com.jr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

/**
 *
 * 收入表
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class Income {
    /** 主键id */
    private Integer id;
    /** 日收入 */
    private Double dailyincome;
    /** 月收入 */
    private Double monthlyincome;
    /** 年收入 */
    private Double yearlyincome;
    /** 总收入 */
    private Double totalincome;
}
