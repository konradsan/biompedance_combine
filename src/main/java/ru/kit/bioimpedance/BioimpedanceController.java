package ru.kit.bioimpedance;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import javafx.util.StringConverter;
import ru.kit.bioimpedance.commands.CheckStatus;
import ru.kit.bioimpedance.commands.Command;
import ru.kit.bioimpedance.commands.Launch;
import ru.kit.bioimpedance.commands.StartTest;
import ru.kit.bioimpedance.control.LineChartWithMarker;
import ru.kit.bioimpedance.dto.*;
import ru.kit.bioimpedance.equipment.*;
import ru.kit.tonometr_comport.Result;
import ru.kit.tonometr_comport.TonometrReader;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static ru.kit.bioimpedance.CustomPointType.*;
import static ru.kit.bioimpedance.Util.deserializeData;
import static ru.kit.bioimpedance.Util.serialize;

public class BioimpedanceController {

    private int age;
    private boolean isMan;
    private int weight;
    private int height;
    private int activityLevel;
    private int systBP;
    private int diastBP;
    private int seconds;

    public int getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(int activityLevel) {
        this.activityLevel = activityLevel;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isMan() {
        return isMan;
    }

    public void setIsMan(boolean isMan) {
        this.isMan = isMan;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getSystBP() {
        return systBP;
    }

    public void setSystBP(int systBP) {
        this.systBP = systBP;
    }

    public int getDiastBP() {
        return diastBP;
    }

    public void setDiastBP(int diastBP) {
        this.diastBP = diastBP;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    private int count = 0;
    private int testTime = 140;
    private final List<CustomPoint> heartRatePoints = new ArrayList<>();
    private final List<Integer> pulseWavePoints = new ArrayList<>();
    private final Color BACKGROUND_COLOR = Color.valueOf("#4b5962");
    private final EquipmentService equipmentService = new EquipmentServiceMock();
//    private boolean testStarted = false;
    private IntegerProperty diastolicPressureProperty = new SimpleIntegerProperty();
    private IntegerProperty systolicPressureProperty = new SimpleIntegerProperty();
    private final int MAX_TIME = 140;
    private int TIME_BETWEEN_FRAMES = 20;
    private boolean pulseIsRun = false;

    final static int portBioimpedance = 8086;
    final static int portHypoxia = 8085;
    private static final int MAX_DATA_POINTS = 8400;//;7302 / 123 * secondsForTest / NUMBER_OF_SKIP;
    private static final int MAX_DATA_VALUES = 150;
    private XYChart.Series<Number, Number> seriesHR;
    private XYChart.Series<Number, Number> seriesSPO2;
    private boolean isTesting = false;
    Thread checkReadyThread;
    private static final int NUMBER_OF_SKIP = 5;

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
    GridPane forChart;

    private LineChartWithMarker<Number, Number> chart;

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
        initHypoxiaChart();

        pieChart.setLegendVisible(false);
        systolicPressureLabel.textProperty().bindBidirectional(systolicPressureProperty, new PressureConverter());
        diastolicPressureLabel.textProperty().bindBidirectional(diastolicPressureProperty, new PressureConverter());

        startChecking();
//        drawHRnSPO2();
    }

    private void startChecking(){

        checkReadyThread = new CheckReadyThread(this, portHypoxia, portBioimpedance);
        checkReadyThread.setDaemon(true);
        checkReadyThread.start();
    }

    class CheckReadyThread extends Thread {

        final private int hypoxiaPort;
        final private int bioPort;
        BioimpedanceController controller;

        public CheckReadyThread(BioimpedanceController controller, int hypoxiaPortNumber, int bioPortNumber) {
            this.controller = controller;
            this.hypoxiaPort = hypoxiaPortNumber;
            this.bioPort = bioPortNumber;
        }

        public void run () {
            try (Socket hypoxiaSocket = new Socket("localhost", this.hypoxiaPort);
                 BufferedWriter outputH = new BufferedWriter(new OutputStreamWriter(hypoxiaSocket.getOutputStream()));
                 BufferedReader brH = new BufferedReader(new InputStreamReader(hypoxiaSocket.getInputStream()));
                 Socket bioSocket = new Socket("localhost", this.bioPort);
                 BufferedWriter outputB = new BufferedWriter(new OutputStreamWriter(bioSocket.getOutputStream()));
                 BufferedReader brB = new BufferedReader(new InputStreamReader(bioSocket.getInputStream()))
            ) {
/*                Command command = new Launch(controller.getAge(), controller.isMan(),
                        controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                        controller.getSystBP(), controller.getDiastBP(), controller.getSeconds());*/
                Command command = new Launch(20, true, 65, 175, 5, 120, 80, 7);
                sendCommand(command, outputB);
                sendCommand(command, outputH);

                int counterPoints = 0;
                do {
                    sendCommand(new CheckStatus(), outputB);
                    sendCommand(new CheckStatus(), outputH);

                    String lineH = brH.readLine();
                    String lineB = brB.readLine();

                    if (lineH == null || lineB == null) break;
                    Data dataB = deserializeData(lineB);
                    Data dataH = deserializeData(lineH);

                    ReadyStatusBio readyStatusB = (ReadyStatusBio) dataB;
                    ReadyStatus readyStatusH = (ReadyStatus) dataH;
                    System.err.println(readyStatusB);
                    System.err.println(readyStatusH);

                    if (readyStatusB.isError()) {
                        System.err.println("Device not found or BC lib not initialized");
                        equipmentService.setEquipmentReady(false);

                    }else if (!readyStatusB.isHands() && !readyStatusB.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands not connected, feet not connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(false);
                        equipmentService.setHandsReady(false);

                    } else if (readyStatusB.isHands() && !readyStatusB.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands connected, feet not connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(false);
                        equipmentService.setHandsReady(true);

                    } else if (!readyStatusB.isHands() && readyStatusB.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands not connected, feet connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(true);
                        equipmentService.setHandsReady(false);

                    } else if (readyStatusB.isHands() && readyStatusB.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands connected, feet connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(true);
                        equipmentService.setHandsReady(true);
                    }

                    if (readyStatusH.isPulse()) {
                        Inspections inspections = (Inspections) deserializeData(brH.readLine());
//                            equipmentService.setLastPulseoximeterValue(inspections.getPulse(), inspections.getSpo2(), inspections.getWave());
                        System.err.println(" HR: " + inspections.getPulse() + " SPO2: " + inspections.getSpo2() + " WAVE: " + inspections.getWave());
                        drawOneInspectionsHRnSPO2(inspections, /*++counterInspections,*/  counterPoints++);
                        showHRnSPO2Num(inspections);

                    } else {
                        controller.disableAll();
                        Platform.runLater(() -> {
                            controller.heartRateLabel.setText("0");
                            controller.spo2Label.setText("0");
                        });
                    }

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupt checking ready");
                    }

                } while (!isTesting && !isInterrupted());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    class CheckReadyBioThread extends Thread {

        final private int port;
        BioimpedanceController controller;

        public CheckReadyBioThread(BioimpedanceController controller, int portNumber) {
            this.controller = controller;
            this.port = portNumber;
        }

        public void run () {
            try (Socket socket = new Socket("localhost", this.port);
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                 BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

/*                Command command = new Launch(controller.getAge(), controller.isMan(),
                        controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                        controller.getSystBP(), controller.getDiastBP(), controller.getSeconds());*/
                Command command = new Launch(20, true, 65, 175, 5, 120, 80, 7);
                sendCommand(command, output);

                do {
                    sendCommand(new CheckStatus(), output);

                    String line = br.readLine();
                    if (line == null) break;
                    Data data = deserializeData(line);

                    ReadyStatusBio readyStatus = (ReadyStatusBio) data;
                    System.err.println(readyStatus);

                    if (readyStatus.isError()) {
                        System.err.println("Device not found or BC lib not initialized");
                        equipmentService.setEquipmentReady(false);

                    }else if (!readyStatus.isHands() && !readyStatus.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands not connected, feet not connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(false);
                        equipmentService.setHandsReady(false);

                    } else if (readyStatus.isHands() && !readyStatus.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands connected, feet not connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(false);
                        equipmentService.setHandsReady(true);

                    } else if (!readyStatus.isHands() && readyStatus.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands not connected, feet connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(true);
                        equipmentService.setHandsReady(false);

                    } else if (readyStatus.isHands() && readyStatus.isLegs()){
                        System.err.println("Device is found, BC lib initialized, hands connected, feet connected");
                        equipmentService.setEquipmentReady(true);
                        equipmentService.setLegsReady(true);
                        equipmentService.setHandsReady(true);

                    }
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupt checking ready");
                    }

                } while (!controller.isTesting && !isInterrupted());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    class CheckReadyHypoxiaThread extends Thread {

        BioimpedanceController controller;
        final private int port;

        public CheckReadyHypoxiaThread(BioimpedanceController controller, int portNumber) {
            this.controller = controller;
            this.port = portNumber;
        }

        public void run () {
            try (Socket socket = new Socket("localhost", this.port);
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                 BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

/*                Command command = new Launch(controller.getAge(), controller.isMan(),
                        controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                        controller.getSystBP(), controller.getDiastBP(), controller.getSeconds());*/
                Command command = new Launch(20, true, 65, 175, 5, 120, 80, 7);
                sendCommand(command, output);

                int counterPoints = 0;
                do {
                    sendCommand(new CheckStatus(), output);

                    String line = br.readLine();
                    if (line == null) break;
                    Data data = deserializeData(line);

                    ReadyStatus readyStatus = (ReadyStatus) data;
                    System.err.println(readyStatus);

                    if (readyStatus.isPulse()) {
                        Inspections inspections = (Inspections) deserializeData(br.readLine());
//                            equipmentService.setLastPulseoximeterValue(inspections.getPulse(), inspections.getSpo2(), inspections.getWave());
                        System.err.println(" HR: " + inspections.getPulse() + " SPO2: " + inspections.getSpo2() + " WAVE: " + inspections.getWave());
                        drawOneInspectionsHRnSPO2(inspections, /*++counterInspections,*/  counterPoints++);
                        showHRnSPO2Num(inspections);

                    } else {
                        controller.disableAll();
                        Platform.runLater(() -> {
                            controller.heartRateLabel.setText("0");
                            controller.spo2Label.setText("0");
                        });
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupt checking ready");
                    }

                } while (!controller.isTesting && !isInterrupted());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendCommand (Command command, BufferedWriter output) {
        try {
            output.write(serialize(command));
            output.newLine();
            output.flush();
        } catch (IOException ex) {
            System.err.println("sendCommand() fail! command - " + command);
            ex.printStackTrace();
        }
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

    private void initHypoxiaChart(){
        chart = new LineChartWithMarker<>(new NumberAxis(), new NumberAxis());

        chart.getStylesheets().add("ru/kit/bioimpedance/css/linechart.css");

        forChart.getChildren().add(chart);

        NumberAxis xAxis = (NumberAxis) chart.getXAxis();

        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(MAX_DATA_POINTS);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);

        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(MAX_DATA_VALUES);
        yAxis.setAutoRanging(false);
        yAxis.setTickUnit(10);
        yAxis.setTickLabelFill(Color.WHITE);

        chart.setVerticalGridLinesVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setLegendVisible(false);

        prepareChart();
    }

    void enableAll() {
        startButton.setDisable(false);
    }

    void disableAll() {
        startButton.setDisable(true);
    }

    //Hypoxia chart
    private void prepareChart() {
        chart.getData().clear();

        seriesHR = new XYChart.Series<>();
        seriesSPO2 = new XYChart.Series<>();

        // Set Name for Series
        seriesHR.setName("Heart rate");
        seriesSPO2.setName("SPO2");

        //set size of UpperBound of Chart
        double upperBound = ((NumberAxis) chart.getXAxis()).getUpperBound();
        ((NumberAxis) chart.getXAxis()).setUpperBound(upperBound / 10);

        // Add Chart Series
        chart.getData().addAll(seriesHR, seriesSPO2);
    }

    private void beforeTest() {

        isTesting = true;
        checkReadyThread.interrupt();
//        disableAll();
        prepareChart();
        System.err.println("Before test");
    }

    void afterTest() {
        isTesting = false;
        seconds = 0;
        startChecking();
        System.err.println("After test");
    }

    //отображение значений SPO2 и HR
    private void showHRnSPO2Num(Inspections inspections) {

        Platform.runLater(() -> {
            this.heartRateLabel.setText("" + inspections.getPulse());
            this.spo2Label.setText("" + inspections.getSpo2());
        });
    }

    private void drawOneInspectionsHRnSPO2 (Inspections inspections, /*int counterInspections,*/ int counterPoints){

        seriesHR.getData().add(new XYChart.Data(counterPoints, inspections.getPulse()));
        seriesSPO2.getData().add(new XYChart.Data(counterPoints, inspections.getSpo2()));

        double upperBound = ((NumberAxis) chart.getXAxis()).getUpperBound();
        if (counterPoints > 0.8 * upperBound) {
            ((NumberAxis) chart.getXAxis()).setUpperBound(upperBound * 1.33);
        }

        Platform.runLater(() -> {
            this.heartRateLabel.setText("" + inspections.getPulse());
            this.spo2Label.setText("" + inspections.getSpo2());
        });
    }

    private void initTimeline() {

        if (isTesting){
            equipmentService.clearWavesValue();
        } else {
            equipmentService.setMockWavesValues();
        }

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

            if (/*!isTesting*/ true) {

                if (!equipmentService.isEquipmentReady()) {
                    handsReadyLabel.setTextFill(Paint.valueOf("#FF0000"));
                    handsReadyLabel.setText("---");
                    legsReadyLabel.setTextFill(Paint.valueOf("#FF0000"));
                    legsReadyLabel.setText("---");
                }
                else {
                    checkReady(equipmentService.isHandsReady(), handsReadyLabel);
                    checkReady(equipmentService.isLegsReady(), legsReadyLabel);
                }
            }

            PulseOximeterValue pulseoximeterValue = equipmentService.getLastPulseoximeterValue();

            if (pulseoximeterValue == null) {
                heartRateLabel.setText("--");
                spo2Label.setText("--");
            } else {
                /*heartRateLabel.setText(pulseoximeterValue.getHeartRate() != null ?
                                        pulseoximeterValue.getHeartRate().toString() : "--");

                spo2Label.setText(pulseoximeterValue.getSpo2() != null ?
                                    pulseoximeterValue.getSpo2().toString() : "--");*/

                startButton.setDisable(!(pulseoximeterValue.getHeartRate() != null && pulseoximeterValue.getSpo2() != null
                        && equipmentService.isHandsReady() && equipmentService.isLegsReady()
                        /*&& diastolicPressureProperty.get()> 0 && systolicPressureProperty.get() > 0)*/ ));
            }

            /*if (!testStarted) {
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
            }*/

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

        beforeTest();

        //Tonometr test
        //TODO

        //Bioimpedance test & show results
        initHeartRhythm();
        initPulseWave();
        initTimeline();
        initHypoxiaChart();

        StartTestBioImpedance startTestBioImpedance = new StartTestBioImpedance(this, portBioimpedance);
        startTestBioImpedance.setDaemon(true);
        startTestBioImpedance.start();

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

                    //Hypoxia test
                    testHypoxiaProgressBarTimer();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        Thread bioimpedanceThread = new Thread(bioimpedance);
        bioimpedanceThread.setDaemon(true);
        bioimpedanceThread.start();

        afterTest();
    }

    class StartTestHypoxia extends Thread {

        final private int port;
        BioimpedanceController controller;

        public StartTestHypoxia(BioimpedanceController controller, int port) {
            this.controller = controller;
            this.port = port;
        }

        public void run() {

            try (Socket socket = new Socket("localhost", this.port);
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                 BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))

            ){
/*                Command command = new Launch(controller.getAge(), controller.isMan(),
                        controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                        controller.getSystBP(), controller.getDiastBP(), controller.getSeconds());*/
                Command command = new StartTest();
                sendCommand(command, output);

                int counterPoints = 0;
                do {
                    String line = br.readLine();

                    if (line == null) break;
                    Data data = deserializeData(line);

                    if (data instanceof Inspections) {
                        Inspections inspections = (Inspections) deserializeData(br.readLine());
                        equipmentService.setLastPulseoximeterValue(inspections.getPulse(), inspections.getSpo2(), inspections.getWave());
                        System.err.println(" HR: " + inspections.getPulse() + " SPO2: " + inspections.getSpo2() + " WAVE: " + inspections.getWave());
                        drawOneInspectionsHRnSPO2(inspections, /*++counterInspections,*/  counterPoints++);
                        showHRnSPO2Num(inspections);
                    } else if (data instanceof LastResearch) {
                        //TODO
                        System.err.println("Hypoxia LastrReaserch is ready");
                    }

                } while(counterPoints <= 8312);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    class StartTestBioImpedance extends Thread{

        final private int port;
        BioimpedanceController controller;

        public StartTestBioImpedance(BioimpedanceController controller, int port) {
            this.controller = controller;
            this.port = port;
        }

        public void run () {
            try (Socket Socket = new Socket("localhost", this.port);
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(Socket.getOutputStream()));
                 BufferedReader br = new BufferedReader(new InputStreamReader(Socket.getInputStream()))
            ) {
/*                Command command = new Launch(controller.getAge(), controller.isMan(),
                        controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                        controller.getSystBP(), controller.getDiastBP(), controller.getSeconds());*/
                Command command = new StartTest();
                sendCommand(command, output);

                do {
                    String line = br.readLine();

                    if (line == null) break;
                    Data data = deserializeData(line);

                    if (data instanceof LastResearch) {
                        LastResearch lastResearch = (LastResearch) data;
                        double ffm = lastResearch.getInspections().get("FFM").getValue();
                        double tbw = lastResearch.getInspections().get("TBW").getValue();
                        double mm = lastResearch.getInspections().get("MM").getValue();
                        equipmentService.setBioimpedanceValue(new BioimpedanceValue(ffm, tbw, mm));

                        System.err.println("Bioimpedance LastResearch is ready");
                        System.err.println(lastResearch);
                        break;
                    }

                } while (isTesting);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void testHypoxiaProgressBarTimer () {

        prepareChart();
        StartTestHypoxia startTestHypoxia = new StartTestHypoxia(this, portHypoxia);
        startTestHypoxia.setDaemon(true);
        startTestHypoxia.start();

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
