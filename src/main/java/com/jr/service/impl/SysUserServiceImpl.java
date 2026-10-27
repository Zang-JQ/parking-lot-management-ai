package com.jr.service.impl;

import com.jr.entity.SysUser;
import com.jr.mapper.SysUserMapper;
import com.jr.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public String findstats(String username) {
        return sysUserMapper.selectstats(username);
    }

    @Override
    public Boolean updatestatsopen(String username) {
        return sysUserMapper.updatestatsopen(username)>0;
    }

    @Override
    public Boolean updatestatsclose(String username) {
        return sysUserMapper.updatestatsclose(username)>0;

    }

    @Override
    public SysUser findSysUserById(Integer id) {
        return sysUserMapper.selectSyeUserById(id);
    }

    @Override
    public Boolean addUser(SysUser sysUser)                                                {
        return sysUserMapper.insertUser(sysUser)>0;
    }


}
