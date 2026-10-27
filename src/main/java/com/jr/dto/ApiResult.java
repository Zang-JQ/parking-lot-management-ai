package com.jr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用的「保存成功/失败」返回体，给管理页的保存按钮用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult {
    private Boolean success;
    private String message;

    public static ApiResult ok(String message) {
        return new ApiResult(true, message);
    }

    public static ApiResult fail(String message) {
        return new ApiResult(false, message);
    }
}
