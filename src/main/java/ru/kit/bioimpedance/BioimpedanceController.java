package ru.kit.bioimpedance;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import javafx.util.StringConverter;
import ru.kit.bioimpedance.equipment.*;
import ru.kit.tonometr_comport.Result;
import ru.kit.tonometr_comport.TonometrReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static ru.kit.bioimpedance.CustomPointType.*;

public class BioimpedanceController {


    private int count = 0;
    private int testTime = 140;
    private final List<CustomPoint> heartRatePoints = new ArrayList<>();
    private final List<Integer> pulseWavePoints = new ArrayList<>();
    private final Color BACKGROUND_COLOR = Color.valueOf("#4b5962");
    private final EquipmentService equipmentService = new EquipmentServiceMock();
    private boolean testStarted = false;
    private IntegerProperty diastolicPressureProperty = new SimpleIntegerProperty();
    private IntegerProperty systolicPressureProperty = new SimpleIntegerProperty();
    private final int MAX_TIME = 140;
    private int TIME_BETWEEN_FRAMES = 20;
    private boolean pulseIsRun = false;

    @FXML
    private Canvas heartRhythm;
    @FXML
    private Canvas pulseWave;
    @FXML
    private GridPane innerGrid;
    @FXML
    private PieChart pieChart;
    @FXML
    private Label legsReadyLabel;
    @FXML
    private Label handsReadyLabel;
    @FXML
    private Label heartRateLabel;
    @FXML
    private Label spo2Label;
    @FXML
    private Label systolicPressureLabel;
    @FXML
    private Label diastolicPressureLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Button startButton;
    @FXML
    private Button measurePressureButton;
    @FXML
    private ProgressBar progressBar;

    private final Person person = new Person(75, 175);

    @FXML
    private void initialize() {
        initHeartRhythm();
        initPulseWave();
        initTimeline();

        pieChart.setLegendVisible(false);
        systolicPressureLabel.textProperty().bindBidirectional(systolicPressureProperty, new PressureConverter());
        diastolicPressureLabel.textProperty().bindBidirectional(diastolicPressureProperty, new PressureConverter());
    }

    private void addPulse() {
        int distanceToNextPulse = distanceToNextPulse(equipmentService.getLastPulseoximeterValue().getHeartRate());
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 5, START));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 10, P));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 15, PR_START));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 20, PR_FINISH));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 25, Q));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 27, R));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 29, S));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 34, ST_START));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 42, ST_FINISH));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 50, T));
        heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 54, FINISH));
    }

    private void initPulseWave() {
        innerGrid.widthProperty().addListener((observable, oldValue, newValue) -> {
            pulseWave.getGraphicsContext2D().setFill(BACKGROUND_COLOR);
            pulseWave.setWidth(newValue.doubleValue() / 2 - 8);
            pulseWave.getGraphicsContext2D().fillRect(0, 0, pulseWave.getWidth(), pulseWave.getHeight());
        });
        innerGrid.heightProperty().addListener((observable, oldValue, newValue) -> {
            pulseWave.getGraphicsContext2D().setFill(BACKGROUND_COLOR);
            pulseWave.setHeight(newValue.doubleValue() / 2 - 58);
            pulseWave.getGraphicsContext2D().fillRect(0, 0, pulseWave.getWidth(), pulseWave.getHeight());
        });

    }

    private void initHeartRhythm() {
        innerGrid.widthProperty().addListener((observable, oldValue, newValue) -> {
            heartRhythm.getGraphicsContext2D().setFill(BACKGROUND_COLOR);
            heartRhythm.setWidth(newValue.doubleValue() / 2 - 8);
            heartRhythm.getGraphicsContext2D().fillRect(0, 0, heartRhythm.getWidth(), heartRhythm.getHeight());
        });
        innerGrid.heightProperty().addListener((observable, oldValue, newValue) -> {
            heartRhythm.getGraphicsContext2D().setFill(BACKGROUND_COLOR);
            heartRhythm.setHeight(newValue.doubleValue() / 2 - 58);
            heartRhythm.getGraphicsContext2D().fillRect(0, 0, heartRhythm.getWidth(), heartRhythm.getHeight());
        });
    }

    private void initTimeline() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(TIME_BETWEEN_FRAMES), event -> {
            drawHeartRhythm();
            drawPulseWave();
            count = (int) ((count + 1) % heartRhythm.getWidth());
            if (count == 0) {
                heartRatePoints.clear();
                pulseWavePoints.clear();
                GraphicsContext gc = heartRhythm.getGraphicsContext2D();
                gc.setFill(BACKGROUND_COLOR);
                gc.fillRect(heartRhythm.getWidth() - 2, 0, 2, heartRhythm.getHeight());
                pulseIsRun = false;
            }
            if (!testStarted) {
                checkReady(equipmentService.isHandsReady(), handsReadyLabel);
                checkReady(equipmentService.isLegsReady(), legsReadyLabel);
            }
            PulseOximeterValue pulseoximeterValue = equipmentService.getLastPulseoximeterValue();
            if (pulseoximeterValue == null) {
                heartRateLabel.setText("--");
                spo2Label.setText("--");
            } else {
                heartRateLabel.setText(pulseoximeterValue.getHeartRate() != null ?
                        pulseoximeterValue.getHeartRate().toString() : "--");
                spo2Label.setText(pulseoximeterValue.getSpo2() != null ?
                        pulseoximeterValue.getSpo2().toString() : "--");

                startButton.setDisable(!(pulseoximeterValue.getHeartRate() != null && pulseoximeterValue.getSpo2() != null
                        && equipmentService.isHandsReady() && equipmentService.isLegsReady()
                        && diastolicPressureProperty.get() > 0 && systolicPressureProperty.get() > 0));
            }

        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkReady(boolean ready, Label label) {
        if (ready) {
            label.setTextFill(Paint.valueOf("#00FF00"));
            label.setText("ON");
        } else {
            label.setTextFill(Paint.valueOf("#FF0000"));
            label.setText("OFF");
        }
    }

    private void drawPulseWave() {
        GraphicsContext gc = pulseWave.getGraphicsContext2D();
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, count, pulseWave.getHeight());

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.0);
        double centerY = heartRhythm.getHeight() / 4;
        gc.beginPath();
        gc.moveTo(0, pulseWave.getHeight() - (heartRhythm.getHeight() / 8));
        pulseWavePoints.add(equipmentService.getLastPulseoximeterValue().getWave());
        for (int i = 0; i < count; i++) { //count is equals to pulseWavePoints.size
            gc.lineTo(i, pulseWave.getHeight() - (centerY + pulseWavePoints.get(i)));
        }
        gc.stroke();
        gc.setStroke(Color.YELLOW);
        gc.strokeLine(count, 0, count, pulseWave.getHeight());
    }

    private void drawHeartRhythm() {
        if (!pulseIsRun) {
            if (equipmentService.getLastPulseoximeterValue() != null) {
                addPulse();
                pulseIsRun = true;
            }
        }
        GraphicsContext gc = heartRhythm.getGraphicsContext2D();
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, count, heartRhythm.getHeight());

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.0);
        double centerY = 5 * (heartRhythm.getHeight() / 8);
        gc.beginPath();
        gc.moveTo(0, centerY);
        for (int i = 0; i < heartRatePoints.size(); i++) {
            CustomPoint point = heartRatePoints.get(i);
            if (point.getX() <= count) {
                if (point.getType().equals(START) || point.getType().equals(ST_FINISH)) {
                    gc.lineTo(point.getX(), centerY);
                    if (heartRatePoints.get(i + 2).getX() <= count) {
                        gc.bezierCurveTo(point.getX(), centerY + point.getType().getDeltaY(),
                                heartRatePoints.get(i + 1).getX(), centerY + heartRatePoints.get(i + 1).getType().getDeltaY(),
                                heartRatePoints.get(i + 2).getX(), centerY + heartRatePoints.get(i + 2).getType().getDeltaY());
                    }
                }
                if (!(point.getType().equals(START) ||
                        point.getType().equals(P) ||
                        point.getType().equals(ST_FINISH) ||
                        point.getType().equals(T) ||
                        point.getType().equals(FINISH)
                )) {
                    CustomPoint nextPoint = heartRatePoints.get(i + 1);
                    if (nextPoint.getX() <= count) {
                        gc.lineTo(nextPoint.getX(), centerY + nextPoint.getType().getDeltaY());
                    }
                }
            }
        }
        if (heartRatePoints.size() > 0) {
            CustomPoint point = heartRatePoints.get(heartRatePoints.size() - 1);
            if (point.getType().equals(FINISH)) {
                if (point.getX() <= count) {
                    addPulse();
                }
            }
        }
        //рисуем линию до конца жёлтой линии
        gc.lineTo(count, centerY);
        gc.stroke();

        gc.setStroke(Color.YELLOW);
        gc.strokeLine(count, 0, count, heartRhythm.getHeight());

    }


    @FXML
    private void startTest(ActionEvent actionEvent) {


        testTime = MAX_TIME;
        progressBar.setProgress(0);
        timeLabel.setText(String.format("%d:%02d", testTime / 60, testTime % 60));
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            testTime--;
            timeLabel.setText(String.format("%d:%02d", testTime / 60, testTime % 60));
            double value = ((double) (MAX_TIME - testTime)) / MAX_TIME;
            progressBar.setProgress(value);
        }));
        timer.setCycleCount(testTime);
        timer.play();
        Task<BioimpedanceValue> bioimpedance = new Task<BioimpedanceValue>() {
            @Override
            protected BioimpedanceValue call() throws Exception {
                while (!equipmentService.isBioimpedanceReady()) {
                    Thread.sleep(1000);
                }
                return equipmentService.getBioimpedanceValue();
            }
        };
        bioimpedance.setOnSucceeded(event -> {
            try {
                BioimpedanceValue bioimpedanceValue = bioimpedance.get();
                if (bioimpedance != null) {
                    pieChart.getData().clear();
                    pieChart.getData().add(new PieChart.Data("% Жировой массы", bioimpedanceValue.getFat() / person.getWeight()));
                    pieChart.getData().add(new PieChart.Data("% Мышечной массы", bioimpedanceValue.getMuscle() / person.getWeight()));
                    pieChart.getData().add(new PieChart.Data("% Воды", bioimpedanceValue.getAllWater() / person.getWeight()));
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        Thread bioimpedanceThread = new Thread(bioimpedance);
        bioimpedanceThread.setDaemon(true);
        bioimpedanceThread.start();
    }

    @FXML
    private void inputSystolicPressure(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("120");
        dialog.setTitle("Систалическое давление");
        dialog.setHeaderText("Ввод систалического давления");
        dialog.setContentText("Введите, пожалуйста, систалическое давление:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            Pattern compile = Pattern.compile("\\d+");
            if (compile.matcher(result.get().trim()).matches()) {
                systolicPressureProperty.setValue(Integer.parseInt(result.get().trim()));
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Некорректный ввод");
                alert.setHeaderText("Некорректный ввод");
                alert.setContentText("Систолическое давление введёно некорректно.");
                alert.showAndWait();
            }
        }
    }


    @FXML
    private void inputDiastolicPressure(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("80");
        dialog.setTitle("Диастолическое давление");
        dialog.setHeaderText("Ввод диастолическое давление");
        dialog.setContentText("Введите, пожалуйста, диастолическое давление:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            Pattern compile = Pattern.compile("\\d+");
            if (compile.matcher(result.get().trim()).matches()) {
                diastolicPressureProperty.setValue(Integer.parseInt(result.get().trim()));
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Некорректный ввод");
                alert.setHeaderText("Некорректный ввод");
                alert.setContentText("Диасталическое давление введёно некорректно.");
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void measurePressure(ActionEvent actionEvent) {
        TonometrReader tonometr = new TonometrReader("COM3");
        Task<Result> longTask = new Task<Result>() {
            @Override
            protected Result call() throws Exception {
                synchronized (tonometr) {
                    tonometr.callPort();
                    while (!tonometr.isReady()) {
                        tonometr.wait(500);
                    }
                }
                return tonometr.getResult();
            }
        };

        longTask.setOnSucceeded(event -> {
            Result result = longTask.getValue();
            if (result == Result.SUCCESS) {
                systolicPressureProperty.setValue(tonometr.getPressure().getSys());
                diastolicPressureProperty.setValue(tonometr.getPressure().getDia());
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Измерение давления");
                alert.setHeaderText("Не удалось измерить давление");
                alert.setContentText("К сожалению, не удалось измерить давление. Пожалуйста, повторите попытку.");
                alert.showAndWait();
            }
            measurePressureButton.setDisable(false);
        });

        measurePressureButton.setDisable(true);
        final Thread th = new Thread(longTask);
        th.setDaemon(true);
        th.start();
    }

    private int distanceToNextPulse(int heartRate) {
        int intervalInSeconds = 1000 / TIME_BETWEEN_FRAMES;
        double heartRateInSecond = heartRate / 60.0D;
        return (int) (intervalInSeconds * heartRateInSecond);
    }
}

class PressureConverter extends StringConverter<Number> {

    @Override
    public String toString(Number object) {
        return object.intValue() == 0 ? "---" : object.toString();
    }

    @Override
    public Number fromString(String string) {
        return Integer.parseInt(string);
    }
}
