package com.jr.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

/**
 * 车位信息表 parkingspot
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class ParkingSpot {
    /** 主键ID */
    private Integer id;
    /** 车位编号（如A001） */
    private String spotcode;
    /** 车位类型：普通 */
    private String spottype;
    /** 状态：空闲 / 占用 / 维修 */
    private String status;
    /** 备注 */
    private String remark;
}