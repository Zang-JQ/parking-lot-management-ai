package com.jr.mapper;

import com.jr.entity.FeeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FeeRuleMapper {

    /** 全局只有一行规则 */
    @Select("select * from feerule order by id limit 1")
    FeeRule selectRule();

    @Update("update feerule set rulename=#{rulename}, freeminutes=#{freeminutes}, "
            + "firsthourfee=#{firsthourfee}, incrementalfee=#{incrementalfee}, "
            + "maxhourfee=#{maxhourfee}, maxdayfee=#{maxdayfee} where id=#{id}")
    int updateRule(FeeRule rule);
}
