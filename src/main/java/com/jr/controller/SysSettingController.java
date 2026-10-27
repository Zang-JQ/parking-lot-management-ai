package com.jr.controller;

import com.jr.dto.ApiResult;
import com.jr.entity.SysSetting;
import com.jr.mapper.SysSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置：读 / 存 syssetting 表。
 */
@RestController
@RequestMapping("SysSetting/")
public class SysSettingController {

    @Autowired
    private SysSettingMapper sysSettingMapper;

    @RequestMapping("get")
    public SysSetting get() {
        return sysSettingMapper.selectSetting();
    }

    @RequestMapping("save")
    public ApiResult save(SysSetting setting) {
        if (setting.getId() == null) {
            return ApiResult.fail("保存失败：缺少记录 ID");
        }
        if (setting.getParkingname() == null || setting.getParkingname().isBlank()) {
            return ApiResult.fail("停车场名称不能为空");
        }
        if (setting.getTotalspots() == null || setting.getTotalspots() <= 0) {
            return ApiResult.fail("总车位数必须是正整数");
        }
        if (setting.getFreeminutes() == null || setting.getFreeminutes() < 0) {
            return ApiResult.fail("免费停车时长不能为负数");
        }
        int rows = sysSettingMapper.updateSetting(setting);
        return rows > 0 ? ApiResult.ok("设置已保存") : ApiResult.fail("保存失败，请重试");
    }
}
