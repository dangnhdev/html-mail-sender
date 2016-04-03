package com.hoasao.dangnh.jfx;

/**
 * Created by dangg on 3/31/2016.
 */

import javafx.application.*;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        Parent root = FXMLLoader.load(getClass().getResource("view/MainView.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("com/hoasao/dangnh/jfx/view/img/gmail.png"));
        primaryStage.setTitle("HTML Mail Sender - Hoa Sao Group");
        primaryStage.setHeight(visualBounds.getMaxY());
        primaryStage.setMinWidth(660);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
    }
}
