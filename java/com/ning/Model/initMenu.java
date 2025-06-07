package com.ning.Model;

import com.ning.Controller.mainViewController;
import javafx.scene.control.Alert;

public class initMenu {

    public static void init(mainViewController mvc){
        mvc.getMi_mysql().setOnAction(actionEvent -> {
            initButtonMonitorAndConnection.dbSelect = 1; // 表明使用mysql
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("提示");
//            alert.setContentText("成功选择mysql数据库，enjoy！");
            mvc.getLb_State().setText("成功选择mysql数据库，enjoy！");
//            alert.showAndWait();
        });

        mvc.getMi_mssql().setOnAction(actionEvent -> {
            initButtonMonitorAndConnection.dbSelect = 2; // 表明使用mssql
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setContentText("mssql等待完善，\n目前mssql暂不支持sql监视");
            mvc.getLb_State().setText("成功选择mssql数据库，enjoy！");
            alert.showAndWait();
        });

        mvc.getMi_Author().setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("作者");
            alert.setContentText("23信安1 学号 23385115");
            alert.showAndWait();
        });

        mvc.getMi_Version().setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("版本");
            alert.setContentText("dbMonitor2.0");
            alert.showAndWait();
        });

        mvc.getMi_httpProxy().setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("代理");
            alert.setContentText("等待完善，建议使用Proxifier设置局部代理方便");
            alert.showAndWait();
        });

        mvc.getMi_socksProxy().setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setTitle("代理");
            alert.setContentText("等待完善，建议使用Proxifier设置局部代理方便");
            alert.showAndWait();
        });
    }
}
