package com.ning.Util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class timeForm {
    public static String formTime(Timestamp ts) {
        SimpleDateFormat time = new SimpleDateFormat("MM-dd HH:mm:ss");
        if (ts != null) {
            time = new SimpleDateFormat("MM-dd HH:mm:ss");
            return time.format(ts);
        }
        return time.format(new Date());
    }
}
