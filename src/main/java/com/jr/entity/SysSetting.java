package com.jr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统设置表 syssetting（全局只有一行）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysSetting {
    private Integer id;
    /** 停车场名称 */
    private String parkingname;
    /** 总车位数 */
    private Integer totalspots;
    /** 免费停车时长（分钟），仅展示用，真正计费看 feerule */
    private Integer freeminutes;
    private String contactphone;
    private String address;
}
