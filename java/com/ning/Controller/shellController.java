package com.ning.Controller;

import com.ning.Model.initButtonMonitorAndConnection;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class shellController {
    @FXML private VBox xpCmdshellContainer;
    @FXML private TextArea terminalOutput;
    @FXML private TextField commandInput;

    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private boolean isProcessingCommand = false;

    @FXML
    public void initialize() {
        setupTerminal();
        printWelcomeMessage();
        setupCommandInput();
    }

    private void setupTerminal() {
        // 样式已经在FXML中设置，这里只需要初始化行为
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);
    }

    private void setupCommandInput() {
        commandInput.setOnKeyPressed(this::handleCommandInputKeyPress);
    }

    private void printWelcomeMessage() {
        appendOutput("xp_cmdshell终端\n");
        appendOutput("输入 'help' 获取帮助信息\n\n");
        appendOutput("Command> ");
    }

    private void appendOutput(String text) {
        terminalOutput.appendText(text);
        scrollToBottom();
    }

    private void scrollToBottom() {
        terminalOutput.setScrollTop(Double.MAX_VALUE);
    }

    private void handleCommandInputKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            executeCommand(commandInput.getText().trim());
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
        if (command.isEmpty()) {
            appendOutput("\nCommand> ");
            return;
        }

        // 添加到历史记录
        if (commandHistory.isEmpty() || !commandHistory.get(commandHistory.size() - 1).equals(command)) {
            commandHistory.add(command);
        }
        historyIndex = commandHistory.size();

        appendOutput(command + "\n");

        // 处理特殊命令
        if (handleSpecialCommand(command)) {
            return;
        }

        // 执行 xp_cmdshell
        executeXPCMDShell(command);
    }

    private boolean handleSpecialCommand(String command) {
        switch (command.toLowerCase()) {
            case "exit":
                appendOutput("使用关闭按钮退出终端");
                return true;

            case "help":
                appendOutput("可用命令:\n");
                appendOutput("  help     - 显示帮助信息\n");
                appendOutput("  cls      - 清屏\n");
                appendOutput("  exit     - 显示退出信息\n");
                appendOutput("  其他命令将通过 xp_cmdshell 执行\n\nCommand> ");
                return true;

            case "cls":
                terminalOutput.clear();
                appendOutput("Command> ");
                return true;

            default:
                return false;
        }
    }

    private void executeXPCMDShell(String command) {
        // 先检查执行条件
        if (!checkXPCMDShellConditions()) {
            appendOutput("Command> ");
            return;
        }

        try {
            String safeCommand = command.replace("'", "''");
            String sql = "EXEC xp_cmdshell '" + safeCommand + "'";

            try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                boolean hasOutput = false;
                while (rs.next()) {
                    String line = rs.getString(1);
                    if (line != null) {
                        appendOutput(line + "\n");
                        hasOutput = true;
                    }
                }

                if (!hasOutput) {
                    appendOutput("命令执行完成\n");
                }
            }
        } catch (SQLException e) {
            appendOutput("错误: " + e.getMessage() + "\n");
        }

        appendOutput("Command> ");
    }

    private boolean checkXPCMDShellConditions() {
        //检查数据库连接
        if (initButtonMonitorAndConnection.conn == null) {
            appendOutput("\n错误: 数据库未连接\n");
            return false;
        }

        //检查是否为SQL Server
        if (initButtonMonitorAndConnection.dbSelect != 2) {
            appendOutput("\n错误: xp_cmdshell仅支持SQL Server\n");
            return false;
        }

        //检查xp_cmdshell是否启用
        if (!isXPCMDShellEnabled()) {
            appendOutput("\n错误: xp_cmdshell未启用\n");
            appendOutput("请使用以下SQL命令启用:\n");
            appendOutput("exec sp_configure 'show advanced options', 1;reconfigure;\n");
            appendOutput("exec sp_configure 'xp_cmdshell',1;reconfigure;\n");
            return false;
        }

        //检查执行权限
        if (!hasXPCMDShellPermission()) {
            appendOutput("\n错误: 当前用户没有执行xp_cmdshell的权限\n");
            appendOutput("请让管理员授予权限:\n");
            appendOutput("GRANT EXECUTE ON xp_cmdshell TO [您的用户名];\n");
            return false;
        }

        return true;
    }

    private boolean isXPCMDShellEnabled() {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT CONVERT(INT, value) AS config_value " +
                             "FROM sys.configurations " +
                             "WHERE name = 'xp_cmdshell'")) {

            if (rs.next()) {
                return rs.getInt("config_value") == 1;
            }
        } catch (SQLException e) {
            appendOutput("\n警告: 无法检查xp_cmdshell状态 - " + e.getMessage() + "\n");
        }
        return false;
    }

    private boolean hasXPCMDShellPermission() {
        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT IS_SRVROLEMEMBER('sysadmin') AS is_sysadmin")) {

            if (rs.next()) {
                // 如果是 sysadmin 角色成员(返回1)，则有权限
                return rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            appendOutput("\n警告: 无法检查 sysadmin 角色 - " + e.getMessage() + "\n");

            // 如果检查 sysadmin 失败，尝试直接检查 xp_cmdshell 执行权限
            try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT HAS_PERMS_BY_NAME('xp_cmdshell', 'OBJECT', 'EXECUTE') AS can_execute")) {

                if (rs.next()) {
                    return rs.getInt(1) == 1;
                }
            } catch (SQLException ex) {
                appendOutput("\n警告: 无法检查 xp_cmdshell 权限 - " + ex.getMessage() + "\n");
            }
        }
        return false;
    }
}