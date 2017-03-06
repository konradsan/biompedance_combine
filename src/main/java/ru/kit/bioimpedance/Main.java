package ru.kit.bioimpedance;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.kit.SoundManagerSingleton;

import java.io.IOException;

public class Main extends Application {
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Hello World!");
        Button btn = new Button();
        btn.setText("Базовый тест");
        btn.setOnAction(event -> {
            Stage s = null;
            try {
                s = new BioimpedanceStage(22, true, 82, 183, 0, 120, 80, "", SoundManagerSingleton.getInstance());
            } catch (IOException e) {
                e.printStackTrace();
            }
            s.show();
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
