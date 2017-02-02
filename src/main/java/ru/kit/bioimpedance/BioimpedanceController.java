package ru.kit.bioimpedance;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.*;
import org.json.JSONObject;
import ru.kit.bioimpedance.commands.*;
import ru.kit.bioimpedance.control.LineChartWithMarker;
import ru.kit.bioimpedance.dto.*;
import ru.kit.bioimpedance.equipment.*;
import ru.kit.tonometr_comport.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static ru.kit.bioimpedance.CustomPointType.*;
import static ru.kit.bioimpedance.Util.deserializeData;
import static ru.kit.bioimpedance.Util.serialize;
import static ru.kit.bioimpedance.Util.sendCommand;

public class BioimpedanceController {

    private int age;
    private boolean isMan;
    private int weight;
    private int height;
    private int activityLevel;
    private int systBP;
    private int diastBP;
    private String path;

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

    public void setMan(boolean isMan) {
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
        return secondsForTest;
    }

    public void setSeconds(int seconds) {
        this.secondsForTest = seconds;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setComPort(String comPort) {
        this.comPort = comPort;
    }


    private int count = 0;
    private int secondsForTest = 140;
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
    private LinkedList<Integer> heartRateList = new LinkedList<>();

    final static int portBioimpedance = 8086;
    final static int portHypoxia = 8085;
    private static final int MAX_DATA_POINTS = 8400;//;7302 / 123 * secondsForTest / NUMBER_OF_SKIP;
    private static final int MAX_DATA_VALUES = 150;
    private XYChart.Series<Number, Number> seriesHR;
    private XYChart.Series<Number, Number> seriesSPO2;
    private XYChart.Series seriesBio;
    private XYChart.Series<Number, Number> seriesScatter;
    private XYChart.Series<Number, Number> bisectScatter;

    private boolean isTesting = false;
    private Thread checkReadyThread;
    private static final int NUMBER_OF_SKIP = 5;
    private BioimpedanceStage stage;
    private volatile Map<String, Inspection> summorizedLastResearch = new HashMap<>();
    private boolean haveBioLastResearch = false;
    private boolean haveHypoLastResearch = false;
    private String comPort;
    volatile boolean isStageClosed = false;
    volatile Socket hypoxiaSocket = null;
    volatile Socket bioSocket = null;
    private Timeline timeline, timer;
    volatile static int COUNTER_MMM = 0;
    private Task<TonometrData> measureNewTonometrTask;
    private static int measureNewTonometrCount = 0;
    private Task<Void> enterMeassure;

    /*@FXML
    private Canvas heartRhythm;*/
    @FXML
    private LineChart<Number,Number> scatterChart;
    @FXML
    private Canvas pulseWave;
    @FXML
    private GridPane innerGrid;
    @FXML
    private BarChart<Number,String> barChart;//new
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
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
    private Label systolicPressureLabel,systolicPressureLabelPopUp;
    @FXML
    private Label diastolicPressureLabel, diastolicPressureLabelPopUp;
    @FXML
    private Label timeLabel;
    @FXML
    private Button startButton;
    @FXML
    private Button measurePressureButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Button okButton;
    @FXML
    private AnchorPane okEndScreen,badEndScreen, warningScreen, measurePreassure;
    @FXML
    private HBox bioLegend;
    @FXML
    private Button acceptPressureValuesButton;




    @FXML
    private void onContinueWarning(){
        warningScreen.setVisible(false);
        measurePreassure.setVisible(true);
    }
    @FXML
    private void onAcceptMeasurePressure(){
        enterMeassure.cancel(true);
        measurePreassure.setVisible(false);
        startChecking();
    }
    @FXML
    private void initialize() {
        initHeartRhythm();
        initScatterChart();
        initPulseWave();
        initTimeline();
        initHypoxiaChart();

        barChart.setLegendVisible(false);
        systolicPressureLabel.textProperty().bindBidirectional(systolicPressureProperty, new PressureConverter());
        systolicPressureLabelPopUp.textProperty().bindBidirectional(systolicPressureProperty, new PressureConverter());
        diastolicPressureLabel.textProperty().bindBidirectional(diastolicPressureProperty, new PressureConverter());
        diastolicPressureLabelPopUp.textProperty().bindBidirectional(diastolicPressureProperty, new PressureConverter());

        enterMeassure = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (!this.isCancelled()){
                    Thread.sleep(1000);
                    if(diastolicPressureProperty.get()> 0 && systolicPressureProperty.get() > 0){
                        acceptPressureValuesButton.setDisable(false);
                        break;
                    }
                }
                return null;
            }
        };

        Thread enterMeasureThread = new Thread(enterMeassure);
        enterMeasureThread.start();

    }


    @FXML
    private void cancel () {
        closeConnections();
        stage.close();
    }

    @FXML
    private void ok() {

        changeBioKgToPercent();
        writeJSON(summorizedLastResearch);
        closeConnections();
        stage.close();
    }

    void closeConnections(){
        isStageClosed = true;
        if (timeline!=null) timeline.stop();
        if (timer!=null) timer.stop();
        if (measureNewTonometrTask!=null) measureNewTonometrTask.cancel(true);
        if (enterMeassure!=null) enterMeassure.cancel(true);
        closeSocketConnection(bioSocket);
        closeSocketConnection(hypoxiaSocket);

    }

    private void closeSocketConnection(Socket socket){
        if (socket!=null) {
            try {
                if(!socket.isClosed()){
                    socket.shutdownInput();
                    socket.shutdownOutput();
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void setStage (BioimpedanceStage stage) {
        this.stage = stage;
    }

    //начать режим проверки оборудования - графики и поля приложени заполняются значениями
    private void startChecking(){
        checkReadyThread = new CheckReadyThread(this, portHypoxia, portBioimpedance);
        checkReadyThread.setDaemon(true);
        checkReadyThread.start();
    }

    //тред проверки оборудования
    //заполняется график гипоксии, в полях ЧСС и SPO2 отражаются текущие значения
    //передача значений состояния пластин и оборудования в equipmentService
    class CheckReadyThread extends Thread {

        final private int hypoxiaPort;
        final private int bioPort;
        BioimpedanceController controller;

        public CheckReadyThread(BioimpedanceController controller, int hypoxiaPortNumber, int bioPortNumber) {
            this.controller = controller;
            this.hypoxiaPort = hypoxiaPortNumber;
            this.bioPort = bioPortNumber;
        }

        @Override
        public void run () {

            boolean isServicesConnected = false;

            //Ожидание запуска сервисов пульсоксиметра и биомпиданса
            while (!isServicesConnected && !isStageClosed) {
                try {
                    hypoxiaSocket = new Socket("localhost", this.hypoxiaPort);
                    bioSocket = new Socket("localhost", this.bioPort);
                    isServicesConnected = true;
                } catch (IOException e) {
                    System.err.println("Services not running. Waiting...");
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            try (
                 BufferedWriter outputH = new BufferedWriter(new OutputStreamWriter(hypoxiaSocket.getOutputStream()));
                 BufferedReader brH = new BufferedReader(new InputStreamReader(hypoxiaSocket.getInputStream()));
                 BufferedWriter outputB = new BufferedWriter(new OutputStreamWriter(bioSocket.getOutputStream()));
                 BufferedReader brB = new BufferedReader(new InputStreamReader(bioSocket.getInputStream()))
            ) {
                Command command = new Launch(controller.getAge(), controller.isMan(),
                        controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                        controller.getSystBP(), controller.getDiastBP(), controller.getSeconds());

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
                        //equipmentService.setMockWavesValues();
                        Inspections inspections = (Inspections) deserializeData(brH.readLine());
                        System.err.println(" HR: " + inspections.getPulse() + " SPO2: " + inspections.getSpo2() + " WAVE: " + inspections.getWave());
                        drawOneInspectionsHRnSPO2(inspections, counterPoints++);


                    } else {
                        equipmentService.clearWavesValue();
//                        controller.disableAll();
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

                } while (!isTesting && !isInterrupted() && !isStageClosed);
            } catch (IOException ex) {
                ex.printStackTrace();
            }finally {
                try {
                    if (hypoxiaSocket!=null) hypoxiaSocket.close();
                    if (bioSocket!=null) bioSocket.close();
                } catch (IOException e) {
                    System.err.println("Cannot close sockets connections!");
                }
            }
        }
    }



    private void addPulse(int id) {
        if (equipmentService.getLastPulseoximeterValue().getWave() == 0 &&
                equipmentService.getLastPulseoximeterValue().getSpo2() == 0 &&
                equipmentService.getLastPulseoximeterValue().getHeartRate() == 0) {

        } else {
            int distanceToNextPulse = distanceToNextPulse(equipmentService.getLastPulseoximeterValue().getHeartRate());
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 0, START));
            //heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 10, P));
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 1, PR_START));
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 1, PR_FINISH));
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 6, Q));//1ый низ
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 8, R));//1ый верх
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 10, S));//2ой низ
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 15, ST_START));
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 15, ST_FINISH));
            //heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 50, T));
            heartRatePoints.add(new CustomPoint(distanceToNextPulse + count + 15, FINISH));
        }
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
       
    }

    private void initScatterChart() {

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

        bioLegend.setVisible(false);

        //scatterChart.setAnimated(false);
        scatterChart.getXAxis().setAutoRanging(false);
        scatterChart.getYAxis().setAutoRanging(false);

        prepareChart();
    }

    void disableAll() {
        startButton.setDisable(true);
        measurePressureButton.setDisable(true);
//        okButton.setDisable(true);
    }

    void enableAll(){
        startButton.setDisable(false);
    }

    //удаление всех данных с графика гипоксии
    //установка размеров графика по оси Х
    private void prepareChart() {
        chart.getData().clear();

        seriesHR = new XYChart.Series<>();
        seriesSPO2 = new XYChart.Series<>();
        seriesScatter = new XYChart.Series<>();
        bisectScatter = new XYChart.Series<>();

        // Set Name for Series
        seriesHR.setName("Heart rate");
        seriesSPO2.setName("SPO2");
        seriesScatter.setName("Scatter heart rate");

        //set size of UpperBound of Chart
        double upperBound = ((NumberAxis) chart.getXAxis()).getUpperBound();
        ((NumberAxis) chart.getXAxis()).setUpperBound(/*upperBound / 50*/200);

        bisectScatter.getData().add(new XYChart.Data<>(400,400));
        bisectScatter.getData().add(new XYChart.Data<>(1500,1500));
        // Add Chart Series
        chart.getData().addAll(seriesHR, seriesSPO2);
        scatterChart.getData().addAll(seriesScatter);
        scatterChart.getData().addAll(bisectScatter);
    }

    //установка флага начала тестирования
    //прерывание режима проверки оборудования
    private void beforeTest() {

        isTesting = true;
        secondsForTest = MAX_TIME;
        haveBioLastResearch = false;
        haveHypoLastResearch = false;
        //summorizedLastResearch = new HashMap<>();
        checkReadyThread.interrupt();
        barChart.getData().clear();
        disableAll();

        System.err.println("Before test");
    }

    //установка флага завершения тестирвоания
    //установка заглушки для графика пульсовой волны
    //переход в режим проверки оборудования
    void afterTest() {
        if(secondsForTest!=0){
            badEndScreen.setVisible(true);
        }else {
            okEndScreen.setVisible(true);
        }
        isTesting = false;
        //equipmentService.setMockWavesValues();
        //startChecking();
        secondsForTest = MAX_TIME;

        System.err.println("After test");
    }

    private void drawOneScatterPoint(){
        double rr1 = 60.0/heartRateList.get(heartRateList.size()-1) * 1000;
        double rr2 = 60.0/heartRateList.get(heartRateList.size()/2) * 1000;

        System.out.println("["+rr1+", "+rr2+"]");

        final double rr1Random = ThreadLocalRandom.current().nextDouble(rr1-10,rr1+10+1);
        final double rr2Random = ThreadLocalRandom.current().nextDouble(rr2-10,rr2+10+1);
        System.out.println("Random -["+rr1Random+", "+rr2Random+"]");
        Platform.runLater(() -> {
            seriesScatter.getData().add(new XYChart.Data<>(rr1Random,rr2Random));
            heartRateList.clear();
        });
    }

    //отрисовка одного значения HR и SPO2 на графике гипоксии
    private void drawOneInspectionsHRnSPO2 (Inspections inspections, int counterPoints){

        seriesHR.getData().add(new XYChart.Data(counterPoints, inspections.getPulse()));
        seriesSPO2.getData().add(new XYChart.Data(counterPoints, inspections.getSpo2()));

        //увеличение правой границы графика гипоксии по оси Х
        double upperBound = ((NumberAxis) chart.getXAxis()).getUpperBound();
        if (counterPoints > 0.90 * upperBound) {
            ((NumberAxis) chart.getXAxis()).setUpperBound(upperBound * 1.33);
        }

        //отражения в полях HR и SPO2 текущих значений
        Platform.runLater(() -> {
            this.heartRateLabel.setText("" + inspections.getPulse());
            this.spo2Label.setText("" + inspections.getSpo2());
        });
    }

    private void initTimeline() {

        //в зависимости от режима тестирование/проверка оборудования
        //очистка графика пульсовой волны или установка заглушки
        if (isTesting){
            equipmentService.clearWavesValue();
        } else {
            //equipmentService.setMockWavesValues();
        }

        //отрисовка графика сердечного ритма, пульсовой волны
        timeline = new Timeline(new KeyFrame(Duration.millis(TIME_BETWEEN_FRAMES), event -> {
            drawPulseWave();
            count = (int) ((count + 1) % pulseWave.getWidth());
            if (count == 0) {
                heartRatePoints.clear();
                pulseWavePoints.clear();

                pulseIsRun = false;
            }

            //отражение значений в полях сенсоров рук и ног
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

            //отображение отсутствия значений в полях HR и SPO2
            PulseOximeterValue pulseoximeterValue = equipmentService.getLastPulseoximeterValue();
            if (pulseoximeterValue == null) {
                heartRateLabel.setText("--");
                spo2Label.setText("--");

                if (haveBioLastResearch && haveHypoLastResearch) {
                    okButton.setDisable(false);
                } else {
                    okButton.setDisable(true);
                }
            } else {
                /*heartRateLabel.setText(pulseoximeterValue.getHeartRate() != null ?
                                        pulseoximeterValue.getHeartRate().toString() : "--");

                spo2Label.setText(pulseoximeterValue.getSpo2() != null ?
                                    pulseoximeterValue.getSpo2().toString() : "--");*/

                if (!isTesting) {
                    startButton.setDisable(!(!heartRateLabel.getText().equals("0") && !spo2Label.getText().equals("0")
                            && pulseoximeterValue.getHeartRate() != null && pulseoximeterValue.getSpo2() != null
                        //&& equipmentService.isHandsReady() && equipmentService.isLegsReady()
                        && diastolicPressureProperty.get()> 0 && systolicPressureProperty.get() > 0));
                }
            }

            if (isTesting) {
                okButton.setDisable(true);
            } else if (!isTesting && haveBioLastResearch && haveHypoLastResearch) {
                okButton.setDisable(false);
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
        double centerY = pulseWave.getHeight() / 4;
        gc.beginPath();
        gc.moveTo(0, pulseWave.getHeight()/2);
        pulseWavePoints.add(equipmentService.getLastPulseoximeterValue().getWave());
        for (int i = 0; i < count; i++) { //count is equals to pulseWavePoints.size
            gc.lineTo(i, pulseWave.getHeight() - (centerY + pulseWavePoints.get(i)));
        }
        gc.stroke();
        gc.setStroke(Color.YELLOW);
        gc.strokeLine(count, 0, count, pulseWave.getHeight());
    }


    @FXML
    private void startTest(ActionEvent actionEvent) {

        beforeTest();

        //Занесение давления в репорт
        Inspection distBP = new Inspection();
        Inspection systBP = new Inspection();
        distBP.setName("distbp");
        distBP.setValue(diastolicPressureProperty.getValue());
        systBP.setName("systbp");
        systBP.setValue(systolicPressureProperty.getValue());
        summorizedLastResearch.put("distBP",distBP);
        summorizedLastResearch.put("systBP",systBP);
        //TODO

        //Bioimpedance test & show results
        StartTestBioImpedance startTestBioImpedance = new StartTestBioImpedance(this, portBioimpedance);
        startTestBioImpedance.setDaemon(true);
        startTestBioImpedance.start();

        Task<BioimpedanceValue> bioimpedance = new Task<BioimpedanceValue>() {
            @Override
            protected BioimpedanceValue call() throws Exception {
                //ожидание (3 сек) сбора LastRearch биоимпеданса
                int i=1;
                while (equipmentService.getBioimpedanceValue()==null) {
                    if (equipmentService.isBioimpedanceReady()) break;
                    Thread.sleep(1000);
                }
                if (equipmentService.getBioimpedanceValue()==null){
                    this.cancel(true);
                    afterTest();
                }
                return equipmentService.getBioimpedanceValue();
            }
        };
        bioimpedance.setOnSucceeded(event -> {
            try {
                BioimpedanceValue bioimpedanceValue = bioimpedance.get();

                if (bioimpedance != null) {
                    xAxis.setAnimated(false);
                    double fat = bioimpedanceValue.getFat();
                    double mm = bioimpedanceValue.getMuscle();
                    double tbw = bioimpedanceValue.getAllWater();
                    final XYChart.Data<String,Number> dataFat = new XYChart.Data(Math.round(fat*10.0)/10.0+" кг",fat);
                    final XYChart.Data<String,Number> dataMM = new XYChart.Data(Math.round(mm*10.0)/10.0+" кг ",mm);
                    final XYChart.Data<String,Number> dataWater = new XYChart.Data(Math.round(tbw*10.0)/10.0+" кг  ",tbw);
                    final XYChart.Data<String,Number> dataWeight = new XYChart.Data(getWeight()+" кг   ",getWeight());

                    seriesBio = new XYChart.Series();
                    seriesBio.getData().addAll(dataFat,dataMM,dataWater,dataWeight);
                    barChart.getData().addAll(seriesBio);

                    bioLegend.setVisible(true);
                    dataFat.getNode().setStyle("-fx-bar-fill:  #faa71b");
                    dataMM.getNode().setStyle("-fx-bar-fill: #f3622d");
                    dataWater.getNode().setStyle("-fx-bar-fill: #0087be");
                    dataWeight.getNode().setStyle("-fx-bar-fill: #36804d");


//                    barChart.getData().add(new PieChart.Data(String.format("Жировая масса \n%.2f%%", fat), fat));
//                    barChart.getData().add(new PieChart.Data(String.format("Мышечная масса \n%.2f%%", mm), mm));
//                    barChart.getData().add(new PieChart.Data(String.format("Вода \n%.2f%%", tbw), tbw));
                }
                Thread oxiWrapperThread = new Thread(() -> {
                    //для отображения "---" в полях сенсоров рук/ног
                    //equipmentService.setEquipmentReady(false);

                    testHypoxiaProgressBarTimer();
                    /*try {
                        Thread.sleep(2000); //Ждем пока progressBar дойдет до конца, drycode...
                    } catch (InterruptedException e) { // для формулы 7302/123 * seconds
                        e.printStackTrace();
                    }*/
                    afterTest();
                });
                oxiWrapperThread.setDaemon(true);
                oxiWrapperThread.start();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        bioimpedance.setOnCancelled(event -> {

        });

        Thread bioimpedanceThread = new Thread(bioimpedance);
        bioimpedanceThread.setDaemon(true);
        bioimpedanceThread.start();

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
                Command command = new StartTest();
                sendCommand(command, output);

                int counterPoints = 0;
                do {
                    String line = br.readLine();

                    if (line == null) break;
                    Data data = deserializeData(line);

                    if (data instanceof Inspections) {
                        Inspections inspections = (Inspections) data;
                        equipmentService.setLastPulseoximeterValue(inspections.getPulse(), inspections.getSpo2(), inspections.getWave());
                        if (counterPoints++ % NUMBER_OF_SKIP == 0) {
                            System.err.println(" HR: " + inspections.getPulse() + " SPO2: " + inspections.getSpo2() + " WAVE: " + inspections.getWave());
                            drawOneInspectionsHRnSPO2(inspections,  counterPoints);
                            heartRateList.add(inspections.getPulse());
                            if(heartRateList.size()>=20){
                                drawOneScatterPoint();
                            }
                        }
                    } else if (data instanceof LastResearch) {
                        //TODO
                        System.err.println(data);
                        System.err.println("Hypoxia LastrReaserch is ready");
                        summorizedLastResearch.putAll(((LastResearch) data).getInspections());
                        System.out.println(summorizedLastResearch.keySet());
                        haveHypoLastResearch = true;
                        return;
                    }

                } while(isTesting && !isStageClosed) ;

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

                Command command = new StartTest();
                sendCommand(command, output);

                do {
                    String line = br.readLine();

                    if (line == null) {
                        System.out.println("break from startTestBio");
                        break;
                    }
                    Data data = deserializeData(line);
                    if (data == null){
                        equipmentService.setBioimpedanceValue(null);//Если данные с биомпиданса не пришли
                        return;
                    }
                    if (data instanceof LastResearch) {
                        LastResearch lastResearch = (LastResearch) data;
                        double fat = lastResearch.getInspections().get("FAT").getValue();
                        double tbw = lastResearch.getInspections().get("TBW").getValue();
                        double mm = lastResearch.getInspections().get("MM").getValue();
                        equipmentService.setBioimpedanceValue(new BioimpedanceValue(fat, tbw, mm));

                        System.err.println("Bioimpedance LastResearch is ready");
                        System.err.println(lastResearch);
                        summorizedLastResearch.putAll(lastResearch.getInspections());
                        System.out.println(summorizedLastResearch.keySet());
                        haveBioLastResearch = true;
                        break;
                    }

                } while (isTesting && !isStageClosed);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void testHypoxiaProgressBarTimer () {

        //увеличить правой границы оси Х графика для гипоксии
        double upperBound = ((NumberAxis) chart.getXAxis()).getUpperBound();
        ((NumberAxis) chart.getXAxis()).setUpperBound(/*upperBound * 60*/8000);

        secondsForTest = MAX_TIME;
        progressBar.setProgress(0);
        timeLabel.setText(String.format("%d:%02d", secondsForTest / 60, secondsForTest % 60));

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsForTest--;
            timeLabel.setText(String.format("%d:%02d", secondsForTest / 60, secondsForTest % 60));
            double value = ((double) (MAX_TIME - secondsForTest)) / MAX_TIME;
            progressBar.setProgress(value);
        }));
        timer.setCycleCount(secondsForTest);
        timer.play();

        equipmentService.clearWavesValue();
        StartTestHypoxia startTestHypoxia = new StartTestHypoxia(this, portHypoxia);
        startTestHypoxia.setDaemon(true);
        startTestHypoxia.start();

        try {
            startTestHypoxia.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    private class TonometrData{
        int systP;
        int diastP;

        public TonometrData(int systP, int diastP){this.systP = systP; this.diastP = diastP;}
    }
    @FXML
    private void measurePressure(ActionEvent actionEvent) {
        if(comPort.equals("")){
            measureNewTonometr();
        }else {
            measureOldTonometr(comPort);
        }

    }

    private void measureNewTonometr() {
        measureNewTonometrTask = new Task<TonometrData>() {
            @Override
            protected TonometrData call() throws Exception {

                try (Socket socket = new Socket("localhost", 8084)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    //TODO
                    sendCommand(new Launch(getAge(), isMan(), getWeight(), getHeight(), getActivityLevel(), 0, 0, 0), output);
                    sendCommand(new CheckStatus(), output);


                    ReadyStatusTonometr readyStatusTonometr = (ReadyStatusTonometr) deserializeData(br.readLine());
                    boolean isReady = readyStatusTonometr.isReady();

                    if (isReady) {

                    } else {
                        System.err.println("Error: Cannot open Tonometr!");
                        return null;
                    }

                    sendCommand(new StartTest(), output);
                    while (true) {
                        if (this.isCancelled()){
                            System.err.println("Measure new Tonometr Interrupted");
                            return null;
                        }


                        String line = br.readLine();

                        if(line == null) continue;

                        Data data = deserializeData(line);
                        if (data instanceof InspectionsTonometr) {
                            InspectionsTonometr inspections = (InspectionsTonometr) data;
                            System.err.println(inspections);

                            if (inspections.getStatus() == 0) {
                                if(inspections.getErrors() == 0){
                                    sendCommand(new GetLastResearch(), output);
                                    LastResearch lastResearch = (LastResearch)deserializeData(br.readLine());
                                    System.err.println(lastResearch);

                                    int sbp = (int)lastResearch.getInspections().get("systBP").getValue();
                                    int dbp = (int)lastResearch.getInspections().get("distBP").getValue();

                                    return new TonometrData(sbp, dbp);
                                }
                                break;
                            }
                        } else {
                            break;
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            System.err.println("Testing is interrupted");
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;

            }
        };

        measureNewTonometrTask.setOnSucceeded(event -> {
            TonometrData data = measureNewTonometrTask.getValue();
            if (data != null && data.systP > 0 && data.systP < 255 && data.diastP > 0 && data.diastP < 255) {
                systolicPressureProperty.setValue(data.systP);
                diastolicPressureProperty.setValue(data.diastP);
                measureNewTonometrCount = 0;
            } else {
                if (measureNewTonometrCount<3) {
                    measureNewTonometrCount++;
                    System.err.println("Trying to re-measure pressure time="+measureNewTonometrCount);
                    measureNewTonometr();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Измерение давления");
                    alert.setHeaderText("Не удалось измерить давление");
                    alert.setContentText("К сожалению, не удалось измерить давление. Пожалуйста, повторите попытку.");
                    alert.showAndWait();
                    measureNewTonometrCount = 0;
                }
            }
            measurePressureButton.setDisable(false);
        });

        measurePressureButton.setDisable(true);
        final Thread th = new Thread(measureNewTonometrTask);
        th.setDaemon(true);
        th.start();
    }

    /* старый тонометр */

    public class PressureResult {
        boolean isOk;
        int sys, dia, aver, hr;
    }

    private void measureOldTonometr(String comPort) {

        Thread thread = new Thread(() -> {

            try {
                String[] cmd = {"./usb_switch.exe"};
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor();
                Thread.sleep(5000);
            } catch (Exception ex) {
                System.out.println("ERROR SWITCH EXE");
            }


//            ReadDataTon rd = new ReadDataTon(comPort);
            TonometrReader rd = new TonometrReader(comPort);
            PressureResult result = new PressureResult();



            rd.callPort();

            Task<Result> longTask = new Task<Result>() {
                @Override
                protected Result call() throws Exception {
                    try {
                        synchronized (rd) {
                            while (!rd.isReady()) {
                                rd.wait(1000);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return rd.getResult();
                }
            };

            final Thread th = new Thread(longTask);
            th.setDaemon(true);

            longTask.setOnSucceeded(event -> {
                try {
                    Result res = longTask.getValue();

                    if (res == Result.SUCCESS) {
                        // вывод результата
                        result.isOk = true;
                        systolicPressureProperty.setValue((int) rd.getPressure().getSys());
                        diastolicPressureProperty.setValue((int) rd.getPressure().getDia());
                    } else {
                        result.isOk = false;
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Измерение давления");
                        alert.setHeaderText("Не удалось измерить давление");
                        alert.setContentText("К сожалению, не удалось измерить давление. Пожалуйста, повторите попытку.");
                        alert.showAndWait();
                    }
                    measurePressureButton.setDisable(false);
                } finally {
                    th.interrupt();
                }
            });
            th.start();
        });
        thread.setDaemon(true);
        thread.start();
    }
    /* конец старый тонометр */

    private int distanceToNextPulse(int heartRate) {
        int intervalInSeconds = 1000/TIME_BETWEEN_FRAMES;
        double heartRateInSecond = heartRate / 60.0D;
        return (int) (intervalInSeconds / heartRateInSecond) - 15;
    }

    private JSONObject createJSON(Map<String, Inspection> inspections) {

        JSONObject jsonObject = new JSONObject();

        for (InspectionNames name : InspectionNames.values()) {

            JSONObject innerJSONObject = new JSONObject();
            Inspection inspection = inspections.get(name.toString());

            innerJSONObject.put("power", inspection.getValue());
            innerJSONObject.put("min", inspection.getMin());
            innerJSONObject.put("max", inspection.getMax());

//            if(inspection.getName().equalsIgnoreCase("RI")){
//                innerJSONObject.put("power", 30 + (int)(Math.random() * 15));
//            }else if(inspection.getName().equalsIgnoreCase("svr")){
//                innerJSONObject.put("power", 900 + (int)(Math.random() * 600));
//                innerJSONObject.put("min", 900.0);
//            }else if(inspection.getName().equalsIgnoreCase("sv")){
//                innerJSONObject.put("power", 60 + (int)(Math.random() * 40));
//            }

            jsonObject.put(name.toString().toLowerCase(), innerJSONObject);
        }

        return jsonObject;
    }

    private void writeJSON(Map<String, Inspection> inspections) {
        try {
            String jsonFileName = path.concat("base_output_file.json");
            BufferedWriter e = new BufferedWriter(new FileWriter(new File(jsonFileName)));
            Throwable var2 = null;

            try {
                e.write(this.createJSON(inspections).toString());
                System.err.println("JSON is written to " + jsonFileName);
            } catch (Throwable var12) {
                var2 = var12;
                throw var12;
            } finally {
                if(var2 != null) {
                    try {
                        e.close();
                    } catch (Throwable var11) {
                        var2.addSuppressed(var11);
                    }
                } else {
                    e.close();
                }
            }
        } catch (IOException var14) {
            var14.printStackTrace();
        }

    }

    private void changeBioKgToPercent(){
        String[] inspectionsToChange = {"FAT","MM","TBW","ECW","ICW"};
        double tbw = summorizedLastResearch.get("TBW").getValue();
        for (String inspection:inspectionsToChange){
            Inspection i = summorizedLastResearch.get(inspection);
            if(inspection.equals("FAT") || inspection.equals("MM") || inspection.equals("TBW")) {
                i.setValue(formatDouble(100 * i.getValue() / getWeight()));
                i.setMin(formatDouble(100 * i.getMin() / getWeight()));
                i.setMax(formatDouble(100 * i.getMax() / getWeight()));
            }else if (inspection.equals("ECW") || inspection.equals("ICW")){
                i.setValue(formatDouble(100 * i.getValue() / tbw));
                i.setMin(formatDouble(100 * i.getMin() / tbw));
                i.setMax(formatDouble(100 * i.getMax() / tbw));
            }
        }
    }

    private static double formatDouble(double value){
        return Math.floor(value * 100) / 100;
    }
}

//обозначения показателей, записываемых в результирующий JSON
enum InspectionNames {
    FAT, TBW, ECW, ICW, MM, STRESS, LF, HF, BIOAGE, systBP, distBP, HR, SVR, SV, RI, SI, SPO2
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
