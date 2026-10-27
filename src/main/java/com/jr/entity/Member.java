package com.jr.entity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 会员信息表 member
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class Member {
    /** 主键ID */
    private Integer id;
    /** 用户ID */
    private Integer userid;
    /** 绑定车牌号 */
    private String platenumber;
    /** 绑定车牌号在场状态 */
    private String platenumberstate;
    /** 备选车牌号 */
    private String spareplatenumber;
    /** 备选车牌号在场状态 */
    private String spareplatenumberstate;
    /** 开通时间（精确到秒） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opendate;
    /** 到期时间（精确到秒） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredate;
    /** 开通价格（元） */
    private Double price;
    /** 有效天数 */
    private Integer perioddays;
    /** 状态：有效 / 过期 */
    private String status;
    /** 头像URL */
    private String avatar;
}