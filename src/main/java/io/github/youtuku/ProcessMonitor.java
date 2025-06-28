package io.github.youtuku;

import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

/**
 * 进程监控核心类,负责:
 * 1.根据配置文件动态监控目标进程的生命周期
 * 2.记录进程启动/退出时间
 * 3.定期扫描新进程
 * 4.进程退出时自动保存运行时数据
 * 
 * @author youtuku
 */
public class ProcessMonitor {
    private final AppConfig config;
    private final TimeCalculator timer;
    private final Map<Long, ProcessHandle> monitoredPids = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    /**
     * 创建进程监控器实例
     * @param config 应用程序配置对象,提供文件路径和存储方法
     * @param timer 时间计算器,用于记录进程时间数据
     */
    public ProcessMonitor(AppConfig config, TimeCalculator timer){
        this.config = config;
        this.timer = timer;
    }
    
    /**
     * 从配置文件中读取目标进程列表
     * @return 包含所有目标进程名的列表(已去除空格)
     * @throws IOException 当配置文件不存在、格式错误或读取失败时抛出
     */
    private List<String> getTargetProcesses() throws IOException{
        try (JsonReader reader = new JsonReader(new FileReader(config.configFile))) {
            JsonObject jsonObject = new Gson().fromJson(reader, JsonObject.class);
            JsonElement element = jsonObject.get(AppConfig.CONFIG_KEY_DEFAULT);
            
            if (element == null || !element.isJsonPrimitive()) {
                throw new IOException("配置文件格式错误: 缺少 '" + AppConfig.CONFIG_KEY_DEFAULT + "' 字段");
            }
            
            String procString = element.getAsString();
            return Arrays.stream(procString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
    }

    /**
     * 判断进程是否为监控目标
     * @param ph 待检测的进程句柄
     * @param targets 目标进程名列表
     * @return 当进程命令包含任意目标进程名时返回true，否则返回false
     * @implNote 匹配规则:
     *           1.不区分大小写
     *           2.部分匹配(如："explorer.exe" 匹配 "C:\Windows\explorer.exe")
     *           3.空命令进程始终返回false
     */
    private boolean isTargetProcess(ProcessHandle ph, List<String> targets){
        return ph.info().command()
            .map(cmd -> targets.stream().anyMatch(cmd::contains))
            .orElse(false);
    }

    /**
     * 扫描系统所有进程,匹配配置文件中的目标进程
     * @return 包含所有匹配进程的映射表(Key: PID, Value: 进程句柄)
     * @throws IOException 当配置文件读取失败或格式错误时抛出
     */
    private Map<Long, ProcessHandle> findMatchingProcess() throws IOException{
        List<String> targets = getTargetProcesses();
        return ProcessHandle.allProcesses()
            .filter(ph -> isTargetProcess(ph, targets))
            .collect(Collectors.toMap(
                ProcessHandle::pid, 
                Process -> Process));
    }

    /**
     * 开始监控进程。首先扫描一次当前匹配的进程,然后每隔10秒扫描一次新进程
     * @throws IOException 当初始进程扫描失败时抛出
     */
    public void startMonitoring() throws IOException{
        try {
            findMatchingProcess().entrySet().stream()
                .map(Map.Entry::getValue)
                .forEach(this::monitorProcess);
        } catch (IOException e) {
            System.err.println("初始进程扫描失败: " + e.getMessage());
        }
        
        SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                scanNewProcess();
            } catch (IOException e) {
                System.err.println("[" + Instant.now() + "] 进程扫描异常: " + e.getMessage());
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * 停止监控进程。保存所有未完成的进程数据,并关闭调度器
     */
    void stopMonitoring(){
        for (Map.Entry<Long, ProcessHandle> entry : monitoredPids.entrySet()) {
            Long pid = entry.getKey();
            try {
                DataStore data = timer.getOngoingEvent(pid);
                if (data != null) {
                    config.appendtoAppData(data);
                    System.out.println("保存未完成进程: " + data.getProcName() + " (PID: " + pid + ")");
                }
            } catch (IOException e) {
                System.err.println("保存未完成进程数据失败 (PID:" + pid + "): " + e.getMessage());
            }
        }
        
        SCHEDULER.shutdown();
        try {
            if (!SCHEDULER.awaitTermination(3, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 扫描新进程,过滤掉已在监控列表中的进程,将新发现的匹配进程加入监控
     * @throws IOException 若读取配置文件失败
     */
    private void scanNewProcess() throws IOException{
        findMatchingProcess().entrySet().stream()
            .filter(entry -> !monitoredPids.containsKey(entry.getKey()))
            .map(Map.Entry::getValue)
            .forEach(this::monitorProcess);
    }

    /**
     * 监控指定进程。当进程退出时,记录其运行时间并保存数据及清理资源
     * @param ph 要监控的进程句柄
     */
    private void monitorProcess(ProcessHandle ph) {
        String processName = ph.info().command().orElse("Unknown");
        
        monitoredPids.put(ph.pid(), ph);
        timer.setStartEvents(ph.pid(), processName);

        ph.onExit().thenRun(() -> {
            try {
                System.out.println("进程终止:" + timer.ongoingEvents.get(ph.pid()) + "(PID: " + ph.pid() + ")");
                monitoredPids.remove(ph.pid());
                DataStore data = timer.setCompletedEvents(ph.pid());
                config.appendtoAppData(data);
            } catch (IOException e) {
                System.err.println("进程终止处理失败 (PID:" + ph.pid() + "): " + e.getMessage());
            } finally{
                timer.ongoingEvents.remove(ph.pid());
                timer.startEvents.remove(ph.pid());
            }
        });
    }
}
