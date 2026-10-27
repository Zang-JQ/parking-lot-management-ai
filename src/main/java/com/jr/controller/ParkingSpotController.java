package com.jr.controller;

import com.jr.dto.ApiResult;
import com.jr.dto.SpotView;
import com.jr.entity.ParkingRecord;
import com.jr.entity.ParkingSpot;
import com.jr.mapper.ParkingRecordMapper;
import com.jr.mapper.ParkingSpotMapper;
import com.jr.service.ParkingSpotService;
import com.jr.util.ParkingFeeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("ParkingSpot/")
public class ParkingSpotController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 车位状态：已占用是系统自动维护的，人工只能改这两个 */
    private static final String STATUS_FREE = "空闲";
    private static final String STATUS_USED = "已占用";
    private static final String STATUS_REPAIR = "维修中";

    @Autowired
    private ParkingSpotService parkingSpotService;
    @Autowired
    private ParkingSpotMapper parkingSpotMapper;
    @Autowired
    private ParkingRecordMapper parkingRecordMapper;

    @RequestMapping("findAll")
    public List<ParkingSpot> findAll() {
        return parkingSpotService.findAll();
    }

    @RequestMapping("findfreeSpot")
    public List<ParkingSpot> findfreeSpot() {
        return parkingSpotService.findfreeSpot();
    }

    @RequestMapping("findAllParkingSpot")
    public List<ParkingSpot> findAllParkingSpot() {
        return parkingSpotService.findAllParkingSpot();
    }

    /**
     * 车位网格：车位信息 + 当前停在这格上的车。
     * 状态以「在场记录」为准做一次校正——即使 parkingspot.status 忘了同步，
     * 有在场记录的车位也会显示成已占用，不会出现"空闲但停着车"。
     */
    @RequestMapping("grid")
    public List<SpotView> grid() {
        List<ParkingSpot> spots = parkingSpotService.findAll();
        List<ParkingRecord> actives = parkingRecordMapper.selectAllParkingNow();

        Map<String, ParkingRecord> bySpot = new HashMap<>();
        for (ParkingRecord record : actives) {
            bySpot.putIfAbsent(record.getSpotcode(), record);
        }

        LocalDateTime now = LocalDateTime.now();
        List<SpotView> list = new ArrayList<>();
        for (ParkingSpot spot : spots) {
            ParkingRecord record = bySpot.get(spot.getSpotcode());
            SpotView view = new SpotView();
            view.setSpotcode(spot.getSpotcode());
            view.setSpottype(spot.getSpottype());
            view.setStatus(record != null ? STATUS_USED : spot.getStatus());
            view.setRemark(spot.getRemark());
            if (record != null) {
                view.setPlatenumber(record.getPlatenumber());
                view.setEntrytime(record.getEntrytime() == null ? null : record.getEntrytime().format(TS));
                view.setMinutes(record.getEntrytime() == null ? 0
                        : ParkingFeeUtil.calculateMinutes(record.getEntrytime(), now));
            }
            list.add(view);
        }
        list.sort(Comparator.comparing(SpotView::getSpotcode));
        return list;
    }

    /* ================= 增 / 改 / 删 ================= */

    @RequestMapping("add")
    public ApiResult add(ParkingSpot spot) {
        String code = trim(spot.getSpotcode());
        if (code.isEmpty()) {
            return ApiResult.fail("车位编号不能为空");
        }
        if (code.length() > 10) {
            return ApiResult.fail("车位编号最多 10 个字符");
        }
        if (parkingSpotMapper.selectBySpotcode(code) != null) {
            return ApiResult.fail("车位编号 " + code + " 已存在");
        }

        spot.setSpotcode(code);
        spot.setSpottype(normalizeType(spot.getSpottype()));
        String status = trim(spot.getStatus());
        // 新增时也允许直接建成「已占用」（预留位/线下已停）
        if (!STATUS_USED.equals(status) && !STATUS_REPAIR.equals(status)) {
            status = STATUS_FREE;
        }
        spot.setStatus(status);
        spot.setRemark(blankToDash(spot.getRemark()));

        int rows = parkingSpotMapper.insertSpot(spot);
        parkingSpotService.clearCache();
        return rows > 0 ? ApiResult.ok("车位 " + code + " 已添加") : ApiResult.fail("添加失败，请重试");
    }

    @RequestMapping("update")
    public ApiResult update(ParkingSpot spot) {
        String code = trim(spot.getSpotcode());
        if (code.isEmpty()) {
            return ApiResult.fail("缺少车位编号");
        }
        ParkingSpot exist = parkingSpotMapper.selectBySpotcode(code);
        if (exist == null) {
            return ApiResult.fail("车位 " + code + " 不存在");
        }

        String status = trim(spot.getStatus());
        if (!STATUS_FREE.equals(status) && !STATUS_REPAIR.equals(status) && !STATUS_USED.equals(status)) {
            return ApiResult.fail("状态只能是「空闲」「已占用」或「维修中」");
        }

        // 有真实在场车辆时，只允许保持「已占用」，不能手动改成空闲/维修
        if (parkingSpotMapper.countParkingNowBySpotcode(code) > 0 && !STATUS_USED.equals(status)) {
            return ApiResult.fail("车位 " + code + " 上还有车辆在场，请先办理出场再修改状态");
        }

        spot.setSpotcode(code);
        spot.setSpottype(normalizeType(spot.getSpottype()));
        spot.setStatus(status);
        spot.setRemark(blankToDash(spot.getRemark()));

        int rows = parkingSpotMapper.updateSpot(spot);
        parkingSpotService.clearCache();
        return rows > 0 ? ApiResult.ok("车位 " + code + " 已更新") : ApiResult.fail("更新失败，请重试");
    }

    @RequestMapping("delete")
    public ApiResult delete(String spotcode) {
        String code = trim(spotcode);
        if (code.isEmpty()) {
            return ApiResult.fail("缺少车位编号");
        }
        if (parkingSpotMapper.selectBySpotcode(code) == null) {
            return ApiResult.fail("车位 " + code + " 不存在");
        }
        if (parkingSpotMapper.countParkingNowBySpotcode(code) > 0) {
            return ApiResult.fail("车位 " + code + " 上还有车辆在场，不能删除");
        }

        int rows = parkingSpotMapper.deleteSpot(code);
        parkingSpotService.clearCache();
        return rows > 0 ? ApiResult.ok("车位 " + code + " 已删除") : ApiResult.fail("删除失败，请重试");
    }

    /* ---------------- 小工具 ---------------- */

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String normalizeType(String type) {
        return "会员".equals(trim(type)) ? "会员" : "普通";
    }

    private String blankToDash(String remark) {
        String r = trim(remark);
        return r.isEmpty() ? "-" : r;
    }
}
