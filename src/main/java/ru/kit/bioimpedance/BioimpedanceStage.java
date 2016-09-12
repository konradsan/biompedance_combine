package ru.kit.bioimpedance;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BioimpedanceStage extends Stage {
    public BioimpedanceStage() throws IOException {
        Parent root = new FXMLLoader(getClass().getResource("/ru/kit/bioimpedance/fxml/bioimpedance.fxml")).load();
        this.setScene(new Scene(root));
    }
}
