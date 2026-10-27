package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员管理列表的一行：会员表 + 用户表的姓名电话。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberView {
    private Integer userid;
    private String realname;
    private String phone;
    private String platenumber;
    private String spareplatenumber;
    private String opendate;
    private String expiredate;
    private String status;
    /** 距离到期还有几天，负数表示已过期 */
    private long daysLeft;
}
