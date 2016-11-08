package ru.kit.bioimpedance.equipment;

public interface EquipmentService {
    PulseOximeterValue getLastPulseoximeterValue();

    boolean isHandsReady();

    boolean isLegsReady();

    boolean isBioimpedanceReady();

    BioimpedanceValue getBioimpedanceValue();

    //методы, добавленные к Мишиным
    public void setBioimpedanceValue(BioimpedanceValue bioimpedanceValue);

    public void setHandsReady(boolean handsReady);

    public void setLegsReady(boolean legsReady);

    public boolean isEquipmentReady();

    public void setEquipmentReady(boolean equipmentReady);

    public void setLastPulseoximeterValue(Integer heartRate, Integer spo2, Integer wave);

    public void clearWavesValue();

    public void setMockWavesValues();


}
