# 进程监控计时系统 ProcTrace

## 简介
这是一个用于监控目标进程运行时间的Java应用程序。  
它会定期扫描系统中运行的进程，记录配置文件中指定进程的运行时间数据（包括启动时间和持续时间），并将这些数据保存到JSON格式的文件中。

## 主要功能
1. **进程监控**  
    每10秒扫描匹配进程 (配置文件中的TargetProcesses)
2. **时间追踪**  
    进程启动时记录时间戳  
    进程退出时计算持续时间
3. **配置管理**  
    自动创建用户目录 (~/Documents/ProcessTracer)  
    生成默认配置文件 (config.json)和数据文件 (appData.json)
4. **数据持久化**  
    JSON格式存储进程运行记录
    程序退出时自动保存未完成进程数据
5. **异常处理**  
    关键操作捕获IOException并输出错误日志

## 使用说明
1. **运行程序**
   - **环境要求**：最低支持 JDK 11
   - **编译项目** (在包含pom.xml的项目根目录下执行)：
     ```bash
     mvn compile
     ```
   - **运行程序** (在包含pom.xml的项目根目录下执行)：
     ```bash
     mvn exec:java -Dexec.mainClass="io.github.youtuku.Main"
     ```
   - 或在 IDE 中直接执行 `Main.java`

2. **配置文件**  
    程序会在首次运行时创建默认配置文件 (Documents/ProcessTracer/config.json)  
    默认配置内容如下：
    ```json
    {
        "TargetProcesses": "steam.exe, explorer.exe"
    }

3. **配置说明**
    - TargetProcesses：要监控的进程名列表 (逗号分隔)
    - 支持部分匹配 (如"explorer.exe"可匹配"C:\Windows\explorer.exe")
    - 不区分大小写

4. **数据存储**  
    进程运行数据存储在 Documents/ProcessTracer/appData.json 文件中，格式如下：
    ```json
    [
        {
            "ProcessName": "explorer.exe",
            "StartTime": "2025-06-29T10:15:30.123Z",
            "Duration": "PT5M30S"
        },
        {
            "ProcessName": "steam.exe",
            "StartTime": "2025-06-29T11:20:15.456Z",
            "Duration": "PT2H15M"
        }
    ]

## 注意事项
1. 程序需要读取系统进程列表的权限
2. 配置文件修改后需要重启程序生效
3. 按 Ctrl+C 可安全退出程序 (未完成进程会自动保存)
4. 监控间隔固定为10秒 (代码中 ProcessMonitor.SCHEDULER 可调整)
5. 仅支持 Windows 系统 (依赖 Windows 进程命令路径格式)

## Maven依赖
    Google Gson库 (用于JSON处理)

## 更新方向
1. **轮询间隔调整**  
    允许通过配置文件自定义扫描间隔 (当前硬编码为10秒)
   
2. **跨平台支持**  
    适配 Linux/macOS 的进程路径格式(如 `/usr/bin/` 替代 `C:\`)  
    使用 `ps` 命令替代 Windows 专属 API
   
3. **存储优化**  
    增加数据压缩功能 (减少长期运行的 JSON 文件体积)  
    支持 SQLite 数据库存储替代 JSON 文件
   
4. **配置增强**  
    增加进程匹配模式选项 (全匹配/正则表达式/模糊匹配)  
    支持监控进程树 (包含子进程)

