package com.jr.util;

import com.jr.entity.FeeRule;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 停车场计费工具类。
 *
 * 规则现在从 feerule 表读（管理员可在「费率规则管理」页改），这里只负责算。
 * 没传规则时（或库里没数据）退回到默认规则，保证老调用方行为不变：
 *   1. 停放时长按分钟算，向上取整
 *   2. 前 N 分钟免费（默认 15）
 *   3. 首小时 X 元（默认 5）
 *   4. 每多 1 小时 +Y 元（默认 1）
 *   5. 每小时封顶 Z 元（默认 10）
 *   6. 每日封顶 D 元（默认 60），满 24 小时拆天累加
 */
@Component
public class ParkingFeeUtil {

    /** 默认规则，和改造前的写死值保持一致 */
    private static final FeeRule DEFAULT = new FeeRule(
            1, "标准费率规则", 15, 5.0, 1.0, 10.0, 60.0);

    /**
     * 计算时间间隔，单位：分钟
     */
    public static int calculateMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        return (int) duration.toMinutes();
    }

    /**
     * 按默认规则计费（兼容老调用方）
     */
    public static double calculateFee(LocalDateTime startTime, LocalDateTime endTime) {
        return calculateFee(startTime, endTime, null);
    }

    /**
     * 按指定规则计费。rule 为 null 时用默认规则。
     */
    public static double calculateFee(LocalDateTime startTime, LocalDateTime endTime, FeeRule rule) {
        FeeRule r = normalize(rule);
        int totalMinute = calculateMinutes(startTime, endTime);
        if (totalMinute <= r.getFreeminutes()) {
            return 0.0;
        }

        int oneDayMinute = 24 * 60;
        int fullDayCount = totalMinute / oneDayMinute;
        int remainMinute = totalMinute % oneDayMinute;

        double totalFee = r.getMaxdayfee() * fullDayCount + calcSingleDayFee(remainMinute, r);
        return Math.round(totalFee * 100.0) / 100.0;
    }

    /**
     * 计算单日（不足 24h）费用
     */
    private static double calcSingleDayFee(int totalMinute, FeeRule r) {
        if (totalMinute <= r.getFreeminutes()) {
            return 0.0;
        }
        int totalHour = (totalMinute + 59) / 60;

        double sum = 0;
        for (int i = 1; i <= totalHour; i++) {
            // 第 i 小时价格：首小时价格 + 递增，最多不超过每小时封顶
            double hourPrice = Math.min(
                    r.getFirsthourfee() + (i - 1) * r.getIncrementalfee(),
                    r.getMaxhourfee());
            sum += hourPrice;
        }

        if (sum > r.getMaxdayfee()) {
            sum = r.getMaxdayfee();
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    /** 规则里任何一项为空都退回默认值，避免空指针把计费搞崩 */
    private static FeeRule normalize(FeeRule rule) {
        if (rule == null) {
            return DEFAULT;
        }
        FeeRule r = new FeeRule();
        r.setFreeminutes(rule.getFreeminutes() == null ? DEFAULT.getFreeminutes() : rule.getFreeminutes());
        r.setFirsthourfee(rule.getFirsthourfee() == null ? DEFAULT.getFirsthourfee() : rule.getFirsthourfee());
        r.setIncrementalfee(rule.getIncrementalfee() == null ? DEFAULT.getIncrementalfee() : rule.getIncrementalfee());
        r.setMaxhourfee(rule.getMaxhourfee() == null ? DEFAULT.getMaxhourfee() : rule.getMaxhourfee());
        r.setMaxdayfee(rule.getMaxdayfee() == null ? DEFAULT.getMaxdayfee() : rule.getMaxdayfee());
        return r;
    }
}
