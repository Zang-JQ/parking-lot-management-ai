package com.jr.service;

import com.jr.entity.ParkingRecord;

import java.util.List;

public interface ParkingRecordService {
    Boolean addParkingRecord(ParkingRecord parkingRecord);

    ParkingRecord findParkingRecordByUserid(Integer id);

    Boolean updateParkingRecord(ParkingRecord parkingRecord);

    List<ParkingRecord> findByUserid(Integer userid);

    int findCountByUserid(Integer userid);

    Double findSumFeeByUserid(Integer userid);

    String findplatenumberByUserid(Integer userid);

    Boolean updateisfreetomember(Integer userid);

    Boolean addmemberParkingRecord(ParkingRecord parkingRecord);

    ParkingRecord findParkingRecordByPlatenumber(String platenumber);

    Boolean updatememberParkingRecord(ParkingRecord parkingRecord);

}
