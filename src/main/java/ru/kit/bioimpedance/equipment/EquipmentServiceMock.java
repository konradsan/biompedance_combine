package ru.kit.bioimpedance.equipment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EquipmentServiceMock implements EquipmentService {

    private final List<Boolean> handsReady = new ArrayList<>();
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
    }
}
