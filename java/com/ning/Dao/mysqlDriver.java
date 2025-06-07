package com.ning.Dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class mysqlDriver {

    private Connection conn;
    private String url;
    private String user;
    private String pass;

    public mysqlDriver(String url, String user, String pass){
        this.url = url;
        this.user = user;
        this.pass = pass;
    }
    public Connection doCon() throws SQLException {
        if (conn == null){
            this.openConnection();
        }
        return conn;
    }

    private void openConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("没有找到mysql驱动");
        }
        try {
            conn = DriverManager.getConnection(url,user,pass);
            System.out.println("数据库连接成功");
        }catch (SQLException ignored){
                throw new SQLException("数据库连接失败");
        }

    }
}
