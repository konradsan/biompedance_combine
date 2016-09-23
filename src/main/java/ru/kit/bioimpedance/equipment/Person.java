package ru.kit.bioimpedance.equipment;

public class Person {
    private final double weight;
    private final int height;

    public Person(double weight, int height) {
        this.weight = weight;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public double getWeight() {
        return weight;
    }
}
