package com.lacalandria.applacalandria;

public class ControlCercoRecord {
    public String tramo;
    public boolean roja_si;
    public boolean roja_no;
    public boolean verde_si;
    public boolean verde_no;
    public String timestamp;

    public ControlCercoRecord(String tramo, boolean roja_si, boolean roja_no, boolean verde_si, boolean verde_no, String timestamp) {
        this.tramo = tramo;
        this.roja_si = roja_si;
        this.roja_no = roja_no;
        this.verde_si = verde_si;
        this.verde_no = verde_no;
        this.timestamp = timestamp;
    }
}
