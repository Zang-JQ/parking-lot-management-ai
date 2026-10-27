package com.jr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

/**
 * 系统用户表 sysuser
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class SysUser {
    /** 主键ID */
    private Integer id;
    /** 登录账号 */
    private String username;
    /** 登录密码（加密存储） */
    private String password;
    /** 真实姓名 */
    private String realname;
    /** 手机号 */
    private String phone;
    /** 角色：管理员 / 普通用户 / 会员 */
    private String role;
    /** 是否停车 */
    private String stats;
    /** 状态：启用 / 禁用 */
    private String status;
}