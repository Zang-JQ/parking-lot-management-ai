package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车位网格里的一格：车位基础信息 + 当前停在上面那辆车的信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotView {
    private String spotcode;
    /** 普通 / 会员 */
    private String spottype;
    /** 空闲 / 已占用 / 维修中（以在场记录为准做过校正） */
    private String status;
    private String remark;
    /** 只有被占用时才有值 */
    private String platenumber;
    private String entrytime;
    private int minutes;
}
