package com.lacalandria.applacalandria;

// Añadido: import explícito de R por si el archivo no está en la carpeta de paquete esperada

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.annotation.SuppressLint; // <-- agregado

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    // Credenciales hardcodeadas (ejemplo)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "1234";
    private static final String GUARD_USER = "guard";
    private static final String GUARD_PASS = "1234";

    // Copiado: constantes y prefs utilizadas para alarmas
    private static final boolean USE_UNSAFE_SSL = true; // ajustar según entorno
    private static final String PREFS = "control_cerco";
    private static final String KEY_ALARM_LIST_JSON = "alarms_json";
    private static final long MIN_WINDOW_MS = 60 * 1000L;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // nuevo layout con diseño tipo Home

        final EditText etUser = findViewById(R.id.etUser);
        final EditText etPass = findViewById(R.id.etPass);
        Button btnIngresar = findViewById(R.id.btnIngresar);

        // mantener coherencia visual con Home
        TextView tvHello = findViewById(R.id.tvHello);
        if (tvHello != null) tvHello.setText("La Calandria System Control");

        // Enlazar botones superiores (si querés que actúen desde login; aquí solo los ocultamos/desactivamos)
        Button topControl = findViewById(R.id.btnControlCerco);
        Button topCargar = findViewById(R.id.btnCargarContingencia);
        if (topControl != null) topControl.setVisibility(View.GONE);
        if (topCargar != null) topCargar.setVisibility(View.GONE);

        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String user = etUser.getText() != null ? etUser.getText().toString().trim() : "";
                String pass = etPass.getText() != null ? etPass.getText().toString() : "";

                if (user.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validación por credenciales (prioriza Admin)
                if (user.equals(ADMIN_USER) && pass.equals(ADMIN_PASS)) {
                    // Administrador -> primero cargar alarmas en background, luego abrir AdminActivity
                    fetchAlarmsFromServerAndSchedule(); // se ejecuta asíncrono
                    Intent i = new Intent(LoginActivity.this, AdminActivity.class);
                    startActivity(i);
                    finish();
                    return;
                }

                if (user.equals(GUARD_USER) && pass.equals(GUARD_PASS)) {
                    // Guardia -> cargar alarmas y abrir HomeActivity
                    fetchAlarmsFromServerAndSchedule();
                    Intent i = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(i);
                    finish();
                    return;
                }

                // Otras cuentas: comportamiento por defecto (rechazar)
                Toast.makeText(LoginActivity.this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --------------------------------------------------------------------------------
    // Métodos añadidos: descarga de alarmas y programación (copiados/adaptados desde Home)
    // --------------------------------------------------------------------------------
    private void fetchAlarmsFromServerAndSchedule() {
        Log.d(TAG, "Iniciando descarga de lista de alarmas desde servidor...");
        try {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client;
            if (USE_UNSAFE_SSL) {
                Log.w(TAG, "USANDO WORKAROUND TLS INSEGURO: se desactiva verificación de host (solo para pruebas)");
                client = getUnsafeOkHttpClient(logging);
            } else {
                client = new OkHttpClient.Builder().addInterceptor(logging).build();
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://www.vajillas.lovar.com.ar/") // ajustar según tu servidor
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService service = retrofit.create(ApiService.class);
            Call<ApiResponse> call = service.getAlarmConfig();
            Log.d(TAG, "Llamando endpoint get_alarm.php ...");
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        ApiResponse body = response.body();
                        List<AlarmConfig> alarms = body.alarms;
                        if (alarms == null) alarms = java.util.Collections.emptyList();
                        Log.i(TAG, "Recibidas " + alarms.size() + " alarmas desde servidor");

                        // serializar y guardar JSON en prefs para BootReceiver / AlarmReceiver
                        try {
                            JSONArray arr = new JSONArray();
                            for (AlarmConfig a : alarms) {
                                JSONObject o = new JSONObject();
                                o.put("id", a.id);
                                o.put("alarm_hour", a.alarm_hour);
                                o.put("alarm_min", a.alarm_min);
                                o.put("active", a.active);
                                o.put("label", a.label == null ? JSONObject.NULL : a.label);
                                arr.put(o);
                            }
                            getSharedPreferences(PREFS, MODE_PRIVATE)
                                    .edit()
                                    .putString(KEY_ALARM_LIST_JSON, arr.toString())
                                    .apply();
                            Log.d(TAG, "Lista de alarmas guardada en prefs");
                        } catch (Exception je) {
                            Log.e(TAG, "Error serializando alarms JSON", je);
                        }

                        // programar / cancelar según cada alarma (usar id como requestCode)
                        for (AlarmConfig a : alarms) {
                            if (a.active == 1) {
                                scheduleDailyAlarm(a.alarm_hour, a.alarm_min, a.id);
                            } else {
                                cancelExistingAlarm(a.id);
                            }
                        }
                    } else {
                        Log.w(TAG, "Respuesta inválida getAlarms: " + (response != null ? response.message() : "nula")
                                + " code=" + (response != null ? response.code() : -1));
                        // fallback: programar una alarma por defecto (09:00) usando requestCode 0
                        scheduleDailyAlarm(9, 0, 0);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.w(TAG, "Fallo al obtener lista de alarmas desde servidor: " + t.getClass().getSimpleName() + " - " + t.getMessage(), t);
                    // fallback
                    scheduleDailyAlarm(9, 0, 0);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando petición getAlarms: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            scheduleDailyAlarm(9, 0, 0);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleDailyAlarm(int hourOfDay, int minute, int requestCode) {
        try {
            cancelExistingAlarm(requestCode);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long triggerAt = calendar.getTimeInMillis();
            long now = System.currentTimeMillis();

            if (triggerAt <= now || (triggerAt - now) < MIN_WINDOW_MS) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                triggerAt = calendar.getTimeInMillis();
            }

            Intent intent = new Intent(LoginActivity.this, AlarmReceiver.class);
            intent.putExtra("alarm_id", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(LoginActivity.this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                    }
                    Log.i(TAG, "Alarm scheduled (id=" + requestCode + ") at " + triggerAt + " (" + String.format("%02d:%02d", hourOfDay, minute) + ")");
                } catch (IllegalArgumentException iae) {
                    Log.w(TAG, "setExact failed for id=" + requestCode + ", falling back to inexact repeating: " + iae.getMessage(), iae);
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, AlarmManager.INTERVAL_DAY, pendingIntent);
                } catch (Throwable t) {
                    Log.e(TAG, "Error programando alarma exacta id=" + requestCode + ", usando fallback inexact", t);
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, AlarmManager.INTERVAL_DAY, pendingIntent);
                }
            } else {
                Log.w(TAG, "AlarmManager es null; no se pudo programar la alarma id=" + requestCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al programar alarma id=" + requestCode, e);
        }
    }

    private void cancelExistingAlarm(int requestCode) {
        try {
            Intent intent = new Intent(LoginActivity.this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(LoginActivity.this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            try { pendingIntent.cancel(); } catch (Exception ignore) {}
            Log.d(TAG, "Cancelado alarm pendingIntent id=" + requestCode);
        } catch (Exception e) {
            Log.w(TAG, "Error cancelando alarma previa id=" + requestCode, e);
        }
    }

    private void cancelExistingAlarm() { cancelExistingAlarm(0); }

    private OkHttpClient getUnsafeOkHttpClient(HttpLoggingInterceptor logging) {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            X509TrustManager trustManager = (X509TrustManager)trustAllCerts[0];

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .hostnameVerifier((hostname, session) -> true)
                    .addInterceptor(logging);

            builder.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            builder.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS);

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error creando unsafe OkHttpClient", e);
            return new OkHttpClient.Builder().addInterceptor(logging).build();
        }
    }

    // ...existing code...
}
