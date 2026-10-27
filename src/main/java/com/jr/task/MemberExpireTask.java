package com.jr.task;

import com.jr.entity.Member;
import com.jr.mapper.MemberMapper;
import com.jr.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberExpireTask {

    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 每天凌晨3点执行一次 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void expireMembers() {
        List<Member> expiredList = memberMapper.selectExpiredMembers();
        for (Member member : expiredList) {
            memberMapper.updateStatusExpired(member.getUserid());
            sysUserMapper.updateRoleToNormal(member.getUserid());
        }
    }
}
