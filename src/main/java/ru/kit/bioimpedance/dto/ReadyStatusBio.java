package ru.kit.bioimpedance.dto;

public class ReadyStatusBio extends Data {

    private boolean isHands;
    private boolean isLegs;
    private boolean isError;

    public ReadyStatusBio(boolean isHands, boolean isLegs, boolean isError) {

        this.isHands = isHands;
        this.isLegs = isLegs;
        this.isError = isError;
    }

    public ReadyStatusBio() {
    }


    @Override
    public String toString() {
        return "Hands: " + isHands + " Legs: " + isLegs + " isError " + isError;
    }

    public boolean isHands() {
        return isHands;
    }

    public void setHands(boolean hands) {
        isHands = hands;
    }

    public boolean isLegs() {
        return isLegs;
    }

    public void setLegs(boolean legs) {
        isLegs = legs;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }
}
