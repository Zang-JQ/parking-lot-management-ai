package com.jr.controller;

import com.jr.dto.MemberView;
import com.jr.entity.Member;
import com.jr.entity.SysUser;
import com.jr.mapper.MemberMapper;
import com.jr.mapper.SysUserMapper;
import com.jr.service.MemberService;
import com.jr.util.FastDFSUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("Member/")
public class MemberController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 会员管理列表：会员表 + 用户表的姓名/电话，并算好剩余天数。
     */
    @RequestMapping("list")
    public List<MemberView> list() {
        List<Member> members = memberMapper.selectAll();
        List<MemberView> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Member m : members) {
            SysUser user = sysUserMapper.selectSyeUserById(m.getUserid());
            long daysLeft = 0;
            if (m.getExpiredate() != null) {
                daysLeft = Duration.between(now, m.getExpiredate()).toDays();
            }
            list.add(new MemberView(
                    m.getUserid(),
                    user == null ? "-" : user.getRealname(),
                    user == null ? "-" : user.getPhone(),
                    m.getPlatenumber(),
                    m.getSpareplatenumber(),
                    m.getOpendate() == null ? "-" : m.getOpendate().format(TS),
                    m.getExpiredate() == null ? "-" : m.getExpiredate().format(TS),
                    daysLeft > 0 ? "有效" : "已过期",
                    daysLeft));
        }
        return list;
    }

    @RequestMapping("addMember")
    public Boolean addMember(Member member) {
        return memberService.addMember(member);
    }

    @RequestMapping("findMemberByUserId")
    public Member findMemberByUserId(Integer userid) {
        return memberService.findMemberByUserId(userid);
    }

    @RequestMapping("updateplatenumberstateopenByplatenumber")
    public Boolean updateplatenumberstateopenByplatenumber(Member member) {
        return memberService.updateplatenumberstateopenByplatenumber(member.getPlatenumber());
    }

    @RequestMapping("findplatenumberByUserId")
    public String findplatenumberByUserId(Integer userid) {
        return memberService.findplatenumberByUserId(userid);
    }

    @RequestMapping("findspareplatenumberByUserid")
    public String findspareplatenumberByUserid(Integer userid) {
        return memberService.findspareplatenumberByUserid(userid);
    }

    @RequestMapping("findplatenumberstateByplatenumber")
    public String findplatenumberstateByplatenumber(Member member) {
        return memberService.findplatenumberstateByplatenumber(member.getPlatenumber());
    }

    @RequestMapping("updateaddexpiredate")
    public Boolean updateaddexpiredate(Integer userid) {
        return memberService.updateaddexpiredate(userid);
    }

    /** 上传会员头像：存 FastDFS，返回图片URL */
    @RequestMapping("uploadAvatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file, Integer userid) {
        try {
            String[] ids = FastDFSUtils.uploadFile(file.getInputStream(), file.getOriginalFilename());
            if (ids == null) {
                return null;
            }
            String url = "http://192.168.126.128:8888/" + ids[0] + "/" + ids[1];
            memberService.updateAvatar(userid, url);
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
