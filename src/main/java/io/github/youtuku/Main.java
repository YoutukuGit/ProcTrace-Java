package io.github.youtuku;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主类,包含程序入口和主流程
 * 
 * @author youtuku
 */
public class Main {
    public static void main(String[] args) throws SQLException {
        System.out.println("==== 进程监控计时系统启动 ====");
        
        try {
            // 1. 初始化配置和数据库
            AppConfig config = new AppConfig();
            System.out.println("数据库初始化完成: " + AppConfig.DB_PATH);
            
            // 打印配置信息
            Map<String, String> targets = config.getTargetProcesses();
            List<String> targetList = targets.keySet().stream()
                                    .collect(Collectors.toList());
            System.out.println("监控目标进程: " + String.join(", ", targetList));
            
            // 2. 初始化核心组件
            TimeCalculator timer = new TimeCalculator();
            ProcessMonitor monitor = new ProcessMonitor(config, timer);
            
            // 3. 添加关闭钩子 (确保资源释放)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n==== 收到关闭信号，停止监控 ====");
                try {
                    monitor.stopMonitoring();
                    System.out.println("监控已停止，数据保存完成");
                } catch (Exception e) {
                    System.err.println("关闭过程中发生错误: " + e.getMessage());
                }
            }));
            
            // 4. 启动监控
            System.out.println("\n==== 开始监控目标进程 ====");
            monitor.startMonitoring();
            System.out.println("监控器已启动，每10秒扫描一次新进程");
            System.out.println("按 Ctrl+C 退出程序...");
            
            // 5. 保持主线程运行
            Thread.currentThread().join();
            
        } catch (SQLException e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("主线程被中断，程序退出");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("未处理的异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}