package com.lacalandria.applacalandria;

import java.util.List;

public class ApiResponse {
    public boolean success;
    public String message;

    // Campos legacy (opcional)
    public int alarm_hour;
    public int alarm_min;

    // Nuevo: lista de alarmas (mapeo de get_alarm.php)
    public int count;
    public List<AlarmConfig> alarms;
}
