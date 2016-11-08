package ru.kit.bioimpedance;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.*;

public class BioimpedanceStage extends Stage {

    private static final String BIOIMPEDANCE_SERVICE_NAME = "BioService";
    private static final String OXI_SERVICE_NAME = "OxiService";

    public BioimpedanceStage(int age, boolean isMale, int width, int height, int activityLevel, int systBP, int diastBP, String path) throws IOException {

        execService("stop", OXI_SERVICE_NAME);

        execService("start", OXI_SERVICE_NAME);

        execService("stop",BIOIMPEDANCE_SERVICE_NAME);

        execService("start", BIOIMPEDANCE_SERVICE_NAME);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kit/bioimpedance/fxml/bioimpedance.fxml"));
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
        controller.setStage(this);

        this.setScene(new Scene(root));

    }

    private void execService(String command, String serviceName){

        try {
            Process p = null;
            p = Runtime.getRuntime().exec("net " + command +" " + serviceName);

            p.waitFor();

            System.out.println(serviceName + ":" + command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                System.err.println(line);
                line = reader.readLine();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*public BioimpedanceStage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kit/bioimpedance/fxml/bioimpedance.fxml"));
        Parent root = loader.load();
        this.setScene(new Scene(root));
        BioimpedanceController controller = loader.getController();
        controller.setStage(this);

    }*/
}
