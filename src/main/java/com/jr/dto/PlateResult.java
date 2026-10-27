package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车牌识别结果。
 * 注意：它不是数据库表对应的实体，只是「上传照片 → 识别车牌」接口的返回结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlateResult {
    /** 是否识别到了车牌 */
    private Boolean success;
    /** 清洗后的车牌号；识别失败时为空串 */
    private String plate;
    /** 是否符合中国大陆车牌格式：true 直接可用；false 需要用户手动核对 */
    private Boolean valid;
    /** 给前端直接显示的中文提示 */
    private String message;

    public static PlateResult ok(String plate, boolean valid, String message) {
        return new PlateResult(true, plate, valid, message);
    }

    public static PlateResult fail(String message) {
        return new PlateResult(false, "", false, message);
    }
}
