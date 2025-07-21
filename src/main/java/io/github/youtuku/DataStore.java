package io.github.youtuku;

import java.time.Duration;
import java.time.Instant;

/**
 * 数据存储类,用于表示一个进程的时间跟踪数据
 * 
 * @author youtuku
 */
public class DataStore {
    private final String processName;
    private final Instant startTime;
    private final Duration duration;

    /**
     * 创建一个数据存储对象
     * @param processName 进程名称
     * @param startTime 进程开始时间
     * @param duration 进程运行持续时间
     */
    DataStore(String processName, Instant startTime, Duration duration){
        this.processName = processName;
        this.startTime = startTime;
        this.duration = duration;
    }

    String getProcessName(){
        return processName;
    }

    Instant getStartTime(){
        return startTime;
    }

    Duration getDuration(){
        return duration;
    }
}
