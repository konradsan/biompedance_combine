package ru.kit.bioimpedance.equipment;

public class PulseOximeterValue {
    private final Integer heartRate;
    private final Integer spo2;
    private final Integer wave;

    public PulseOximeterValue(Integer heartRate, Integer spo2, Integer wave) {
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.wave = wave;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public Integer getSpo2() {
        return spo2;
    }

    public Integer getWave() {
        return wave;
    }
}
