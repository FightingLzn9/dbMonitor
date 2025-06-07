package com.ning.Controller;

import com.ning.Entity.tbSqlMonitor;
import com.ning.Model.initButtonExec;
import com.ning.Model.initButtonMonitorAndConnection;
//import com.ning.Model.initLabel;
import com.ning.Model.initMenu;
import com.ning.Model.initTbView;
import com.ning.Util.alertMine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class mainViewController{

    @FXML
    private MenuBar MenuBar;

    @FXML
    private VBox VBoxMain;

    @FXML
    private Button bu_Con;

    @FXML
    private Button bu_Update;

    @FXML
    private Label lb_State;

    @FXML
    private Label lb_dbNowName;

    @FXML
    private Menu menu_About;

    @FXML
    private Menu menu_New;

    @FXML
    private Menu menu_Proxy;

    @FXML
    private MenuItem mi_Author;

    @FXML
    private MenuItem mi_Version;

    @FXML
    private MenuItem mi_httpProxy;

    @FXML
    private MenuItem mi_mssql;

    @FXML
    private MenuItem mi_mysql;

    @FXML
    private MenuItem mi_mysql1;

    @FXML
    private MenuItem mi_socksProxy;

    @FXML
    private Tab tb_sqlExecute;

    @FXML
    private Tab tb_sqlMonitor;

    @FXML
    private Tab tb_udf;

    @FXML
    private Tab tb_unser;

    @FXML
    private TextField tf_Host;

    @FXML
    private PasswordField tf_Pass;

    @FXML
    private TextField tf_Port;

    @FXML
    private TextField tf_User;

    @FXML
    private TabPane tp_Main;

    @FXML
    private Tab tp_xpcs;

    @FXML
    private TableColumn<tbSqlMonitor,String> tbc_DATE;

    @FXML
    private TableColumn<tbSqlMonitor,Integer> tbc_ID;

    @FXML
    private TableColumn<tbSqlMonitor,String> tbc_SQL;

    @FXML
    private TableView<tbSqlMonitor> tbv_SqlMonitor;

    @FXML
    private TextField tf_Sql;

    @FXML
    private Button bu_Clear;

    @FXML
    private Button bu_Exec;

    @FXML
    private TextArea ta_Log;

    @FXML private Tab xpCmdshellTab;

    public TextField getTf_Sql() {
        return tf_Sql;
    }

    public Button getBu_Clear() {
        return bu_Clear;
    }

    public Button getBu_Exec() {
        return bu_Exec;
    }

    public javafx.scene.control.MenuBar getMenuBar() {
        return MenuBar;
    }


    public Button getBu_Con() {
        return bu_Con;
    }

    public Button getBu_Update() {
        return bu_Update;
    }

    public Label getLb_State() {
        return lb_State;
    }

    public MenuItem getMi_Author() {
        return mi_Author;
    }

    public MenuItem getMi_Version() {
        return mi_Version;
    }

    public MenuItem getMi_mssql() {
        return mi_mssql;
    }

    public MenuItem getMi_mysql() {
        return mi_mysql;
    }

    public TextField getTf_Host() {
        return tf_Host;
    }

    public PasswordField getTf_Pass() {
        return tf_Pass;
    }

    public TextField getTf_Port() {
        return tf_Port;
    }

    public TextField getTf_User() {
        return tf_User;
    }

    public TableColumn<tbSqlMonitor,String> getTbc_DATE() {
        return tbc_DATE;
    }

    public MenuItem getMi_httpProxy() {
        return mi_httpProxy;
    }

    public MenuItem getMi_socksProxy() {
        return mi_socksProxy;
    }

    public TableColumn<tbSqlMonitor,Integer> getTbc_ID() {
        return tbc_ID;
    }

    public TableColumn<tbSqlMonitor,String> getTbc_SQL() {
        return tbc_SQL;
    }

    public TableView<tbSqlMonitor> getTbv_SqlMonitor() {
        return tbv_SqlMonitor;
    }

    public mainViewController(){}

    public TextArea getTa_Log() {
        return ta_Log;
    }
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    @FXML
    public void initialize(){
        initConfigComponents();
        alertMine.initAlert();
    }

    private void initConfigComponents() {
        // 初始化各组件
        initMenu.init(this);
        initButtonMonitorAndConnection.init(this);
        initTbView.init(this);
        this.lb_State.setText("请先选择数据库类型");
        initButtonExec.init(this);

        // 设置Tab切换监听
        tp_Main.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == xpCmdshellTab) {
                handleXPCmdShellTabSelection();
            } else if (newTab == tb_udf) {
                handletbUdfTabSelection();
            } else if (newTab == clr_CS) {
                handletbCLRTabSelection();
            }
        });
    }

    //以下是xp_cmdshell
    Object shellController = null;
    private void handleXPCmdShellTabSelection() {
        if (!testIfConnectionValid1()) {
            // 如果检查不通过，切换回上一个Tab
            tp_Main.getSelectionModel().selectPrevious();
            return;
        }

        // 初始化shellController（只初始化一次）
        if(shellController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/xpcmdshell-terminal.fxml"));
                Node content = loader.load();
                shellController = loader.getController();
                xpCmdshellTab.setContent(content);
            } catch (IOException e) {
                showErrorAlert("加载xp_cmdshell界面失败", e.getMessage());
                tp_Main.getSelectionModel().selectPrevious();
            }
        }
    }

    private boolean testIfConnectionValid1() {   // 用于xp_cmdshell
        if (initButtonMonitorAndConnection.conn == null) {
            showErrorAlert("错误", "数据库未连接！");
            return false;
        }

        if (initButtonMonitorAndConnection.dbSelect != 2) {
            showErrorAlert("错误", "xp_cmdshell仅支持MSSQL数据库！");
            return false;
        }

        return true;
    }

    Object udfController = null;
    private void handletbUdfTabSelection(){
        if(!testIfConnectionValid2()){
            // 如果检查不通过，切换回上一个Tab
            tp_Main.getSelectionModel().selectPrevious();
            return;
        }
        try{
            if(udfController == null){
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/udfshell.fxml"));
                Node content = loader.load();
                udfController = loader.getController();
                tb_udf.setContent(content);
            }
        }catch (IOException e){
            showErrorAlert("加载udf提权页面失败",e.getMessage());
            tp_Main.getSelectionModel().selectPrevious();  // 回到之前的tab
        }
    }

    @FXML
    private Tab clr_CS;
    Object clrController = null;
    private void handletbCLRTabSelection(){
        if(!testIfConnectionValid1()){
            // 如果检查不通过，切换回上一个Tab
            tp_Main.getSelectionModel().selectPrevious();
            return;
        }
        try{
            if(clrController == null){
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/gotocs.fxml"));
                Node content = loader.load();
                clrController = loader.getController();
                clr_CS.setContent(content);
            }
        }catch (IOException e){
            showErrorAlert("加载clr上线页面失败",e.getMessage());
            tp_Main.getSelectionModel().selectPrevious();  // 回到之前的tab
        }
    }

    private boolean testIfConnectionValid2() {
        if (initButtonMonitorAndConnection.conn == null) {
            showErrorAlert("错误", "数据库未连接！");
            return false;
        }

        if (initButtonMonitorAndConnection.dbSelect != 1) {
            showErrorAlert("错误", "udf提权仅支持mysql数据库");
            return false;
        }
        return true;
    }
}
