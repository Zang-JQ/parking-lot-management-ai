package com.jr.controller;

import com.jr.dto.PlateResult;
import com.jr.service.PlateRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 车牌识别接口：前端把照片 POST 上来，后端返回识别到的车牌号。
 * 只做识别，不落库、不存文件（FastDFS 那台机器目前也不可用）。
 */
@RestController
@RequestMapping("Plate/")
public class PlateController {

    @Autowired
    private PlateRecognitionService plateRecognitionService;

    @RequestMapping("recognize")
    public PlateResult recognize(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return PlateResult.fail("请先选择一张车牌照片");
        }
        try {
            return plateRecognitionService.recognize(file.getBytes(), file.getContentType());
        } catch (IOException e) {
            return PlateResult.fail("读取上传的文件失败，请重新选择照片");
        }
    }

    /** 照片超过 spring.servlet.multipart.max-file-size 时不要再抛 500，给前端一句人话 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public PlateResult handleTooLarge(MaxUploadSizeExceededException e) {
        return PlateResult.fail("图片太大了，请换一张小一点的，或等前端压缩后重试");
    }
}
