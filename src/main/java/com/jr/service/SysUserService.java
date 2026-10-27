package com.jr.service;

import com.jr.entity.Member;
import com.jr.entity.SysUser;

public interface SysUserService {
    String findstats(String username);

    Boolean updatestatsopen(String username);

    Boolean updatestatsclose(String username);

    SysUser findSysUserById(Integer id);

    Boolean addUser(SysUser sysUser);


}
