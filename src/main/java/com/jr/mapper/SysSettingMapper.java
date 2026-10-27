package com.jr.mapper;

import com.jr.entity.SysSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysSettingMapper {

    @Select("select * from syssetting order by id limit 1")
    SysSetting selectSetting();

    @Update("update syssetting set parkingname=#{parkingname}, totalspots=#{totalspots}, "
            + "freeminutes=#{freeminutes}, contactphone=#{contactphone}, address=#{address} where id=#{id}")
    int updateSetting(SysSetting setting);
}
