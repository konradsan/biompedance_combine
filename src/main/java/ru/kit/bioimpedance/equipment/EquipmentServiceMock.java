package ru.kit.bioimpedance.equipment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EquipmentServiceMock implements EquipmentService {

    private boolean handsReady = false;
    private boolean legsReady = false;
    private boolean equipmentReady = false;
    private int index = 0;
    private int biompedanceCheck = 0;
    private List<PulseOximeterValue> waves = new ArrayList<>();
    private BioimpedanceValue bioimpedanceValue = null;

    @Override
    public PulseOximeterValue getLastPulseoximeterValue() {

        if (waves.size() == 0) {
            return new PulseOximeterValue(0, 0, 0);
        }
        else {
            return waves.size() > index ? waves.get(index++) : waves.get(waves.size() - 1);
        }
    }

    @Override
    public void setLastPulseoximeterValue(Integer heartRate, Integer spo2, Integer wave) {
        this.waves.add(new PulseOximeterValue(heartRate, spo2, wave));
    }

    @Override
    public void clearWavesValue() {
        this.waves = new ArrayList<>();
    }

    @Override
    public void setMockWavesValues() {
        try(BufferedReader br = new BufferedReader(new FileReader("oxi_wave.txt"))) {
            waves = new ArrayList<>();
            String line = br.readLine();
            while (line != null) {
                waves.add(new PulseOximeterValue(ThreadLocalRandom.current().nextInt(80, 100 + 1),
                        ThreadLocalRandom.current().nextInt(95, 100),
                        Integer.parseInt(line)));
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isHandsReady() {
        return this.handsReady;
    }

    @Override
    public boolean isLegsReady() {
        return this.legsReady;
    }

    @Override
    public boolean isBioimpedanceReady() {
        if (biompedanceCheck++ > 8) {
            return true;
        }
        return false;
    }

    @Override
    public BioimpedanceValue getBioimpedanceValue() {

        return this.bioimpedanceValue;
    }

    @Override
    public void setBioimpedanceValue(BioimpedanceValue bioimpedanceValue) {
        this.bioimpedanceValue = bioimpedanceValue;
    }

    public void setHandsReady(boolean handsReady) {
        this.handsReady = handsReady;
    }

    public void setLegsReady(boolean legsReady) {
        this.legsReady = legsReady;
    }

    public boolean isEquipmentReady() {
        return equipmentReady;
    }

    //библиотека подключена, пластины работают
    public void setEquipmentReady(boolean equipmentReady) {
        this.equipmentReady = equipmentReady;
    }


    //Мишин эквипмент-мок-сервис
    /*private final List<Boolean> handsReady = new ArrayList<>();
    private final List<Boolean> legsReady = new ArrayList<>();
    private List<PulseOximeterValue> waves = new ArrayList<>();
    private int index = 0;
    private int biompedanceCheck = 0;

    {
        for (int i = 0; i < 200; i++) {
            handsReady.add(false);
        }
        for (int i = 0; i < 1; i++) {
            handsReady.add(true);
        }

        for (int i = 0; i < 150; i++) {
            legsReady.add(false);
        }
        for (int i = 0; i < 1; i++) {
            legsReady.add(true);
        }

        try(BufferedReader br = new BufferedReader(new FileReader("oxi_wave.txt"))) {
            waves = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                waves.add(new PulseOximeterValue(ThreadLocalRandom.current().nextInt(80, 100 + 1),
                        ThreadLocalRandom.current().nextInt(95, 100), Integer.parseInt(line)));
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    synchronized public PulseOximeterValue getLastPulseoximeterValue() {
        return waves.size() > index ? waves.get(index++) : waves.get(waves.size() - 1);
    }

    @Override
    public boolean isHandsReady() {
        boolean result;
        if (handsReady.size() >= 1) {
            result = handsReady.get(0);
            handsReady.remove(0);
        } else {
            result = true;
        }
        return result;
    }

    @Override
    public boolean isLegsReady() {
        boolean result;
        if (legsReady.size() >= 1) {
            result = legsReady.get(0);
            legsReady.remove(0);
        } else {
            result = true;
        }
        return result;
    }

    @Override
    synchronized public boolean isBioimpedanceReady() {
        if (biompedanceCheck++ > 3) {
            return true;
        }
        return false;
    }

    @Override
    synchronized public BioimpedanceValue getBioimpedanceValue() {
        return new BioimpedanceValue(33.2, 15.2, 45.0);
    }*/
}
