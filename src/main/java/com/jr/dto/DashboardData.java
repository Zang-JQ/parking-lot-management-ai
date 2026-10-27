package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 仪表盘聚合数据（一次请求给前端所有要显示的东西，避免页面开一堆 Ajax）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private int totalSpots;
    private int freeSpots;
    private int usedSpots;
    private int repairSpots;
    /** 当前在场车辆数 */
    private int parkingNow;
    /** 今日收入（按 paymentrecord 实算，跟图表口径一致） */
    private double todayIncome;
    /** 本月收入 */
    private double monthIncome;
    /** 近 7 天收入序列，缺数据的日期补 0 */
    private List<DailyStat> last7days;
    /** 在场车辆明细 */
    private List<ActiveCar> activeCars;

    /** 某一天的停车收入 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private String date;
        /** 图轴用的短格式 MM-dd */
        private String label;
        private double amount;
        private int count;
    }

    /** 一辆在场车 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveCar {
        private String platenumber;
        private String spotcode;
        private String entrytime;
        private int minutes;
    }
}
