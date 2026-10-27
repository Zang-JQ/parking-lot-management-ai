package com.jr.mapper;

import com.jr.entity.Income;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IncomeMapper {
    @Select("select * from income")
    Income selectIncome();

    @Update("update income set dailyincome=(dailyincome+#{dailyincome})," +
            "monthlyincome=(monthlyincome+#{dailyincome})," +
            "yearlyincome=(yearlyincome+#{dailyincome})," +
            "totalincome=(totalincome+#{dailyincome})")
    int updateIncomeadddailyincome(Double dailyincome);

    @Update("update income set dailyincome=0")
    int updateIncomecleandailyincome();
    @Update("update income set dailyincome=0,monthlyincome=0")
    int updateIncomecleanmonthlyincome();
    @Update("update income set dailyincome=0,monthlyincome=0,yearlyincome=0")
    int updateIncomecleanyearlyincome();
}
