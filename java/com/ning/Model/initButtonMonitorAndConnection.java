package com.ning.Model;

import com.ning.Dao.mssqlDriver;
import com.ning.Dao.mysqlDriver;
import com.ning.Entity.mssqlProperty;
import com.ning.Entity.sqlHome;
import com.ning.Entity.mysqlProperty;
import com.ning.Entity.tbSqlMonitor;
import com.ning.Util.timeForm;
import com.ning.Controller.mainViewController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.sql.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class initButtonMonitorAndConnection {
    public static int dbSelect = 0;
    public static Connection conn;
    public static Timestamp lasTs;
    private static ScheduledExecutorService executor;
    private static volatile boolean isMonitoring = false;
    private static ObservableList<tbSqlMonitor> tableList = FXCollections.observableArrayList();

    public static void alerting(Alert.AlertType e, String s) {
        Platform.runLater(() -> {
            Alert alert = new Alert(e);
            alert.setHeaderText(null);
            alert.setTitle(e == Alert.AlertType.ERROR ? "错误" : "提示");
            alert.setContentText(s);
            alert.showAndWait();
        });
    }

    public static void init(mainViewController mvc) {
        AtomicBoolean conSuccess = new AtomicBoolean(false);
        // 数据库连接按钮
        mvc.getBu_Con().setOnAction(actionEvent -> {
            mysqlProperty mysp = new mysqlProperty(mvc);
            mssqlProperty mssp = new mssqlProperty(mvc);
            lasTs = new Timestamp(new Date().getTime());

            if (dbSelect == 0) {
                alerting(Alert.AlertType.ERROR, "请先选择数据库类型！");
            }
            else if (dbSelect == 1) {
                String dbUrl = String.format("jdbc:mysql://%s:%s/mysql?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                        mysp.getDbHost(), mysp.getDbPort());
                mysqlDriver mysqlcon = new mysqlDriver(dbUrl, mysp.getDbUser(), mysp.getDbPass());
                try {
                    conn = mysqlcon.doCon();
                    if (conn != null) {
                        conSuccess.set(true);
                        tableList.clear();
                        mvc.getTbv_SqlMonitor().setItems(tableList);

                        // 开启日志
                        conn.prepareStatement(sqlHome.OpenLog).executeUpdate();
                        //conn.prepareStatement(sqlHome.Opencw).executeUpdate();
                        conn.prepareStatement(sqlHome.changeLog).executeUpdate();

                        //alerting(Alert.AlertType.INFORMATION, "数据库连接成功！");
                        mvc.getLb_State().setText(String.format("[%s]:%s",timeForm.formTime(null),"数据库连接成功"));
                    }
                } catch (SQLException e) {
                    alerting(Alert.AlertType.ERROR, "数据库连接失败: " + e.getMessage());
                }
            }
            else if (dbSelect == 2) {
                // MSSQL连接代码
                String dbUrl = String.format("jdbc:sqlserver://%s:%s;databaseName=master;encrypt=true;trustServerCertificate=true",
                        mssp.getDbHost(), mssp.getDbPort());

                mssqlDriver mssqlcon = new mssqlDriver(dbUrl, mssp.getDbUser(), mssp.getDbPass());
                try {
                    conn = mssqlcon.doCon();
                    if (conn != null) {
                        conSuccess.set(true);
                        tableList.clear();
                        mvc.getTbv_SqlMonitor().setItems(tableList);

                        mvc.getLb_State().setText(String.format("[%s]:%s",
                                timeForm.formTime(null), "数据库连接成功"));
                    }
                } catch (SQLException e) {
                    alerting(Alert.AlertType.ERROR, "MSSQL连接失败: " + e.getMessage());
                }
            }
        });

        // 数据库更新按钮 - 改为启动/停止监控
        mvc.getBu_Update().setOnAction(actionEvent -> {

            if (!conSuccess.get()) {
                alerting(Alert.AlertType.ERROR, "数据库未连接，请先连接数据库！");
                return;
            }
            mvc.getLb_State().setText(String.format("[%s]:%s",timeForm.formTime(null),"更新成功，开启监视"));
            if (!isMonitoring) {
                startMonitoring(mvc);
                mvc.getBu_Update().setText("停止监控");
            } else {
                stopMonitoring();
                mvc.getBu_Update().setText("开始监控");
            }
        });
    }

    private static void startMonitoring(mainViewController mvc) {
        if (executor != null) {
            executor.shutdownNow();
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        isMonitoring = true;

        executor.scheduleAtFixedRate(() -> {
            try {
                checkNewLogs(mvc);
            } catch (SQLException e) {
                alerting(Alert.AlertType.ERROR, "监控错误: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS); // 每秒检查一次
    }

    private static void stopMonitoring() {
        isMonitoring = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private static void checkNewLogs(mainViewController mvc) throws SQLException {
        if (dbSelect == 1) {
            //MySQL 监控代码
            try (PreparedStatement ps = conn.prepareStatement(sqlHome.LOG_SQL)) {
                ps.setTimestamp(1, lasTs);
                ps.setString(2, "%general_log%");

                ResultSet rs = ps.executeQuery();
                ObservableList<tbSqlMonitor> newLogs = FXCollections.observableArrayList();

                // 获取当前最大ID
                int nextId = tableList.isEmpty() ? 1 :
                        tableList.stream()
                                .mapToInt(tbSqlMonitor::getIndex)
                                .max()
                                .orElse(0) + 1;

                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    if (ts.after(lasTs)) {
                        tbSqlMonitor log = new tbSqlMonitor(
                                nextId++,
                                timeForm.formTime(ts),
                                rs.getString(2) + ";"
                        );
                        lasTs = ts;
                        newLogs.add(log);
                    }
                }

                if (!newLogs.isEmpty()) {
                    Platform.runLater(() -> {
                        tableList.addAll(newLogs);
                        mvc.getTbv_SqlMonitor().scrollTo(tableList.size() - 1);
                    });
                }
            }
        }
    }

    public static void shutdown() {
        stopMonitoring();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}