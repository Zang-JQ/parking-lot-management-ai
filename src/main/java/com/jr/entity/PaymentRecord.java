package com.jr.entity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 支付订单表 PaymentRecord
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class PaymentRecord {
    /** 主键ID */
    private Integer id;
    /** 用户ID */
    private Integer userid;
    /** 支付时间（精确到秒） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paytime;
    /** 车牌号 */
    private String platenumber;
    /** 订单类型：停车费 / 会员充值 */
    private String ordertype;
    /** 订单金额（元） */
    private Double amount;

}