package com.jr.aaa;

import com.jr.entity.ParkingSpot;
import com.jr.mapper.ParkingSpotMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AaaApplicationTests {
    @Autowired
    private ParkingSpotMapper  parkingSpotMapper;

    @Test
    void contextLoads() {
        List<ParkingSpot> parkingSpots = parkingSpotMapper.selectAll();
        for (ParkingSpot parkingSpot : parkingSpots) {
            System.out.println(parkingSpot);
        }
    }

}
