package com.jr.service.impl;

import com.jr.entity.SysUser;
import com.jr.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        // 把数据库角色（中文）映射成 Security 角色（英文）
        String role = user.getRole();
        String authority;
        if ("管理员".equals(role)) {
            authority = "ROLE_ADMIN";
        } else if ("会员".equals(role)) {
            authority = "ROLE_MEMBER";
        } else {
            authority = "ROLE_USER";
        }
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authority)
                .build();
    }
}
