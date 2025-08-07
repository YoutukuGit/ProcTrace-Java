# 更新日志

## [2.1] - 2025-08-05 
### 新增功能
- **动态进程分类**  
  - 新增 `Category` 列至 `TargetProcesses` 表，支持自定义类别（如：`"game"`, `"work"`, `"tool"`）
  - 智能关联机制：`ProcessRecords` 表自动获取对应类别  
    ```sql
    INSERT INTO ProcessRecords(Category) 
    VALUES ((SELECT Category FROM TargetProcesses WHERE ProcessName=?))
    ```
  
- **进程路径溯源**  
  - `ProcessRecords` 表新增 `Path` 列，完整记录进程位置  
  - 存储格式示例：  
    `C:\Program Files\Steam\steam.exe` (Windows)  
    `/Applications/Google Chrome.app` (macOS)

### 数据优化
- **名称/路径分离存储**  
  - `ProcessName` 列 → 仅存基础名称（`explorer.exe`）  
  - `Path` 列 → 存储完整路径（`C:\Windows\explorer.exe`）

## [2.0] - 2025-07-22
### 架构重构
- **存储引擎迁移**：JSON → SQLite 数据库
- **新数据库位置**：`~/Documents/ProcessTrace/DataBase.db`
- **配置管理**：配置文件迁移到数据库的 `TargetProcesses` 表

### 新增功能
- 数据库自动初始化：首次运行时自动创建表结构
- 默认配置：自动添加 `steam.exe` 到监控列表
- 跨平台支持：适配 Windows/Linux/macOS 路径格式

### 安全增强
- 使用预编译语句防御 SQL 注入攻击
- 数据库连接全链路 try-with-resources 管理

### 破坏性变更
- ⚠️ **移除 JSON 依赖**：不再需要 Gson 库
- ⚠️ **配置格式变更**：原 `config.json` 已废弃
- ⚠️ **数据不可自动迁移**：旧版 JSON 数据需手动导入

## [1.0] - 2025-06-29
- 首次发布