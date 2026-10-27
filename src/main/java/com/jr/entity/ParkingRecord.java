package com.jr.entity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 停车记录表 parkingrecord
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class ParkingRecord {
    /** 主键ID */
    private Integer id;
    /** 用户ID */
    private Integer userid;
    /** 车牌号 */
    private String platenumber;
    /** 车位编号 */
    private String spotcode;
    /** 入场时间（精确到秒） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime entrytime;
    /** 出场时间（精确到秒） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exittime;
    /** 停车时长（分钟） */
    private Integer duration;
    /** 停车费用 */
    private Double fee;
    /** 是否免费：免费 / 收费 */
    private String isfree;
    /** 状态：停车中 / 已结算 */
    private String status;
}