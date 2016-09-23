package ru.kit.bioimpedance;

public enum CustomPointType {
    START(0), P(-7), PR_START(0), PR_FINISH(0), Q(9), R(-35), S(18), ST_START(0), ST_FINISH(0), T(-12), FINISH(0);

    private final int deltaY;

    CustomPointType(int deltaY) {
        this.deltaY = deltaY;
    }

    public int getDeltaY() {
        return deltaY;
    }
}
