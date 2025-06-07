package com.ning;

import com.ning.Util.banner;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.net.URL;
import java.util.Objects;
import com.ning.Controller.udfController;

public class StartApp extends Application {

    public static void main(String[] args) {
        System.out.println(banner.BANNER);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL mainFxmlUrl = getClass().getResource("/View/mainView.fxml");
        Objects.requireNonNull(mainFxmlUrl, "FXML文件未找到: /View/mainView.fxml");
        Parent root = FXMLLoader.load(mainFxmlUrl);

        Scene scene = new Scene(root, 800, 650);
        primaryStage.setTitle("dbMonitor");
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/image/image1.jpg"))
        ));
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(event -> {
            System.exit(0);
        });
        primaryStage.show();
    }
}