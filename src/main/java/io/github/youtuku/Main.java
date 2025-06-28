package io.github.youtuku;

import java.io.File;
import java.io.IOException;

/**
 * 主类,包含程序入口和主流程
 * 
 * @author youtuku
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("==== 进程监控计时系统启动 ====");
        
        //1.初始化配置
        AppConfig config = new AppConfig();
        try {
            
            // 创建默认配置文件（如果不存在）
            if (!new File(config.configFile).exists()) {
                config.createDefaultConfig();
                System.out.println("已创建默认配置文件:" + AppConfig.CONFIG_FILE);
            }
            
            //创建默认数据文件（如果不存在）
            if (!new File(config.appDataFile).exists()) {
                config.createDefaultAppData();
                System.out.println("已创建默认数据文件:" + AppConfig.APPDATA_FILE);
            }
            
            //打印配置信息
            System.out.println("监控目录:" + config.directoryPath);
            System.out.println("配置文件:" + config.configFile);
            System.out.println("数据文件:" + config.appDataFile);
            
        } catch (IOException e) {
            System.err.println("配置初始化失败:" + e.getMessage());
            return;
        }
        
        //2.初始化组件
        TimeCalculator timer = new TimeCalculator();
        ProcessMonitor monitor = new ProcessMonitor(config, timer);
        
        //3.添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n==== 收到关闭信号，停止监控 ====");
            monitor.stopMonitoring();
            System.out.println("监控已停止");
        }));
        
        //4.启动监控
        try {
            System.out.println("\n==== 开始监控目标进程 ====");
            monitor.startMonitoring();
            System.out.println("监控器已启动,每10秒扫描一次新进程");
            System.out.println("按 Ctrl+C 退出程序...");
            
            //保持主线程运行
            Thread.currentThread().join();
            
        } catch (IOException | InterruptedException e) {
            System.err.println("监控启动失败:" + e.getMessage());
            monitor.stopMonitoring();
        }
}
}