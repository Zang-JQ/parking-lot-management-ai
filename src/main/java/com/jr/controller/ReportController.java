package com.jr.controller;

import com.jr.dto.ReportData;
import com.jr.mapper.ParkingRecordMapper;
import com.jr.mapper.PaymentRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 收费统计报表。
 * 全部按 paymentrecord / parkingrecord 实时统计——不读 income 表，
 * 因为那张表是累加出来的，历史上有漂移（paymentrecord 合计 4135，income 却是 4280）。
 */
@RestController
@RequestMapping("Report/")
public class ReportController {

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;
    @Autowired
    private ParkingRecordMapper parkingRecordMapper;

    @RequestMapping("summary")
    public ReportData summary() {
        ReportData data = new ReportData();
        data.setTodayIncome(round2(paymentRecordMapper.sumTodayIncome()));
        data.setMonthIncome(round2(paymentRecordMapper.sumMonthIncome()));
        data.setYearIncome(round2(paymentRecordMapper.sumYearIncome()));
        data.setTotalIncome(round2(paymentRecordMapper.sumTotalIncome()));

        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = firstDay.plusMonths(1).atStartOfDay();

        // 每天出场车次
        Map<String, Integer> carCount = new HashMap<>();
        for (Map<String, Object> row : parkingRecordMapper.selectDailyCarCount(from, to)) {
            Object day = row.get("d");
            if (day != null) {
                carCount.put(String.valueOf(day), ((Number) row.get("c")).intValue());
            }
        }

        // 每天的收入，按订单类型拆开：[0]=停车费，[1]=会员充值
        Map<String, double[]> money = new HashMap<>();
        for (Map<String, Object> row : paymentRecordMapper.selectDailyByType(from, to)) {
            Object day = row.get("d");
            if (day == null) {
                continue;
            }
            String date = String.valueOf(day);
            String type = String.valueOf(row.get("t"));
            double amount = row.get("s") == null ? 0d : ((Number) row.get("s")).doubleValue();
            double[] bucket = money.computeIfAbsent(date, k -> new double[2]);
            if ("会员充值".equals(type)) {
                bucket[1] += amount;
            } else {
                bucket[0] += amount;
            }
        }

        // 收入和车次任一存在的日期都要出现在报表里
        Set<String> dates = new HashSet<>();
        dates.addAll(money.keySet());
        dates.addAll(carCount.keySet());
        List<String> sorted = new ArrayList<>(dates);
        sorted.sort(Comparator.reverseOrder());

        List<ReportData.DailyRow> rows = new ArrayList<>();
        int monthCars = 0;
        for (String date : sorted) {
            double[] bucket = money.getOrDefault(date, new double[2]);
            int cars = carCount.getOrDefault(date, 0);
            monthCars += cars;
            rows.add(new ReportData.DailyRow(
                    date, cars,
                    round2(bucket[0]),
                    round2(bucket[1]),
                    round2(bucket[0] + bucket[1])));
        }

        data.setDaily(rows);
        data.setMonthCarCount(monthCars);
        return data;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
