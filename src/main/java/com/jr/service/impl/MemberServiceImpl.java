package com.jr.service.impl;

import com.jr.entity.Member;
import com.jr.mapper.IncomeMapper;
import com.jr.mapper.MemberMapper;
import com.jr.mapper.PaymentRecordMapper;
import com.jr.mapper.SysUserMapper;
import com.jr.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class MemberServiceImpl implements MemberService {
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private PaymentRecordMapper  paymentRecordMapper;
    @Autowired
    private IncomeMapper incomeMapper;

    @Override
    public Boolean addMember(Member member) {
        member.setOpendate(LocalDateTime.now());
        member.setExpiredate(LocalDateTime.now().plusDays(30));
        int i = sysUserMapper.updateSyeUserToMember(member.getUserid());
        /*已存在过期记录 → 更新恢复；不存在 → 插入*/
        Member exist = memberMapper.selectMemberByUserId(member.getUserid());
        int i1;
        if (exist != null) {
            i1 = memberMapper.updateMember(member);
        } else {
            i1 = memberMapper.insertMember(member);
        }
        /*缴费记录关联会员表真实生效的车牌（重新开通时以旧车牌为准）*/
        int i2 = paymentRecordMapper.insertMemberPaymentRecord(member.getUserid(), LocalDateTime.now(), memberMapper.selectplatenumberByUserid(member.getUserid()));
        incomeMapper.updateIncomeadddailyincome(200.0);
        if(i>0&&i1>0&&i2>0){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public Member findMemberByUserId(Integer userid) {
        return memberMapper.selectMemberByUserId(userid);
    }

    @Override
    public Boolean updateplatenumberstateopenByplatenumber(String platenumber) {
        String name = memberMapper.selectnameByplatenumber(platenumber);
        if("platenumber".equals(name)){
            return memberMapper.updateplatenumberstateopen(platenumber)>0;
        }else if("spareplatenumber".equals(name)){
            return memberMapper.updatespareplatenumberstateopen(platenumber)>0;
        }else{
            System.out.println("车牌号不正确");
            return false;
        }
    }

    @Override
    public String findplatenumberByUserId(Integer userid) {
        return memberMapper.selectplatenumberByUserid(userid);
    }

    @Override
    public String findspareplatenumberByUserid(Integer userid) {
        return memberMapper.selectspareplatenumberByUserid(userid);
    }

    @Override
    public String findplatenumberstateByplatenumber(String platenumber) {
        String name = memberMapper.selectnameByplatenumber(platenumber);
        if("platenumber".equals(name)){
            return memberMapper.selectplatenumberstateByplatenumber(platenumber);
        }else if("spareplatenumber".equals(name)){
            return memberMapper.selectspareplatenumberstateByspareplatenumber(platenumber);
        }else{
            System.out.println("车牌号不正确");
            return "false";
        }
    }

    @Override
    public Boolean updateAvatar(Integer userid, String avatar) {
        return memberMapper.updateAvatar(userid, avatar) > 0;
    }

    @Override
    public Boolean updateaddexpiredate(Integer userid) {
        int i = memberMapper.updateaddexpiredate(userid);
        String s = memberMapper.selectplatenumberByUserid(userid);
        int i1 = paymentRecordMapper.insertMemberPaymentRecord(userid, LocalDateTime.now(), s);
        incomeMapper.updateIncomeadddailyincome(200.0);
        if(i>0&&i1>0){
            return true;
        }else{
            return false;
        }
    }


}
