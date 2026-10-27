package com.jr.service;

import com.jr.dto.PlateResult;

/**
 * 车牌识别服务（图片理解 / 多模态，不是文生图）。
 */
public interface PlateRecognitionService {

    /**
     * 识别一张照片里的车牌号。
     *
     * @param imageBytes  图片字节
     * @param contentType 图片 MIME 类型，如 image/jpeg
     * @return 识别结果，永不返回 null；失败时 success=false 且 message 说明原因
     */
    PlateResult recognize(byte[] imageBytes, String contentType);

    /** 大模型密钥是否已配置（未配置时接口直接返回友好提示，不会抛异常） */
    boolean isConfigured();
}
