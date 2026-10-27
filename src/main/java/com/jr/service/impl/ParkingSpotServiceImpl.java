package com.jr.service.impl;

import com.jr.entity.ParkingSpot;
import com.jr.mapper.ParkingSpotMapper;
import com.jr.service.ParkingSpotService;
import com.jr.util.RedisGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 车位列表查询：Redis 缓存 + 熔断降级。
 *
 * 三级策略：
 *   1. Redis 正常   → 命中直接返回；未命中查库再写回（60 秒过期）
 *   2. Redis 挂了   → 熔断开关打开，30 秒内完全跳过 Redis，用本地缓存顶 5 秒
 *   3. 本地也过期   → 查库
 *
 * 这样即使 Redis 长时间不可用，也不会每个请求都卡满超时，数据库压力也可控。
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRED)
public class ParkingSpotServiceImpl implements ParkingSpotService {

    private static final String spotKey = "spotlist";

    /** Redis 挂掉时，本地缓存顶多久（毫秒） */
    private static final long LOCAL_TTL_MS = 5000L;

    @Autowired
    private ParkingSpotMapper parkingSpotMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisGuard redisGuard;

    /** 本地兜底缓存：只在 Redis 不可用时使用 */
    private volatile List<ParkingSpot> localCache;
    private volatile long localCacheExpireAt = 0L;

    @Override
    @SuppressWarnings("unchecked")
    public List<ParkingSpot> findAll() {
        // ---------- 1. Redis 熔断打开：直接用本地缓存 / 查库 ----------
        if (!redisGuard.allow()) {
            return fromLocalOrDb();
        }

        // ---------- 2. 恢复后先还欠账 ----------
        // 熔断期间发生过入场/出场或车位修改，Redis 里的旧值没被删掉，
        // 这里趁 Redis 已恢复，主动删一次，保证读到的不是断电前的快照。
        if (redisGuard.isDirty()) {
            try {
                redisTemplate.delete(spotKey);
                redisGuard.clearDirty();
                redisGuard.success();
                log.info("[车位缓存] Redis 已恢复，旧缓存已清除，本次直接回源");
            } catch (Exception e) {
                redisGuard.failure(e);
                return fromLocalOrDb();
            }
        }

        // ---------- 3. 试 Redis ----------
        try {
            Object cached = redisTemplate.opsForValue().get(spotKey);
            redisGuard.success();
            if (cached != null) {
                return (List<ParkingSpot>) cached;
            }
        } catch (Exception e) {
            redisGuard.failure(e);
            return fromLocalOrDb();
        }

        // ---------- 4. 缓存没命中，查库 ----------
        List<ParkingSpot> list = parkingSpotMapper.selectAll();

        // ---------- 5. 写回 Redis（失败就熔断，不影响返回） ----------
        if (list != null) {
            try {
                redisTemplate.opsForValue().set(spotKey, list, 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                redisGuard.failure(e);
            }
        }
        return list;
    }

    /** Redis 不可用时的降级路径：本地缓存没过期就用，否则查库并回填本地 */
    private List<ParkingSpot> fromLocalOrDb() {
        long now = System.currentTimeMillis();
        List<ParkingSpot> local = localCache;
        if (local != null && now < localCacheExpireAt) {
            return local;
        }
        List<ParkingSpot> list = parkingSpotMapper.selectAll();
        localCache = list;
        localCacheExpireAt = now + LOCAL_TTL_MS;
        return list;
    }

    @Override
    public List<ParkingSpot> findfreeSpot() {
        return parkingSpotMapper.selectfreeSpot();
    }

    @Override
    public List<ParkingSpot> findAllParkingSpot() {
        return parkingSpotMapper.selectAllParkingSpot();
    }

    @Override
    public void clearCache() {
        // 本地缓存先失效（这一句不依赖 Redis，必须先执行）
        localCache = null;
        localCacheExpireAt = 0L;

        if (!redisGuard.allow()) {
            // Redis 不可用，删不了 → 记一笔欠账，等恢复后第一次访问时补删
            redisGuard.markDirty();
            return;
        }
        try {
            redisTemplate.delete(spotKey);
            redisGuard.success();
        } catch (Exception e) {
            redisGuard.failure(e);
            redisGuard.markDirty();
        }
    }
}
