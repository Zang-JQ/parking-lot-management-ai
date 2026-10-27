package com.jr.mapper;

import com.jr.entity.ParkingSpot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ParkingSpotMapper {

    @Select("select * from parkingspot")
    List<ParkingSpot> selectAll();

    @Select("select * from parkingspot where status='空闲' and spotcode like 'A%'")
    List<ParkingSpot> selectfreeSpot();

    @Select("select * from parkingspot where status='空闲'")
    List<ParkingSpot> selectAllParkingSpot();

    @Update("update parkingspot set status='已占用' where spotcode=#{spotcode}")
    int zhanyong(String spotcode);

    @Update("update parkingspot set status='空闲' where spotcode=#{spotcode}")
    int kongxian(String spotcode);

    /* ================= 车位管理（增删改查） ================= */

    @Select("select * from parkingspot where spotcode=#{spotcode}")
    ParkingSpot selectBySpotcode(String spotcode);

    @Insert("insert into parkingspot (spotcode, spottype, status, remark) "
            + "values(#{spotcode}, #{spottype}, #{status}, #{remark})")
    int insertSpot(ParkingSpot spot);

    @Update("update parkingspot set spottype=#{spottype}, status=#{status}, remark=#{remark} "
            + "where spotcode=#{spotcode}")
    int updateSpot(ParkingSpot spot);

    @Delete("delete from parkingspot where spotcode=#{spotcode}")
    int deleteSpot(String spotcode);

    /** 该车位是否还有在场车辆（有就不允许删除/改成空闲） */
    @Select("select count(*) from parkingrecord where spotcode=#{spotcode} and status='停车中'")
    int countParkingNowBySpotcode(String spotcode);
}
