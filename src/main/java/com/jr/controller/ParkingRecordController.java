package com.jr.controller;

import com.jr.entity.ParkingRecord;
import com.jr.mapper.ParkingRecordMapper;
import com.jr.service.ParkingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("ParkingRecord")
public class ParkingRecordController{
    @Autowired
    private ParkingRecordService parkingRecordService;
    @Autowired
    private ParkingRecordMapper parkingRecordMapper;

    /** 管理员：全场停车记录（停车记录查询页用） */
    @RequestMapping("all")
    public List<ParkingRecord> all() {
        return parkingRecordMapper.selectAll();
    }

    @RequestMapping("addParkingRecord")
    public Boolean addParkingRecord(ParkingRecord parkingRecord){
        return parkingRecordService.addParkingRecord(parkingRecord);
    }

    @RequestMapping("findParkingRecordById")
    public ParkingRecord findParkingRecordById(Integer userid){
        return parkingRecordService.findParkingRecordByUserid(userid);
    }

    @RequestMapping("updateParkingRecord")
    public Boolean updateParkingRecord(ParkingRecord parkingRecord){
        return parkingRecordService.updateParkingRecord(parkingRecord);
    }

    @RequestMapping("findByUserid")
    public List<ParkingRecord> findByUserid(ParkingRecord parkingRecord){
        return parkingRecordService.findByUserid(parkingRecord.getUserid());
    }

    @RequestMapping("findCountByUserid")
    public int findCountByUserid(ParkingRecord parkingRecord){
        return parkingRecordService.findCountByUserid(parkingRecord.getUserid());
    }

    @RequestMapping("findSumFeeByUserid")
    public Double findSumFeeByUserid(ParkingRecord parkingRecord){
        return parkingRecordService.findSumFeeByUserid(parkingRecord.getUserid());
    }

    @RequestMapping("findplatenumberByUserid")
    public String findplatenumberByUserid(ParkingRecord parkingRecord){
        return parkingRecordService.findplatenumberByUserid(parkingRecord.getUserid());
    }

    @RequestMapping("updateisfreetomember")
    public Boolean updateisfreetomember(Integer userid){
        return parkingRecordService.updateisfreetomember(userid);
    }

    @RequestMapping("addmemberParkingRecord")
    public Boolean addmemberParkingRecord(ParkingRecord parkingRecord){
        return parkingRecordService.addmemberParkingRecord(parkingRecord);
    }

    @RequestMapping("findParkingRecordByPlatenumber")
    public ParkingRecord findParkingRecordByPlatenumber(ParkingRecord parkingRecord){
        return parkingRecordService.findParkingRecordByPlatenumber(parkingRecord.getPlatenumber());
    }

    @RequestMapping("updatememberParkingRecord")
    public Boolean updatememberParkingRecord(ParkingRecord parkingRecord){
        return parkingRecordService.updatememberParkingRecord(parkingRecord);
    }


}
