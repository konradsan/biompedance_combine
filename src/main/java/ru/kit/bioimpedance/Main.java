package ru.kit.bioimpedance;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
    public void start(Stage primaryStage) throws Exception {

        Parent root = new Pane();
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root));
        Stage stage = new BioimpedanceStage(20, true, 65, 175, 5, 120, 80, "");

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
