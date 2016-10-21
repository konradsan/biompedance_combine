package ru.kit.bioimpedance.dto;


public class InspectionsTonometr extends Data {
    private int status;
    private int errors;
    private int settings;
    private int cuff_p;
    private int syst_p;
    private int diast_p;

    public InspectionsTonometr(){}

    public InspectionsTonometr(int status, int errors, int settings, int cuff_p, int syst_p, int diast_p) {
        this.status = status;
        this.errors = errors;
        this.settings = settings;
        this.cuff_p = cuff_p;
        this.syst_p = syst_p;
        this.diast_p = diast_p;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getSettings() {
        return settings;
    }

    public void setSettings(int settings) {
        this.settings = settings;
    }

    public int getCuff_p() {
        return cuff_p;
    }

    public void setCuff_p(int cuff_p) {
        this.cuff_p = cuff_p;
    }

    public int getSyst_p() {
        return syst_p;
    }

    public void setSyst_p(int syst_p) {
        this.syst_p = syst_p;
    }

    public int getDiast_p() {
        return diast_p;
    }

    public void setDiast_p(int diast_p) {
        this.diast_p = diast_p;
    }

    @Override
    public String toString() {
        return "Status: " + status + ". Errors: " + errors + ". Pressure: " + cuff_p + ". Syst_p: " + syst_p + ". Diast_p: " + diast_p + "";
    }
}
