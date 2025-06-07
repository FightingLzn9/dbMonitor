package com.ning.Entity;

import com.ning.Controller.mainViewController;

public class mssqlProperty {

    private String dbHost;
    private String dbPort;
    private String dbUser;
    private String dbPass;
    private String dbUrl;

    public mssqlProperty(mainViewController mvc){
        this.dbHost = mvc.getTf_Host().getText().trim();
        this.dbPort = mvc.getTf_Port().getText().trim();
        this.dbUser = mvc.getTf_User().getText().trim();
        this.dbPass = mvc.getTf_Pass().getText().trim();
    }

    public String getDbHost() {
        return dbHost;
    }

    public String getDbPort() {
        return dbPort;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPass() {
        return dbPass;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUrl() {
        return dbUrl;
    }

}
