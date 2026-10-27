package com.jr.service;

import com.jr.entity.Member;

import java.util.List;

public interface MemberService {

    Boolean addMember(Member member);

    Member findMemberByUserId(Integer userid);

    Boolean updateplatenumberstateopenByplatenumber(String platenumber);

    String findplatenumberByUserId(Integer userid);

    String findspareplatenumberByUserid(Integer userid);

    String findplatenumberstateByplatenumber(String platenumber);

    Boolean updateaddexpiredate(Integer userid);

    Boolean updateAvatar(Integer userid, String avatar);

}
