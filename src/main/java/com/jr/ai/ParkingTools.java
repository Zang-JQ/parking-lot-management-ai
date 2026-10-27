package com.jr.ai;

import com.jr.entity.Income;
import com.jr.entity.Member;
import com.jr.entity.ParkingRecord;
import com.jr.entity.ParkingSpot;
import com.jr.mapper.IncomeMapper;
import com.jr.mapper.MemberMapper;
import com.jr.mapper.ParkingRecordMapper;
import com.jr.mapper.ParkingSpotMapper;
import com.jr.util.ParkingFeeUtil;
import dev.langchain4j.agent.tool.Tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 给 AI 模型调用的「工具集」。
 *
 * 关键点：这个对象是**按用户构造**的（构造时绑定 userId 和角色），
 * 所以模型无论怎么问，都只能查到当前登录用户自己的数据，不会串号。
 */
public class ParkingTools {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ParkingSpotMapper spotMapper;
    private final ParkingRecordMapper recordMapper;
    private final MemberMapper memberMapper;
    private final IncomeMapper incomeMapper;
    /** 当前生效的费率规则，估价时跟真实计费保持一致 */
    private final com.jr.entity.FeeRule feeRule;

    /** 当前登录用户，工具内部一律用它，不从模型参数里取，避免越权查询 */
    private final int userId;
    private final String role;

    public ParkingTools(ParkingSpotMapper spotMapper,
                        ParkingRecordMapper recordMapper,
                        MemberMapper memberMapper,
                        IncomeMapper incomeMapper,
                        com.jr.entity.FeeRule feeRule,
                        int userId,
                        String role) {
        this.spotMapper = spotMapper;
        this.recordMapper = recordMapper;
        this.memberMapper = memberMapper;
        this.incomeMapper = incomeMapper;
        this.feeRule = feeRule;
        this.userId = userId;
        this.role = role;
    }

    @Tool("查询停车场当前车位概况：空闲、已占用、维修中的数量，以及空闲车位的编号（最多列 10 个）。")
    public String spotSummary() {
        List<ParkingSpot> all = spotMapper.selectAll();
        if (all == null || all.isEmpty()) {
            return "车位数据为空，暂时查不到车位信息。";
        }
        long free = count(all, "空闲");
        long used = count(all, "已占用");
        long repair = count(all, "维修中");
        String freeCodes = all.stream()
                .filter(s -> "空闲".equals(s.getStatus()))
                .map(ParkingSpot::getSpotcode)
                .limit(10)
                .collect(Collectors.joining("、"));
        return String.format("车位共 %d 个：空闲 %d、已占用 %d、维修中 %d。空闲编号：%s%s。",
                all.size(), free, used, repair,
                freeCodes.isEmpty() ? "无" : freeCodes,
                free > 10 ? " 等" : "");
    }

    @Tool("查询我（当前登录用户）的在场情况：是否在场、车牌号、车位号、入场时间、已停分钟数、按计费规则预估的停车费。")
    public String myParkingStatus() {
        ParkingRecord record = recordMapper.selectParkingRecordByUserId(userId);
        if (record == null || record.getEntrytime() == null) {
            return "当前不在场：没有查到你的在场停车记录。如果要停车，请到「车辆入场登记」页面。";
        }
        LocalDateTime now = LocalDateTime.now();
        int minutes = ParkingFeeUtil.calculateMinutes(record.getEntrytime(), now);
        String feeText;
        Member member = memberMapper.selectMemberByUserId(userId);
        boolean memberValid = member != null && member.getExpiredate() != null
                && member.getExpiredate().isAfter(now);
        if (memberValid) {
            feeText = "会员有效期内，免费";
        } else {
            double fee = ParkingFeeUtil.calculateFee(record.getEntrytime(), now, feeRule);
            feeText = String.format("%.2f 元", fee);
        }
        return String.format("你在场：车牌 %s，车位 %s，入场时间 %s，已停 %d 分钟，预计费用 %s。",
                record.getPlatenumber(), record.getSpotcode(),
                record.getEntrytime().format(FMT), minutes, feeText);
    }

    @Tool("查询我最近 N 天的停车记录汇总：停车次数、累计花费，以及最近 5 条明细。参数 days 是天数，例如 7 或 30。")
    public String myRecentRecords(int days) {
        int safeDays = Math.max(1, Math.min(days <= 0 ? 7 : days, 365));
        List<ParkingRecord> all = recordMapper.selectByUserId(userId);
        if (all == null || all.isEmpty()) {
            return "你还没有任何停车记录。";
        }
        LocalDateTime from = LocalDateTime.now().minusDays(safeDays);
        List<ParkingRecord> recent = all.stream()
                .filter(r -> r.getEntrytime() != null && r.getEntrytime().isAfter(from))
                .sorted(Comparator.comparing(ParkingRecord::getEntrytime).reversed())
                .collect(Collectors.toList());
        if (recent.isEmpty()) {
            return String.format("最近 %d 天没有停车记录。", safeDays);
        }
        double total = recent.stream()
                .mapToDouble(r -> r.getFee() == null ? 0.0 : r.getFee())
                .sum();
        String detail = recent.stream().limit(5)
                .map(r -> String.format("%s %s（%s，%s 分钟，%s）",
                        r.getEntrytime().format(FMT),
                        r.getPlatenumber(),
                        r.getSpotcode(),
                        r.getDuration() == null ? "-" : r.getDuration(),
                        r.getFee() == null ? "免费" : String.format("%.2f 元", r.getFee())))
                .collect(Collectors.joining("；"));
        return String.format("最近 %d 天你停车 %d 次，累计花费 %.2f 元。最近的记录：%s。",
                safeDays, recent.size(), total, detail);
    }

    @Tool("查询我的会员信息：是不是会员、开通日期、到期日期、是否还在有效期、绑定的主车牌和副车牌。")
    public String myMembership() {
        Member m = memberMapper.selectMemberByUserId(userId);
        if (m == null) {
            return "你还不是会员，目前按普通用户标准按次计费。可以在「充值会员」页面开通。";
        }
        LocalDateTime now = LocalDateTime.now();
        boolean valid = m.getExpiredate() != null && m.getExpiredate().isAfter(now);
        long leftDays = m.getExpiredate() == null ? 0
                : Math.max(0, java.time.Duration.between(now, m.getExpiredate()).toDays());
        return String.format("会员状态：%s。开通 %s，到期 %s（%s）。绑定车牌：%s，副车牌：%s。",
                valid ? "有效期内" : "已过期",
                m.getOpendate() == null ? "-" : m.getOpendate().format(FMT),
                m.getExpiredate() == null ? "-" : m.getExpiredate().format(FMT),
                valid ? "还剩 " + leftDays + " 天" : "需续费",
                m.getPlatenumber() == null ? "-" : m.getPlatenumber(),
                m.getSpareplatenumber() == null ? "无" : m.getSpareplatenumber());
    }

    @Tool("查询停车场的收入概况：今日、本月、本年、累计收入。仅管理员可用。")
    public String incomeOverview() {
        if (!"管理员".equals(role)) {
            return "无权限：收入数据只有管理员可以查询。";
        }
        Income income = incomeMapper.selectIncome();
        if (income == null) {
            return "收入数据为空。";
        }
        return String.format("收入：今日 %.2f 元，本月 %.2f 元，本年 %.2f 元，累计 %.2f 元。",
                nz(income.getDailyincome()), nz(income.getMonthlyincome()),
                nz(income.getYearlyincome()), nz(income.getTotalincome()));
    }

    private long count(List<ParkingSpot> list, String status) {
        return list.stream().filter(s -> status.equals(s.getStatus())).count();
    }

    private double nz(Double v) {
        return v == null ? 0.0 : v;
    }
}
