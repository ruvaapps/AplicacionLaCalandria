package com.lacalandria.applacalandria;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREFS = "control_cerco";
    private static final String KEY_ALARM_LIST_JSON = "alarms_json";
    // evitar ventana muy corta (1 minuto)
    private static final long MIN_WINDOW_MS = 60 * 1000L;

    @SuppressLint("ScheduleExactAlarm")
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent != null && intent.getAction() != null ? intent.getAction() : "";
            Log.i(TAG, "onReceive action=" + action);

            if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !"android.intent.action.MY_PACKAGE_REPLACED".equals(action)) {
                Log.d(TAG, "Acción no manejada: " + action);
                return;
            }

            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_ALARM_LIST_JSON, null);
            if (json == null) {
                Log.i(TAG, "No hay lista de alarmas guardada en prefs; nothing to schedule");
                return;
            }

            JSONArray arr = new JSONArray(json);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int id = o.optInt("id", -1);
                int hour = o.optInt("alarm_hour", 9);
                int minute = o.optInt("alarm_min", 0);
                int active = o.optInt("active", 0);

                if (id < 0) continue;

                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                alarmIntent.putExtra("alarm_id", id);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (active == 1 && alarmManager != null) {
                    // calcular trigger
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

                    long triggerAt = calendar.getTimeInMillis();
                    long now = System.currentTimeMillis();
                    if (triggerAt <= now || (triggerAt - now) < MIN_WINDOW_MS) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                        triggerAt = calendar.getTimeInMillis();
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                        }
                        Log.i(TAG, "Reprogramada alarma id=" + id + " para " + String.format("%02d:%02d", hour, minute));
                    } catch (Exception ex) {
                        Log.w(TAG, "Fallo programando alarma id=" + id + ", usando inexact repeating", ex);
                        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
                    }
                } else {
                    // cancelar si existe
                    try {
                        if (alarmManager != null) {
                            alarmManager.cancel(pi);
                        }
                        try { pi.cancel(); } catch (Exception ignore) {}
                        Log.d(TAG, "Alarma id=" + id + " cancelada (active=" + active + ")");
                    } catch (Exception ex) {
                        Log.w(TAG, "Error cancelando alarma id=" + id, ex);
                    }
                }
            }

        } catch (Throwable t) {
            Log.e(TAG, "Excepción en BootReceiver.onReceive", t);
        }
    }
}
