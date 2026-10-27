package com.jr.cache;

import com.jr.entity.ParkingSpot;
import com.jr.mapper.ParkingSpotMapper;
import com.jr.service.impl.ParkingSpotServiceImpl;
import com.jr.util.RedisGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 车位缓存的降级与恢复行为测试（不需要 Redis、不需要数据库，全部用 mock）。
 *
 * 覆盖四个场景：
 *   1. Redis 正常 → 命中缓存，不查库
 *   2. Redis 异常 → 降级查库，并打开熔断
 *   3. 熔断打开后 → 后续请求不再尝试 Redis（这是"不卡"的关键）
 *   4. 熔断期间改过数据 → 恢复后先删旧缓存，再回源（解决缓存不同步）
 */
class ParkingCacheDegradationTest {

    private static final String KEY = "spotlist";

    private ParkingSpotMapper mapper;
    private RedisTemplate<String, Object> redis;
    private ValueOperations<String, Object> valueOps;
    private RedisGuard guard;
    private ParkingSpotServiceImpl service;

    private final List<ParkingSpot> fromDb = List.of(
            new ParkingSpot(1, "A001", "普通", "空闲", "-"),
            new ParkingSpot(2, "A002", "普通", "已占用", "-"));

    @BeforeEach
    void setUp() {
        mapper = mock(ParkingSpotMapper.class);
        redis = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        guard = new RedisGuard();
        ReflectionTestUtils.setField(guard, "failOpenSeconds", 30);

        service = new ParkingSpotServiceImpl();
        ReflectionTestUtils.setField(service, "parkingSpotMapper", mapper);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "redisGuard", guard);
    }

    @Test
    @DisplayName("Redis 正常：命中缓存，不查数据库")
    void cacheHit() {
        when(valueOps.get(KEY)).thenReturn(fromDb);

        List<ParkingSpot> result = service.findAll();

        assertEquals(2, result.size());
        verify(mapper, never()).selectAll();
        assertFalse(guard.isOpen());
    }

    @Test
    @DisplayName("Redis 异常：降级查库，并打开熔断")
    void degradeWhenRedisDown() {
        when(valueOps.get(KEY)).thenThrow(new RedisConnectionFailureException("connection refused"));
        when(mapper.selectAll()).thenReturn(fromDb);

        List<ParkingSpot> result = service.findAll();

        assertEquals(2, result.size(), "降级后仍要返回正确数据");
        verify(mapper, times(1)).selectAll();
        assertTrue(guard.isOpen(), "失败后熔断应打开");
    }

    @Test
    @DisplayName("熔断打开后：不再尝试 Redis，只查库/本地缓存")
    void skipRedisWhileOpen() {
        when(valueOps.get(KEY)).thenThrow(new RedisConnectionFailureException("down"));
        when(mapper.selectAll()).thenReturn(fromDb);

        service.findAll();   // 第一次：试 Redis 失败 → 查库 → 熔断
        service.findAll();   // 第二次：应完全跳过 Redis

        // Redis 只被试过一次，第二次没再碰
        verify(valueOps, times(1)).get(anyString());
        // 第二次走本地兜底缓存，没有重复查库
        verify(mapper, times(1)).selectAll();
    }

    @Test
    @DisplayName("熔断期间改过数据：Redis 恢复后先清旧缓存，再从数据库回源")
    void invalidateAfterRecovery() {
        when(mapper.selectAll()).thenReturn(fromDb);
        when(valueOps.get(KEY)).thenReturn(null);   // 恢复后缓存是空的

        // 1) 模拟 Redis 故障，熔断打开
        guard.failure(new RedisConnectionFailureException("down"));
        assertTrue(guard.isOpen());

        // 2) 故障期间发生了写操作（入场/出场会清缓存）→ 记一笔欠账
        service.clearCache();
        assertTrue(guard.isDirty(), "删不掉缓存时必须记账");

        // 3) 模拟 30 秒后 Redis 恢复（熔断窗口过去）
        ReflectionTestUtils.setField(guard, "nextAttemptAt", 0L);
        assertFalse(guard.isOpen());

        // 4) 恢复后的第一次查询：必须先删旧缓存
        service.findAll();

        verify(redis, times(1)).delete(KEY);
        assertFalse(guard.isDirty(), "欠账应已还清");
    }
}
