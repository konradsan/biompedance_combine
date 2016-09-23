package ru.kit.bioimpedance.equipment;

public interface EquipmentService {
    PulseOximeterValue getLastPulseoximeterValue();

    boolean isHandsReady();

    boolean isLegsReady();

    boolean isBioimpedanceReady();

    BioimpedanceValue getBioimpedanceValue();
}
