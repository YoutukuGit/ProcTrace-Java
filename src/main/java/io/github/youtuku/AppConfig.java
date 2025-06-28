package io.github.youtuku;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 应用程序配置管理类，统一处理所有配置和数据文件操作。
 *
 * 关键路径常量：
 * - 配置目录: APP_DIRECTORY = "ProcessTracer" (位于用户文档目录下)
 * - 配置文件: CONFIG_FILE = "config.json" (存储目标进程列表)
 * - 数据文件: APPDATA_FILE = "appData.json" (存储进程运行时间数据)
 * 
 * 默认配置常量：
 * - 配置键: CONFIG_KEY_DEFAULT = "TargetProcesses"
 * - 目标进程示例: CONFIG_PROCESS_DEFAULT = "steam.exe, explorer.exe"
 * 
 * @author youtuku
 */
public class AppConfig {
    static final String APP_DIRECTORY = "ProcessTracer";
    static final String CONFIG_FILE = "config.json";
    static final String APPDATA_FILE = "appData.json";
    static final String CONFIG_KEY_DEFAULT = "TargetProcesses";
    static final String CONFIG_PROCESS_DEFAULT = "steam.exe, explorer.exe";
    final String userHome = System.getProperty("user.home");
    final Path documentsPath = Paths.get(userHome, "Documents");
    final String directoryPath = documentsPath.toString() + "\\" + APP_DIRECTORY;
    final String configFile = directoryPath + "\\" + CONFIG_FILE;
    final String appDataFile = directoryPath + "\\" + APPDATA_FILE; 

    /**
     * 确保指定目录存在（不存在时尝试创建）
     * @param dirPath 要检查/创建的目录路径
     * @return 目录已存在或目录不存在但成功创建时返回true,目录不存在且创建失败时返回false
     */
    private boolean ensureDirectoryExists(String dirPath){
        var path = Paths.get(dirPath);
        if ( !Files.exists(path) ) {
            var file = new File(dirPath);
            if(file.mkdir()){
                System.out.println("目录创建成功:" + dirPath);
                return true;
            }
            else{
                System.err.println("目录创建失败:" + dirPath);
                return false;
            }
        }
        else{
            return true;
        }
    }

    /**
     * 初始化创建用户配置文件
     * @throws IOException 当配置目录创建失败或文件写入失败时抛出
     */
    void createDefaultConfig() throws IOException{
        if( !ensureDirectoryExists(directoryPath) ){
            throw new IOException("无法创建配置目录: " + directoryPath);
        }

        try(FileWriter writer = new FileWriter(configFile)){
            var jsonOBJ = new JsonObject();
            jsonOBJ.addProperty(CONFIG_KEY_DEFAULT, CONFIG_PROCESS_DEFAULT);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonOBJ, writer);
            }
        System.out.println("配置文件创建成功:" + configFile);
    }

    /**
     * 初始化创建应用程序数据文件
     * @throws IOException 当数据目录创建失败或文件写入失败时抛出
     */
    void createDefaultAppData() throws IOException{
        var jsonArray = new JsonArray();
        if(ensureDirectoryExists(directoryPath)){
            try(FileWriter writer = new FileWriter(appDataFile)){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(jsonArray, writer);
            }
        System.out.println("数据文件创建成功:" + appDataFile);
        }
    }

    /**
     * 将新进程数据写入到数据文件,若不存在文件则先创建
     * @param dataStore 包含进程运行时间数据的对象
     * @throws IOException 若无法创建目录或写入文件
     */
    void appendtoAppData(DataStore dataStore) throws IOException{
        if (!ensureDirectoryExists(directoryPath)) {
            throw new IOException("数据目录不存在: " + directoryPath);
        }

        JsonArray jsonArray;
        var file = new File(appDataFile);

        if ( !file.exists() || file.length() == 0 ) {
            jsonArray = new JsonArray();
            ensureDirectoryExists(directoryPath);
        }
        else{
            try (Reader reader = new FileReader(file)) {
            jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
            }
        }

        var newEntry = new JsonObject();
        newEntry.addProperty(DataStore.KEY_PROC_NAME, dataStore.getProcName());
        newEntry.addProperty(DataStore.KEY_START_TIME, dataStore.getStartTime().toString());
        newEntry.addProperty(DataStore.KEY_DURATION, dataStore.getDuration().toString());
        jsonArray.add(newEntry);

        try (FileWriter writer = new FileWriter(file)) {
            var gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonArray, writer);
        }
        System.out.println("成功保存数据到: " + appDataFile);
    }

}
