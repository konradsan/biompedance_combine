package ru.kit.bioimpedance;

public class CustomPoint {
    private final int x;
    private final CustomPointType type;

    public CustomPoint(int x, CustomPointType type) {
        this.x = x;
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public CustomPointType getType() {
        return type;
    }
}
