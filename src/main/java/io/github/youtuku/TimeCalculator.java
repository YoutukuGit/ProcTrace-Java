package io.github.youtuku;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时间计算类,用于记录进程的开始时间和结束时间,并计算持续时间
 * 
 * @author youtuku
 */
public class TimeCalculator {
    final Map<Long, Instant> startEvents = new ConcurrentHashMap<>();
    final Map<Long, Duration> completedEvents = new ConcurrentHashMap<>();
    final Map<Long, String> ongoingEvents = new ConcurrentHashMap<>();

    /**
     * 记录进程的开始事件
     * @param key 进程的PID
     * @param name 进程名称
     */
    void setStartEvents(Long key, String name){
        startEvents.put(key, Instant.now());
        ongoingEvents.put(key, name);
    }

    /**
     * 记录进程的结束事件,并计算持续时间
     * @param key 进程的PID
     * @return 包含进程名称、开始时间和持续时间的数据存储对象
     */
    DataStore setCompletedEvents(Long key) {
        Instant startTime = startEvents.get(key);
        Duration duration = Duration.between(startTime, Instant.now());
        completedEvents.put(key, duration);
        
        String name = ongoingEvents.get(key);
        return new DataStore(name, startTime, duration);
    }

    /**
     * 获取正在进行中的进程的事件数据
     * @param key 进程的PID
     * @return 包含进程名称、开始时间和当前持续时间的数据存储对象;如果进程不存在则返回null
     */
    DataStore getOngoingEvent(Long key) {
        Instant startTime = startEvents.get(key);
        if (startTime == null) return null;
        
        Duration duration = Duration.between(startTime, Instant.now());
        String name = ongoingEvents.get(key);
        return new DataStore(name, startTime, duration);
    }
}
