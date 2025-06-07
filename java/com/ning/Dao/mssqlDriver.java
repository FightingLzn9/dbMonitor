package com.ning.Dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class mssqlDriver {
    private Connection conn;
    private String url;
    private String user;
    private String pass;

    public mssqlDriver(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    public Connection doCon() throws SQLException {
        if (conn == null || conn.isClosed()) {
            this.openConnection();
        }
        return conn;
    }

    private void openConnection() throws SQLException {
        try {
            // 加载 MSSQL JDBC 驱动
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("没有找到 MSSQL 驱动，请确保已添加 mssql-jdbc 依赖", e);
        }

        try {
            // 建立 MSSQL 连接
            conn = DriverManager.getConnection(url, user, pass);
            System.out.println("MSSQL 数据库连接成功");
        } catch (SQLException e) {
            throw new SQLException("MSSQL 数据库连接失败: " + e.getMessage(), e);
        }
    }

    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("MSSQL 数据库连接已关闭");
            } catch (SQLException e) {
                System.err.println("关闭 MSSQL 连接时出错: " + e.getMessage());
            }
        }
    }
}