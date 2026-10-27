package com.jr.controller;

import com.jr.dto.DashboardData;
import com.jr.entity.ParkingRecord;
import com.jr.entity.ParkingSpot;
import com.jr.mapper.ParkingRecordMapper;
import com.jr.mapper.ParkingSpotMapper;
import com.jr.mapper.PaymentRecordMapper;
import com.jr.util.ParkingFeeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘聚合接口。
 * 一次请求返回前端要的全部数字，避免仪表盘开五六个 Ajax。
 * 所有数字都来自数据库实时统计，没有任何硬编码。
 */
@RestController
@RequestMapping("Dashboard/")
public class DashboardController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter AXIS = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private ParkingSpotMapper parkingSpotMapper;
    @Autowired
    private ParkingRecordMapper parkingRecordMapper;
    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @RequestMapping("summary")
    public DashboardData summary() {
        // 1) 车位状态统计
        List<ParkingSpot> spots = parkingSpotMapper.selectAll();
        int free = 0;
        int used = 0;
        int repair = 0;
        for (ParkingSpot spot : spots) {
            if ("空闲".equals(spot.getStatus())) {
                free++;
            } else if ("已占用".equals(spot.getStatus())) {
                used++;
            } else if ("维修中".equals(spot.getStatus())) {
                repair++;
            }
        }

        // 2) 在场车辆明细
        List<ParkingRecord> actives = parkingRecordMapper.selectAllParkingNow();
        LocalDateTime now = LocalDateTime.now();
        List<DashboardData.ActiveCar> cars = new ArrayList<>();
        for (ParkingRecord record : actives) {
            cars.add(new DashboardData.ActiveCar(
                    record.getPlatenumber(),
                    record.getSpotcode(),
                    record.getEntrytime() == null ? "-" : record.getEntrytime().format(TS),
                    record.getEntrytime() == null ? 0
                            : ParkingFeeUtil.calculateMinutes(record.getEntrytime(), now)));
        }

        // 3) 近 7 天收入：库里没收入的日期要补 0，否则折线会缺一段
        Map<String, Object[]> byDay = new HashMap<>();
        for (Map<String, Object> row : paymentRecordMapper.selectDailyIncomeLast7()) {
            Object day = row.get("d");
            if (day == null) {
                continue;
            }
            double amount = row.get("s") == null ? 0d : ((Number) row.get("s")).doubleValue();
            int count = row.get("c") == null ? 0 : ((Number) row.get("c")).intValue();
            byDay.put(String.valueOf(day), new Object[]{amount, count});
        }
        List<DashboardData.DailyStat> series = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Object[] value = byDay.get(day.toString());
            series.add(new DashboardData.DailyStat(
                    day.toString(),
                    day.format(AXIS),
                    value == null ? 0d : (Double) value[0],
                    value == null ? 0 : (Integer) value[1]));
        }

        DashboardData data = new DashboardData();
        data.setTotalSpots(spots.size());
        data.setFreeSpots(free);
        data.setUsedSpots(used);
        data.setRepairSpots(repair);
        data.setParkingNow(actives.size());
        data.setTodayIncome(round2(paymentRecordMapper.sumTodayIncome()));
        data.setMonthIncome(round2(paymentRecordMapper.sumMonthIncome()));
        data.setLast7days(series);
        data.setActiveCars(cars);
        return data;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
