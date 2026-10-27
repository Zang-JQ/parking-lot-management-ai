package com.jr.service;

import com.jr.entity.ParkingSpot;

import java.util.List;

public interface ParkingSpotService {
    List<ParkingSpot> findAll();

    List<ParkingSpot> findfreeSpot();

    List<ParkingSpot> findAllParkingSpot();

    /** 清掉 Redis 里的车位缓存，增删改之后必须调 */
    void clearCache();
}
