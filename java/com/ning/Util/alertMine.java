package com.ning.Util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class alertMine {
    /*
     * 免责声明
     */
    public static void initAlert() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("免责声明");
        alert.setHeaderText("用户协议");
        alert.setContentText("本程序作者初衷是为了完成数据库课设和学习javafx。\n" +
                "使用该工具必须遵守国家有关的政策和法律，如刑法、国家安全法、保密法、计算机信息系统安全保护条例等，保护国家利益，保护国家安全，\n" +
                "对于违法使用该工具而引起的一切责任，由用户负全部责任。一旦您使用了本程序，将视为您已清楚了解上列全部声明并且完全同意。\n" +
                "本程序仅供合法的渗透测试以及爱好者参考学习。\n");

        ButtonType buttonTypeAgree = new ButtonType("同意");
        ButtonType buttonTypeCancel = new ButtonType("不同意", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(buttonTypeAgree, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() != buttonTypeAgree) {
            Platform.exit();
        }
    }
}
