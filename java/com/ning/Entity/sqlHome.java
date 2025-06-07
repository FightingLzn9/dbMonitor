package com.ning.Entity;

public class sqlHome {

    //mysql
    public static final String OpenLog = "SET global general_log = on"; // 开启一般日志
    //public static final String Opencw = "SET GLOBAL log_warnings = 2;"; // 记录报错的语句

    /*
    *
    * 高版本mysql的general_log不记录错误语句
    * 需要在my.ini中设置
    * log_raw=1
    #或者：log-raw=1
    * */
    public static final String changeLog = "SET GLOBAL log_output = 'table'";
    public static  String LOG_SQL =
            "SELECT event_time, argument FROM mysql.general_log " +
                    "WHERE (command_type = 'Query' OR command_type = 'Execute' OR command_type = 'Connect') " +
                    "AND event_time > ? AND argument NOT LIKE ? " +
                    "ORDER BY event_time ASC";
}
