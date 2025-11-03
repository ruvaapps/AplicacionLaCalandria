package com.lacalandria.applacalandria;

public class AlarmConfig {
    public int id;
    public int alarm_hour;
    public int alarm_min;
    public int active; // 1 = activa, 0 = inactiva
    public String label;

    // Constructor vacío requerido por Gson/Retrofit
    public AlarmConfig() {}

    @Override
    public String toString() {
        return "AlarmConfig{id=" + id +
                ", alarm_hour=" + alarm_hour +
                ", alarm_min=" + alarm_min +
                ", active=" + active +
                ", label='" + label + '\'' +
                '}';
    }
}
