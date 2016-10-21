package ru.kit.bioimpedance.dto;

public class ReadyStatusTonometr extends Data{

    private boolean isReady;

    public ReadyStatusTonometr(boolean isReady) {
        this.isReady = isReady;
    }

    public ReadyStatusTonometr() {
    }


    public boolean isReady() {
        return isReady;
    }


    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    @Override
    public String toString() {
        return "Ready: " + isReady();
    }
}
