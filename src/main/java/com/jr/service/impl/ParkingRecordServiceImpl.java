package com.jr.service.impl;

import com.jr.entity.Member;
import com.jr.entity.ParkingRecord;
import com.jr.mapper.*;
import com.jr.service.FeeRuleService;
import com.jr.service.ParkingRecordService;
import com.jr.util.ParkingFeeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class ParkingRecordServiceImpl implements ParkingRecordService {
    @Autowired
    private ParkingRecordMapper parkingRecordMapper;
    @Autowired
    private ParkingSpotMapper parkingSpotMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private PaymentRecordMapper paymentRecordMapper;
    @Autowired
    private IncomeMapper incomeMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    /** 计费规则来自 feerule 表（管理员可改），不再写死在工具类里 */
    @Autowired
    private FeeRuleService feeRuleService;
    /** Redis 熔断器：Redis 挂了不要每次都等超时 */
    @Autowired
    private com.jr.util.RedisGuard redisGuard;

    @Override
    public Boolean addParkingRecord(ParkingRecord parkingRecord) {
        /*入场前置校验：该用户已有在场车辆则拒绝（与出场侧"先看在场记录"对称）*/
        if (parkingRecordMapper.countParkingNowByUserid(parkingRecord.getUserid()) > 0) {
            return false;
        }
        String s = sysUserMapper.SelectusernameById(parkingRecord.getUserid());
        parkingRecord.setEntrytime(LocalDateTime.now());
        int i = parkingRecordMapper.insertParkingRecord(parkingRecord);
        int zhanyong = parkingSpotMapper.zhanyong(parkingRecord.getSpotcode());
        clearSpotCache();
        int updatestatsclose = sysUserMapper.updatestatsopen(s);
        if(i>0&&zhanyong>0&&updatestatsclose>0) {
            return true;
        }else{
            return false;
        }

    }

    @Override
    public ParkingRecord findParkingRecordByUserid(Integer userid) {
        return parkingRecordMapper.selectParkingRecordByUserId(userid);
    }

    @Override
    public Boolean updateParkingRecord(ParkingRecord parkingRecord) {
        /*获取车牌号*/
        parkingRecord.setPlatenumber(parkingRecordMapper.selectParkingRecordByUserId(parkingRecord.getUserid()).getPlatenumber());
        /*获取入场时间*/
        parkingRecord.setEntrytime(parkingRecordMapper.selectExitTime(parkingRecord.getUserid()));
        /*获取车位编号*/
        parkingRecord.setSpotcode(parkingRecordMapper.selectSpotcodeByUserId(parkingRecord.getUserid()));
        /*修改车位占用*/
        int kongxian = parkingSpotMapper.kongxian(parkingRecord.getSpotcode());
        clearSpotCache();
        /*加入出场时间*/
        parkingRecord.setExittime(LocalDateTime.now());
        /*算时间和钱*/
        int i=ParkingFeeUtil.calculateMinutes(parkingRecord.getEntrytime(),parkingRecord.getExittime());
        double j=ParkingFeeUtil.calculateFee(parkingRecord.getEntrytime(),parkingRecord.getExittime(), feeRuleService.current());
        parkingRecord.setDuration(i);
        parkingRecord.setFee(j);
        /*先结算在场记录，再判断该用户是否还有其他车在场：没有才关闭用户在场标记*/
        int i1 = parkingRecordMapper.updateParkingRecord(parkingRecord);
        int remain = parkingRecordMapper.countParkingNowByUserid(parkingRecord.getUserid());
        int updatestatsclose = remain == 0 ? sysUserMapper.updatestatsclose(sysUserMapper.SelectusernameById(parkingRecord.getUserid())) : 1;
        int i2 = paymentRecordMapper.insertUserPaymentRecord(parkingRecord.getUserid(), parkingRecord.getExittime(), parkingRecord.getPlatenumber(), parkingRecord.getFee());
        incomeMapper.updateIncomeadddailyincome(parkingRecord.getFee());
        if(kongxian>0&&updatestatsclose>0&&i1>0&&i2>0) {
            return true;
        }else{
            return false;
        }
    }

    @Override
    public List<ParkingRecord> findByUserid(Integer userid) {
        return parkingRecordMapper.selectByUserId(userid);
    }

    @Override
    public int findCountByUserid(Integer userid) {
        return parkingRecordMapper.selectCountByUserid(userid);
    }

    @Override
    public Double findSumFeeByUserid(Integer userid) {
        return parkingRecordMapper.selectSumFeeByUserid(userid);
    }

    @Override
    public String findplatenumberByUserid(Integer userid) {
        return parkingRecordMapper.selectplatenumberByUserid(userid);
    }

    @Override
    public Boolean updateisfreetomember(Integer userid) {
        return parkingRecordMapper.updateisfreetomember(userid)>0;
    }

    @Override
    public Boolean addmemberParkingRecord(ParkingRecord parkingRecord) {
        /*入场前置校验：该车牌已有在场记录则拒绝（防止同一辆车重复入场）*/
        if (parkingRecordMapper.selectParkingRecordByPlatenumber(parkingRecord.getPlatenumber()) != null) {
            return false;
        }
        int zhanyong = parkingSpotMapper.zhanyong(parkingRecord.getSpotcode());
        clearSpotCache();
        parkingRecord.setEntrytime(LocalDateTime.now());
        int i = parkingRecordMapper.insertmemberParkingRecord(parkingRecord);
        String name = memberMapper.selectnameByplatenumber(parkingRecord.getPlatenumber());
        int updateplatenumberstateopen=0;
        int updatespareplatenumberstateopen=0;
        if("platenumber".equals(name)){
            updateplatenumberstateopen = memberMapper.updateplatenumberstateopen(parkingRecord.getPlatenumber());
        }else if("spareplatenumber".equals(name)){
            updatespareplatenumberstateopen = memberMapper.updatespareplatenumberstateopen(parkingRecord.getPlatenumber());
        }else{
            return false;
        }
        if(zhanyong>0&&i>0&&(updateplatenumberstateopen>0||updatespareplatenumberstateopen>0)) {
            return true;
        }else{
            return false;
        }
    }

    @Override
    public ParkingRecord findParkingRecordByPlatenumber(String platenumber) {
        return parkingRecordMapper.selectParkingRecordByPlatenumber(platenumber);
    }

    @Override
    public Boolean updatememberParkingRecord(ParkingRecord  parkingRecord) {
        /*① 查在场记录（拿入场时间、真实userid）*/
        ParkingRecord record = parkingRecordMapper.selectParkingRecordByPlatenumber(parkingRecord.getPlatenumber());
        if (record == null) {
            return false; // 没有在场记录，直接失败
        }
        /*获得入场时间*/
        parkingRecord.setEntrytime(record.getEntrytime());
        /*获得出场时间*/
        parkingRecord.setExittime(LocalDateTime.now());
        /*获得时间间隔*/
        parkingRecord.setDuration(ParkingFeeUtil.calculateMinutes(parkingRecord.getEntrytime(),parkingRecord.getExittime()));
        /*② 判断会员是否过期：没查到会员记录 或 已过期 → 按普通用户收费出场*/
        Member member = memberMapper.selectMemberByPlatenumber(parkingRecord.getPlatenumber());
        boolean expired=member==null||member.getExpiredate()==null|| member.getExpiredate().isBefore(LocalDateTime.now());
        if (expired) {
            /*算出费用*/
            double fee = ParkingFeeUtil.calculateFee(parkingRecord.getEntrytime(), parkingRecord.getExittime(), feeRuleService.current());
            parkingRecord.setFee(fee);
            /*写入缴费记录（用在场记录里的真实userid）*/
            paymentRecordMapper.insertUserPaymentRecord(record.getUserid(), parkingRecord.getExittime(),parkingRecord.getPlatenumber(),fee);
            incomeMapper.updateIncomeadddailyincome(fee);
        }
        /*修改车位占用情况*/
        int kongxian = parkingSpotMapper.kongxian(parkingRecord.getSpotcode());
        clearSpotCache();
        /*修改停车记录（过期会带上fee，未过期fee为null=免费）*/
        int i = parkingRecordMapper.updatememberParkingRecord(parkingRecord);
        /*出场后若该用户已无其他在场车辆，才关闭用户在场标记（双车牌场景保留"是"）*/
        int remain = parkingRecordMapper.countParkingNowByUserid(record.getUserid());
        if (remain == 0) {
            sysUserMapper.updatestatsclose(sysUserMapper.SelectusernameById(record.getUserid()));
        }
        /*修改会员绑定车辆状态*/
        String name = memberMapper.selectnameByplatenumber(parkingRecord.getPlatenumber());
        int updateplatenumberstateclose=0;
        int updatespareplatenumberstateclose=0;
        if("platenumber".equals(name)){
            updateplatenumberstateclose = memberMapper.updateplatenumberstateclose(parkingRecord.getPlatenumber());
        }else if("spareplatenumber".equals(name)){
            updatespareplatenumberstateclose = memberMapper.updatespareplatenumberstateclose(parkingRecord.getPlatenumber());
        }else{
            return false;
        }
        if(kongxian>0&&i>0&&(updateplatenumberstateclose>0||updatespareplatenumberstateclose>0)){
            return true;
        }else{
            return false;
        }

    }

    /** 删除车位列表缓存：入场/出场后调用，保证下次查询是新的车位状态 */
    private void clearSpotCache() {
        // 熔断打开说明 Redis 不可用，直接跳过，别让出入场流程陪着等超时；
        // 但要记一笔欠账，等 Redis 恢复后由 ParkingSpotServiceImpl 补删，否则会读到旧缓存
        if (!redisGuard.allow()) {
            redisGuard.markDirty();
            return;
        }
        try {
            redisTemplate.delete("spotlist");
            redisGuard.success();
        } catch (Exception e) {
            redisGuard.failure(e);
            redisGuard.markDirty();
        }
    }



}
