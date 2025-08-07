package io.github.youtuku;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * 应用程序配置管理类，统一处理所有配置和数据文件操作。
 * @author youtuku
 */
public class AppConfig {
    public static final Path DB_PATH = Paths.get(
        System.getProperty("user.home"), "Documents", "ProcessTrace", "DataBase.db"
        );

    public AppConfig() {
        initializeDatabase();
    }

    /**
     * 初始化数据库结构,此方法负责:
     * 1.创建数据库文件(若不存在)
     * 2.创建进程记录表和目标进程表
     * 3.插入默认目标进程(若表为空)
     */
    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS ProcessRecords (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ProcessName TEXT NOT NULL," +
                "Category TEXT," +
                "StartTime TIMESTAMP NOT NULL," +
                "DurationSeconds INTEGER NOT NULL," +
                "Path TEXT)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS TargetProcesses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ProcessName TEXT NOT NULL UNIQUE," +
                "Category TEXT)");
            
            if (isTableEmpty(conn, "TargetProcesses")) {
                stmt.execute("INSERT INTO TargetProcesses(ProcessName, Category) VALUES ('steam.exe', 'tool')");
            }
        } catch (SQLException | IOException e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据库连接
     * @return 有效的数据库连接对象
     * @throws SQLException 若创建数据库连接失败
     * @throws IOException 若创建数据库目录失败
     */
    private Connection getConnection() throws SQLException, IOException{
        Files.createDirectories(DB_PATH.getParent());
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    /**
     * 获取目标进程名称列表
     * @return 包含所有目标进程名称的列表,不会返回null
     * @throws SQLException 数据库连接错误时抛出
     * 示例返回值：["steam.exe"]
     */
    public Map<String, String> getTargetProcesses() throws SQLException{
        Map<String, String> processes = new HashMap<>();
        String sql = "SELECT ProcessName, Category FROM TargetProcesses";

        try(Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()){
                processes.put(rs.getString("ProcessName") , rs.getString("Category"));
            }
        } catch (Exception e) {
            System.err.println("警告：获取目标进程失败，使用默认值");
            return Map.of("steam.exe" , "tool");
        }
        return processes;
    }

    /**
     * 保存进程运行记录至数据库
     * 此方法将进程名称,开始时间和持续时间(秒)保存到数据库
     * @param dataStore 包含进程信息的DataStore对象
     * @throws SQLException 保存失败时抛出
     */
    public void saveProcessRecord(DataStore dataStore) throws SQLException{
        String sql = "INSERT INTO ProcessRecords(ProcessName, StartTime, DurationSeconds, Path, Category)" +
                        "VALUES (?, ?, ?, ?, " +
                        "(SELECT Category FROM TargetProcesses WHERE ProcessName = ?))";
        
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String baseName = new File(dataStore.getProcessName()).getName();

            pstmt.setString(1, baseName);
            pstmt.setString(2, dataStore.getStartTime().toString());
            pstmt.setLong(3, dataStore.getDuration().getSeconds());
            pstmt.setString(4, dataStore.getProcessName());
            pstmt.setString(5, baseName);

            pstmt.executeUpdate();
            
        } catch (SQLException | IOException e) {
            System.err.println("保存进程记录失败: " + e.getMessage());
        }
    }

    /**
     * 检查指定表是否为空
     * @param conn 有效的数据库连接对象
     * @param tableName 待检查的表名
     * @return 当表为空(无记录)时返回true,否则返回false
     * @throws SQLException 若执行SQL查询时发生错误
     */
    private boolean isTableEmpty(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt("count") == 0;
        }
    }
}
