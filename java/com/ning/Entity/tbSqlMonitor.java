package com.ning.Entity;

public class tbSqlMonitor {

    private int index;
    private String date;
    private String sql;

    public tbSqlMonitor(int i,String d,String s){
        this.index = i;
        this.date = d;
        this.sql = s;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

}
