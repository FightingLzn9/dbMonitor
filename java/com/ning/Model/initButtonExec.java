package com.ning.Model;

import com.ning.Controller.mainViewController;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import java.sql.*;

public class initButtonExec {
    public static void alerting(Alert.AlertType type, String s1, String s2) {
        Alert alert = new Alert(type);
        alert.setTitle(s1);
        alert.setHeaderText(null);
        alert.setContentText(s2);
        alert.showAndWait();
    }

    public static void init(mainViewController mvc) {
        // 初始化TextArea样式
        TextArea resultArea = mvc.getTa_Log();
        resultArea.getStylesheets().add(
                initButtonExec.class.getResource("/sql-result.css").toExternalForm()
        );
        resultArea.getStyleClass().add("sql-result");

        mvc.getBu_Exec().setOnAction(actionEvent -> {
            if (initButtonMonitorAndConnection.conn == null) {
                alerting(Alert.AlertType.ERROR, "错误", "数据库尚未连接，请先建立数据库连接！");
                return;
            }

            String sql = mvc.getTf_Sql().getText().trim();
            if (sql.isEmpty()) {
                alerting(Alert.AlertType.WARNING, "警告", "请输入要执行的SQL语句！");
                return;
            }

            try {
                resultArea.clear();
                TextFlow textFlow = new TextFlow();
                Text header = new Text("执行SQL: ");
                header.getStyleClass().add("sql-header");

                Text sqlText = new Text(sql + "\n\n");
                sqlText.getStyleClass().add("sql-code");

                textFlow.getChildren().addAll(header, sqlText);
                resultArea.appendText(textFlow.getChildren().stream()
                        .map(node -> ((Text) node).getText())
                        .reduce("", String::concat));

                long startTime = System.currentTimeMillis();

                // 改进的执行逻辑
                ExecutionResult result = executeSQL(sql);
                resultArea.appendText(result.getOutput());

                long endTime = System.currentTimeMillis();

                Text timeHeader = new Text("\n执行耗时: ");
                timeHeader.getStyleClass().add("sql-header");

                Text timeValue = new Text((endTime - startTime) + "ms\n");
                timeValue.getStyleClass().add("sql-value");

                resultArea.appendText(timeHeader.getText() + timeValue.getText());

                mvc.getLb_State().setText(result.isSuccess() ? "SQL执行成功！" : "SQL执行失败~");

            } catch (SQLException e) {
                handleSQLException(resultArea, e);
                mvc.getLb_State().setText("SQL执行失败~");
            }
        });
    }

    private static ExecutionResult executeSQL(String sql) throws SQLException {
        String cleanedSql = removeSqlComments(sql).trim();
        String lowerSql = cleanedSql.toLowerCase();
        ExecutionResult result = new ExecutionResult();

        try (Statement stmt = initButtonMonitorAndConnection.conn.createStatement()) {
            // 处理存储过程和xp_cmdshell
            if (lowerSql.startsWith("exec ") || lowerSql.startsWith("execute ")) {
                return handleExecStatement(stmt, cleanedSql);
            }

            // 处理常规SQL
            boolean hasResultSet = stmt.execute(cleanedSql);

            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    result.setOutput(processResultSet(rs));
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                result.setOutput("影响行数: " + updateCount + "\n");
            }

            result.setSuccess(true);
            return result;
        }
    }

    private static ExecutionResult handleExecStatement(Statement stmt, String sql) throws SQLException {
        ExecutionResult result = new ExecutionResult();
        StringBuilder output = new StringBuilder();

        // 特殊处理xp_cmdshell
        if (sql.toLowerCase().contains("xp_cmdshell")) {
            return handleXPCMDShell(stmt, sql);
        }

        // 处理普通存储过程
        boolean hasResultSet = stmt.execute(sql);

        if (hasResultSet) {
            try (ResultSet rs = stmt.getResultSet()) {
                output.append(processResultSet(rs));
            }
        } else {
            int updateCount = stmt.getUpdateCount();
            if (updateCount == -1) {
                output.append("存储过程执行成功\n");
            } else {
                output.append("影响行数: ").append(updateCount).append("\n");
            }
        }

        result.setOutput(output.toString());
        result.setSuccess(true);
        return result;
    }

    private static ExecutionResult handleXPCMDShell(Statement stmt, String sql) throws SQLException {
        ExecutionResult result = new ExecutionResult();
        StringBuilder output = new StringBuilder();

        boolean hasResultSet = stmt.execute(sql);

        if (hasResultSet) {
            try (ResultSet rs = stmt.getResultSet()) {
                boolean hasOutput = false;
                while (rs.next()) {
                    String line = rs.getString(1);
                    if (line != null) {
                        output.append(line).append("\n");
                        hasOutput = true;
                    }
                }

                if (!hasOutput) {
                    output.append("命令执行完成，无输出\n");
                }
            }
        } else {
            output.append("命令执行完成\n");
        }

        result.setOutput(output.toString());
        result.setSuccess(true);
        return result;
    }

    private static String processResultSet(ResultSet rs) throws SQLException {
        StringBuilder output = new StringBuilder();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 输出列名
        output.append("\n结果集:\n");
        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            header.append(String.format("%-20s", metaData.getColumnName(i)));
        }
        output.append(header.toString()).append("\n");
        output.append("-".repeat(columnCount * 20)).append("\n");

        // 输出数据
        int rowCount = 0;
        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                row.append(String.format("%-20s", value != null ? value : "NULL"));
            }
            output.append(row.toString()).append("\n");
            rowCount++;
        }

        output.append("-".repeat(columnCount * 20)).append("\n");
        output.append("查询返回 ").append(rowCount).append(" 行数据\n");

        return output.toString();
    }

    private static void handleSQLException(TextArea resultArea, SQLException e) {
        Text errorHeader = new Text("执行错误: ");
        errorHeader.getStyleClass().add("sql-error-header");

        Text errorMessage = new Text(e.getMessage() + "\n");
        errorMessage.getStyleClass().add("sql-error-message");

        resultArea.appendText(errorHeader.getText() + errorMessage.getText());
    }

    private static String removeSqlComments(String sql) {
        return sql.replaceAll("--.*", "").replaceAll("/\\*.*?\\*/", "");
    }

    private static class ExecutionResult {
        private boolean success;
        private String output;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
    }
}