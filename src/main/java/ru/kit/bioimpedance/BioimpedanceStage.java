package ru.kit.bioimpedance;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.kit.SoundManager;

import java.io.*;

public class BioimpedanceStage extends Stage {

    public BioimpedanceStage(int age, boolean isMale, int width, int height, int activityLevel, int systBP, int diastBP, String path, SoundManager soundManager) throws IOException {
        this(age,isMale,width,height,activityLevel,systBP,diastBP,path,"", soundManager);
    }

    public BioimpedanceStage(int age, boolean isMale, int width, int height, int activityLevel, int systBP, int diastBP, String path, String comPort, SoundManager soundManager) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kit/bioimpedance/fxml/bioimpedance.fxml"));
        loader.setController(new BioimpedanceController(soundManager));
        Parent root = loader.load();

        BioimpedanceController controller = loader.getController();

        controller.setAge(age);
        controller.setMan(isMale);
        controller.setHeight(height);
        controller.setWeight(width);
        controller.setActivityLevel(activityLevel);
        controller.setSystBP(systBP);
        controller.setDiastBP(diastBP);
        controller.setPath(path);
        controller.setComPort(comPort);
        controller.setStage(this);

        this.setOnCloseRequest(event -> {
            try {
                controller.closeConnections();
            }finally {
                this.close();
            }

        });

        this.setScene(new Scene(root));

    }
}
