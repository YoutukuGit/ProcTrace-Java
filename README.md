# 进程监控计时系统 ProcTrace

## 简介
这是一个用于监控目标进程运行时间的Java应用程序。  
它会定期扫描系统中运行的进程，记录配置文件中指定进程的运行时间数据（包括启动时间和持续时间），并将这些数据保存 SQLite 数据库中。

## 主要功能
1. **进程监控**  
    每10秒扫描匹配进程（数据库中的目标进程配置）
2. **时间追踪**  
    进程启动时记录时间戳  
    进程退出时计算持续时间  
    程序退出时自动保存未完成进程数据
3. **配置管理**  
    自动创建用户目录 (`~/Documents/ProcessTrace`)  
    创建 SQLite 数据库 (`DataBase.db`) 并初始化表结构  
    自动插入默认监控进程 (`steam.exe`)
4. **数据持久化**  
    SQLite 数据库存储进程运行记录  
    支持高效查询和统计分析
5. **跨平台支持**  
    兼容 `Windows/Linux/macOS` 系统  
    自动处理不同操作系统的进程路径格式
6. **安全可靠**  
    使用预编译语句防止 SQL 注入  
    完善的异常处理和错误日志

## 使用说明
1. **运行程序**
   - **环境要求**：最低支持 JDK 11，SQLite JDBC 驱动
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
    程序会在首次运行时创建数据库文件：  
    `~/Documents/ProcessTrace/DataBase.db`  

3. **数据库结构**
   ```sql
   -- 进程记录表
   CREATE TABLE ProcessRecords (
     id INTEGER PRIMARY KEY AUTOINCREMENT,
     ProcessName TEXT NOT NULL,
     StartTime TIMESTAMP NOT NULL,
     DurationSeconds INTEGER NOT NULL
   );
   
   -- 目标进程配置表
   CREATE TABLE TargetProcesses (
     id INTEGER PRIMARY KEY AUTOINCREMENT,
     ProcessName TEXT NOT NULL UNIQUE
   );

4. **配置说明**  

    -**默认监控进程**：`steam.exe`  
    -**修改监控进程**：
    ```sql  
    --添加
    INSERT INTO TargetProcesses(ProcessName) VALUES ('explorer.exe');  
    --删除
    DELETE FROM TargetProcesses WHERE ProcessName = 'steam.exe';

5. **数据查询示例**
    ```sql  
    -- 获取所有进程记录
    SELECT * FROM ProcessRecords;

    -- 统计各进程总运行时间
    SELECT ProcessName, SUM(DurationSeconds) AS TotalSeconds 
    FROM ProcessRecords 
    GROUP BY ProcessName;

    -- 查询今日记录
    SELECT * FROM ProcessRecords 
    WHERE DATE(StartTime) = DATE('now');
    ```
## 注意事项
1. 程序需要读取系统进程列表的权限
2. 数据库修改后需要重启程序生效
3. 按 Ctrl+C 可安全退出程序 (未完成进程会自动保存)
4. 监控间隔固定为10秒 (可在 `ProcessMonitor.java` 中调整)
5. 跨平台支持：
    - **Windows**：自动处理 `C:\` 路径格式
    - **Linux/macOS**：自动处理 `/usr/bin/` 路径格式


## 改进方向
1. **轮询间隔调整**  
    允许通过配置文件自定义扫描间隔 (当前硬编码为10秒)
   
2. **进程资源监控**  
    增加 CPU/内存使用率记录（需扩展数据库表结构）

3. **配置增强**  
    增加进程匹配模式选项 (全匹配/正则表达式/模糊匹配)  
    支持监控进程树 (包含子进程)

## 更新日志  
详细变更内容请参阅 [CHANGELOG.md](CHANGELOG.md).

