package com.jr.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentRecordMapper {

    @Insert("insert into paymentRecord (userid,paytime,platenumber,ordertype,amount) values(#{userid},#{paytime},#{platenumber},'停车费',#{amount})")
    int insertUserPaymentRecord(Integer userid, LocalDateTime paytime, String platenumber, Double amount);

    @Insert("insert into paymentRecord (userid,paytime,platenumber,ordertype,amount) values(#{userid},#{paytime},#{platenumber},'会员充值',200.0)")
    int insertMemberPaymentRecord(Integer userid, LocalDateTime paytime, String platenumber);

    /* ================= 统计用（仪表盘 / 报表） ================= */

    /** 今日总收入 */
    @Select("select ifnull(sum(amount),0) from paymentrecord where date(paytime)=curdate()")
    double sumTodayIncome();

    /** 本月总收入 */
    @Select("select ifnull(sum(amount),0) from paymentrecord where date_format(paytime,'%Y-%m')=date_format(curdate(),'%Y-%m')")
    double sumMonthIncome();

    /**
     * 近 7 天每日收入。
     * 返回列：d=日期、s=金额、c=笔数；没有收入的日期不会出现在结果里，前端补 0。
     */
    @Select("select date(paytime) as d, ifnull(sum(amount),0) as s, count(*) as c "
            + "from paymentrecord where paytime >= date_sub(curdate(), interval 6 day) "
            + "group by date(paytime) order by d")
    List<Map<String, Object>> selectDailyIncomeLast7();

    /** 本年总收入 */
    @Select("select ifnull(sum(amount),0) from paymentrecord where year(paytime)=year(curdate())")
    double sumYearIncome();

    /** 累计总收入 */
    @Select("select ifnull(sum(amount),0) from paymentrecord")
    double sumTotalIncome();

    /**
     * 指定区间内，按「日期 + 订单类型」聚合的收入。
     * 返回列：d=日期、t=订单类型、s=金额、c=笔数。
     */
    @Select("select date(paytime) as d, ordertype as t, ifnull(sum(amount),0) as s, count(*) as c "
            + "from paymentrecord where paytime >= #{from} and paytime < #{to} "
            + "group by date(paytime), ordertype order by d desc")
    List<Map<String, Object>> selectDailyByType(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);
}
