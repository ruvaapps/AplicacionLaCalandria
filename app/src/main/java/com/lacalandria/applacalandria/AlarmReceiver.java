package com.lacalandria.applacalandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import androidx.core.app.NotificationCompat;
import android.app.PendingIntent;
import android.os.Build;
import android.app.AlarmManager;
import android.content.SharedPreferences;
import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "control_cerco_channel";
    private static final int NOTIF_ID = 1001;
    private static final String PREFS = "control_cerco";
    private static final String KEY_ALARM_LIST_JSON = "alarms_json";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        // Intent para abrir HomeActivity al pulsar la notificación
        Intent i = new Intent(context, HomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Control Cercos")
                .setContentText("Es hora de controlar los cercos")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, builder.build());
        }

        // Reprogramar para el siguiente día solo la alarma que disparó
        try {
            int alarmId = intent != null ? intent.getIntExtra("alarm_id", 0) : 0;

            // Obtener hora/min desde SharedPreferences (lista JSON)
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_ALARM_LIST_JSON, null);
            int hour = -1, minute = -1;
            if (json != null) {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int idx = 0; idx < arr.length(); idx++) {
                    org.json.JSONObject o = arr.getJSONObject(idx);
                    int id = o.optInt("id", -1);
                    if (id == alarmId) {
                        hour = o.optInt("alarm_hour", -1);
                        minute = o.optInt("alarm_min", -1);
                        break;
                    }
                }
            }

            // Si no encontramos en la lista, usar fallback (09:00)
            if (hour < 0 || minute < 0) {
                SharedPreferences legacy = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                hour = legacy.getInt("alarm_hour", 9);
                minute = legacy.getInt("alarm_minute", 0);
            }

            // calcular siguiente día
            Calendar next = Calendar.getInstance();
            next.add(Calendar.DAY_OF_YEAR, 1);
            next.set(Calendar.HOUR_OF_DAY, hour);
            next.set(Calendar.MINUTE, minute);
            next.set(Calendar.SECOND, 0);
            next.set(Calendar.MILLISECOND, 0);

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            alarmIntent.putExtra("alarm_id", alarmId);
            PendingIntent pi = PendingIntent.getBroadcast(context, alarmId, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
                }
            }
        } catch (Exception e) {
            // si falla la reprogramación, no hacer nada crítico; se puede loggear si querés
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Control Cercos";
            String description = "Canal para recordatorios diarios de control de cercos";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
