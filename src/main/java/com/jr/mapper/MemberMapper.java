package com.jr.mapper;

import com.jr.entity.Member;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MemberMapper {

    @Insert("insert into member (userid,platenumber,platenumberstate,spareplatenumber,spareplatenumberstate,opendate,expiredate,status) values(#{userid},#{platenumber},'否',#{spareplatenumber},'否',#{opendate},#{expiredate},'有效')")
    int insertMember(Member member);

    @Select("select * from member where userid=#{userid}")
    Member selectMemberByUserId(Integer userid);

    @Select("select * from member where expiredate < now()")
    List<Member> selectExpiredMembers();

    /** 会员管理页：全部会员，快到期的排前面 */
    @Select("select * from member order by expiredate")
    List<Member> selectAll();

    @Update("update member set status='过期' where userid=#{userid}")
    int updateStatusExpired(Integer userid);

    @Update("update member set opendate=#{opendate},expiredate=#{expiredate},status='有效' where userid=#{userid}")
    int updateMember(Member member);

    @Select("SELECT CASE WHEN platenumber = #{platenumber} THEN 'platenumber'\n" +
            "WHEN spareplatenumber = #{platenumber} THEN 'spareplatenumber'\n" +
            "ELSE '未找到'\n" +
            "END FROM member\n" +
            "WHERE platenumber = #{platenumber} OR spareplatenumber = #{platenumber}\n" +
            "LIMIT 1")
    String selectnameByplatenumber(String platenumber);

    @Select("select * from member where platenumber=#{platenumber} or spareplatenumber=#{platenumber} limit 1")
    Member selectMemberByPlatenumber(String platenumber);

    @Update("update member set platenumberstate='是' where platenumber=#{platenumber}")
    int updateplatenumberstateopen(String platenumber);

    @Update("update member set spareplatenumberstate='是' where spareplatenumber=#{platenumber}")
    int updatespareplatenumberstateopen(String platenumber);

    @Update("update member set platenumberstate='否' where platenumber=#{platenumber}")
    int updateplatenumberstateclose(String platenumber);

    @Update("update member set spareplatenumberstate='否' where spareplatenumber=#{platenumber}")
    int updatespareplatenumberstateclose(String platenumber);

    @Select("select platenumber from member where userid=#{userid}")
    String selectplatenumberByUserid(Integer userid);

    @Select("select spareplatenumber from member where userid=#{userid}")
    String selectspareplatenumberByUserid(Integer userid);

    @Select("select platenumberstate from member where platenumber=#{platenumber}")
    String selectplatenumberstateByplatenumber(String platenumber);

    @Select("select spareplatenumberstate from member where spareplatenumber=#{spareplatenumber}")
    String selectspareplatenumberstateByspareplatenumber(String spareplatenumber);

    @Update("update member set expiredate=adddate(expiredate,interval 1 month) where userid=#{userid}")
    int updateaddexpiredate(Integer userid);

    @Update("update member set avatar=#{avatar} where userid=#{userid}")
    int updateAvatar(Integer userid, String avatar);
}
