package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 收费统计报表：汇总数字 + 本月每日明细。
 * 全部按 paymentrecord / parkingrecord 实时统计，不读 income 表那张会漂移的缓存。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {
    private double todayIncome;
    private double monthIncome;
    private double yearIncome;
    private double totalIncome;
    /** 本月出场车次 */
    private int monthCarCount;
    /** 本月每日明细，最新的在前 */
    private List<DailyRow> daily;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRow {
        private String date;
        /** 当天出场车次 */
        private int carCount;
        /** 停车费收入 */
        private double parkingFee;
        /** 会员充值收入 */
        private double memberFee;
        private double total;
    }
}
