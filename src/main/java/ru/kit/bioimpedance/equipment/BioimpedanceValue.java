package ru.kit.bioimpedance.equipment;

public class BioimpedanceValue {
    private final Double fat;
    private final Double allWater;
    private final Double muscle;

    public BioimpedanceValue(Double fat, Double allWater, Double muscle) {
        this.fat = fat;
        this.allWater = allWater;
        this.muscle = muscle;
    }

    public Double getFat() {
        return fat;
    }

    public Double getAllWater() {
        return allWater;
    }

    public Double getMuscle() {
        return muscle;
    }
}
