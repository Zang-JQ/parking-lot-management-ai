package com.jr.mapper;

import com.jr.entity.ParkingRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ParkingRecordMapper {
    @Insert("insert into parkingrecord (userid,platenumber,spotcode,entrytime) value(#{userid},#{platenumber},#{spotcode},#{entrytime})")
    int insertParkingRecord(ParkingRecord parkingRecord);

    @Select("select * from parkingrecord where userid=#{userid} and status='停车中' order by id desc limit 1")
    ParkingRecord selectParkingRecordByUserId(Integer userid);

    @Update("update parkingrecord set exittime=#{exittime},duration=#{duration},fee=#{fee},status='已结算' where userid=#{userid} and status='停车中'")
    int updateParkingRecord(ParkingRecord parkingRecord);

    @Select("select entrytime from parkingrecord where userid=#{userid} and status='停车中' order by id desc limit 1")
    LocalDateTime selectExitTime(Integer userid);

    @Select("select spotcode from parkingrecord where userid=#{userid} and status='停车中' order by id desc limit 1")
    String selectSpotcodeByUserId(Integer userid);

    @Select("select * from parkingrecord")
    List<ParkingRecord> selectAll();

    @Select("select * from parkingrecord where userid=#{userid}")
    List<ParkingRecord> selectByUserId(Integer userid);

    @Select("select count(userid) from parkingrecord where userid=#{userid}")
    int selectCountByUserid(Integer userid);

    @Select("select sum(fee) from parkingrecord where userid=#{userid}")
    Double selectSumFeeByUserid(Integer userid);

    @Select("select platenumber from parkingrecord where userid=#{userid} and status='停车中' order by id desc limit 1")
    String selectplatenumberByUserid(Integer userid);

    @Update("update parkingrecord set isfree='免费' where userid=#{userid} and status='停车中'")
    int updateisfreetomember(Integer userid);

    @Insert("insert into parkingrecord (userid,platenumber,spotcode,entrytime,isfree) values(#{userid},#{platenumber},#{spotcode},#{entrytime},'免费')")
    int insertmemberParkingRecord(ParkingRecord parkingRecord);

    @Select("select * from parkingrecord where platenumber=#{platenumber} and status='停车中' order by id desc limit 1")
    ParkingRecord selectParkingRecordByPlatenumber(String platenumber);

    @Select("select count(*) from parkingrecord where userid=#{userid} and status='停车中'")
    int countParkingNowByUserid(Integer userid);

    @Update("update parkingrecord set exittime=#{exittime},duration=#{duration},fee=#{fee},status='已结算' where platenumber=#{platenumber} and status='停车中'")
    int updatememberParkingRecord(ParkingRecord parkingRecord);

    /** 全场当前在场车辆数（仪表盘用） */
    @Select("select count(*) from parkingrecord where status='停车中'")
    int countParkingNow();

    /** 全场当前在场车辆明细（车位网格 / 仪表盘列表用） */
    @Select("select * from parkingrecord where status='停车中' order by entrytime")
    List<ParkingRecord> selectAllParkingNow();

    /** 指定区间内每天出场的车次（报表用） */
    @Select("select date(exittime) as d, count(*) as c from parkingrecord "
            + "where status='已结算' and exittime >= #{from} and exittime < #{to} "
            + "group by date(exittime)")
    List<java.util.Map<String, Object>> selectDailyCarCount(@org.apache.ibatis.annotations.Param("from") LocalDateTime from,
                                                            @org.apache.ibatis.annotations.Param("to") LocalDateTime to);

}
