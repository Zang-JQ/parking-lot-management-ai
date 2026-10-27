package com.jr.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis 熔断器。
 *
 * 解决的问题：Redis 挂掉时，如果每个请求都去尝试连接，
 * 每次都要等满命令超时（Lettuce 默认 60 秒），页面会直接卡死。
 *
 * 做法：一旦失败就"打开开关"，接下来 N 秒内所有缓存操作直接跳过、走数据库；
 * N 秒后放一个请求去探测，成功就自动关闭开关。
 * 这样把"每个请求都卡"变成"每 N 秒只卡一个请求"。
 */
@Slf4j
@Component
public class RedisGuard {

    /** 熔断打开后多久再试一次（秒） */
    @Value("${app.redis.fail-open-seconds:30}")
    private int failOpenSeconds;

    /** 在这个时间点之前，所有 Redis 操作都直接跳过 */
    private volatile long nextAttemptAt = 0L;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    /** 保证"不可用"日志只打一次，避免刷屏 */
    private final AtomicBoolean logged = new AtomicBoolean(false);

    /**
     * 熔断期间是否发生过写操作（比如入场/出场改了车位状态）。
     * 一旦为 true，Redis 恢复后必须先把缓存删掉，否则会读到断电前的旧值。
     */
    private final AtomicBoolean dirtyWhileDown = new AtomicBoolean(false);

    /** 当前是否允许访问 Redis */
    public boolean allow() {
        return System.currentTimeMillis() >= nextAttemptAt;
    }

    /** 熔断期间有写操作：记一笔欠账，恢复后要先清缓存 */
    public void markDirty() {
        if (dirtyWhileDown.compareAndSet(false, true)) {
            log.info("[Redis] 熔断期间发生了数据变更，恢复后将强制刷新缓存");
        }
    }

    /** 是否有欠账没还（恢复后需要先失效缓存） */
    public boolean isDirty() {
        return dirtyWhileDown.get();
    }

    /** 欠账已还清 */
    public void clearDirty() {
        dirtyWhileDown.set(false);
    }

    /** 调用成功：清空失败计数，恢复缓存 */
    public void success() {
        if (consecutiveFailures.getAndSet(0) > 0) {
            nextAttemptAt = 0L;
            if (logged.getAndSet(false)) {
                log.info("[Redis] 已恢复，缓存重新启用");
            }
        }
    }

    /** 调用失败：打开熔断，N 秒内不再尝试 */
    public void failure(Throwable e) {
        consecutiveFailures.incrementAndGet();
        nextAttemptAt = System.currentTimeMillis() + Math.max(1, failOpenSeconds) * 1000L;
        if (logged.compareAndSet(false, true)) {
            log.warn("[Redis] 不可用，接下来 {} 秒内跳过缓存直接查库：{}", failOpenSeconds, e.getMessage());
        }
    }

    /** 熔断是否正处于打开状态（给页面/监控看） */
    public boolean isOpen() {
        return !allow();
    }
}
