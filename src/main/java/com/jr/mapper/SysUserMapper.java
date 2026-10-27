package com.jr.mapper;

import com.jr.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("select * from sysuser where username=#{username}")
    SysUser selectByUsername(String username);

    @Select("select stats from sysuser where username=#{username}")
    String selectstats(String username);

    @Update("update sysuser set stats='是' where username=#{username}")
    int updatestatsopen(String username);

    @Update("update sysuser set stats='否' where username=#{username}")
    int updatestatsclose(String username);

    @Select("select username from sysuser where id=#{userid}")
    String SelectusernameById(Integer id);

    @Update("update sysuser set role='会员' where id=#{id}")
    int updateSyeUserToMember(Integer id);

    @Update("update sysuser set role='普通用户' where id=#{userid}")
    int updateRoleToNormal(Integer userid);

    @Select("select * from sysuser where id=#{id}")
    SysUser selectSyeUserById(Integer id);

    /*====================================*/
    @Select("select * from sysuser")
    List<SysUser> selectAll();

    @Update("update sysuser set password=#{password} where id=#{id}")
    int updatePassword(SysUser sysUser);

    @Insert("insert into sysuser (username,password,realname,phone,role) values(#{username},#{password},#{realname},#{phone},#{role})")
    int insertUser(SysUser sysUser);
}
