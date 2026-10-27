package com.jr.service.impl;

import com.jr.entity.Income;
import com.jr.mapper.IncomeMapper;
import com.jr.service.IncomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class IncomeServiceImpl implements IncomeService {

    @Autowired
    private IncomeMapper incomeMapper;

    @Override
    public Income selectIncome() {
        return incomeMapper.selectIncome();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void playSth1(){
        incomeMapper.updateIncomecleandailyincome();
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void playSth2(){
        incomeMapper.updateIncomecleanmonthlyincome();
    }

    @Scheduled(cron = "0 0 0 1 1 ?")
    public void playSth3(){
        incomeMapper.updateIncomecleanyearlyincome();
    }


}
