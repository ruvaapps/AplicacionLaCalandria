package com.lacalandria.applacalandria;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import java.util.Calendar;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Añadidos para workaround TLS (INSEGURO) - solo para pruebas
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import org.json.JSONArray;
import org.json.JSONObject;

// Nuevos imports usados para la barra de tabs y layout dinamico
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.view.ViewParent;
import android.util.DisplayMetrics;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    // Flag para activar/desactivar workaround TLS (INSEGURO). Usar solo en pruebas.
    private static final boolean USE_UNSAFE_SSL = true;

    // Nuevas constantes para prefs (coinciden con AlarmReceiver)
    private static final String PREFS = "control_cerco";
    private static final String KEY_ALARM_LIST_JSON = "alarms_json"; // guardamos lista completa

    // Nuevo: mínimo margen aceptable entre ahora y el disparo (evita "short window" errors)
    private static final long MIN_WINDOW_MS = 60 * 1000L; // 1 minuto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView tvHello = findViewById(R.id.tvHello);
        if (tvHello != null) {
            tvHello.setText("La Calandria System Control");
        }

        // -----------------------------------------------------------------------
        // Reemplazo: en lugar de crear dinámicamente tabs, enlazamos los botones
        // definidos en el layout (top row).
        // -----------------------------------------------------------------------
        try {
            Button tabControlBtn = findViewById(R.id.btnControlCerco);
            Button tabCargarBtn = findViewById(R.id.btnCargarContingencia);

            if (tabControlBtn != null) {
                tabControlBtn.setOnClickListener(v -> {
                    try {
                        Intent i = new Intent(HomeActivity.this, ControlCercoActivity.class);
                        startActivity(i);
                    } catch (Throwable t) {
                        Log.e(TAG, "Error al abrir ControlCercoActivity", t);
                        Toast.makeText(HomeActivity.this, "No se pudo abrir Control Cerco", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (tabCargarBtn != null) {
                tabCargarBtn.setOnClickListener(v -> {
                    Toast.makeText(HomeActivity.this, "Cargar Contingencia seleccionado", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudieron enlazar botones superiores", e);
        }

        // -----------------------------------------------------------------------
        // Estilo y comportamiento del botón Volver: respetar el fondo definido en XML
        // -----------------------------------------------------------------------
        try {
            Button btnVolver = findViewById(R.id.btnVolver);
            if (btnVolver != null) {
                // asegurar apariencia consistente: quitar la sobrescritura de fondo blanco previa
                btnVolver.setText("←");
                btnVolver.setTextSize(28f);
                btnVolver.setAllCaps(false);
                btnVolver.setContentDescription("Volver");
                // mantenemos el background definido en XML (no reasignar a white)
                btnVolver.setElevation(0f); // sin sombra para que no se note
                int pad = (int) (8 * getResources().getDisplayMetrics().density);
                btnVolver.setPadding(pad, pad, pad, pad);

                // listener: volver al Login y limpiar pila
                btnVolver.setOnClickListener(v -> {
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo ajustar btnVolver", e);
        }

        // Programar/descargar la lista de alarmas al iniciar la actividad
        // La carga de alarmas ahora se realiza desde LoginActivity tras el login,
        // por lo que evitamos duplicar la llamada aquí.
    }

    // Nuevo: descarga la lista completa de alarmas, guarda JSON y programa cada una
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
            Call<ApiResponse> call = service.getAlarmConfig(); // endpoint get_alarm.php devuelve alarms array
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

    // Sobrecarga: scheduleDailyAlarm que permite especificar requestCode para múltiples alarmas
    @SuppressLint("ScheduleExactAlarm")
    private void scheduleDailyAlarm(int hourOfDay, int minute, int requestCode) {
        try {
            // cancelar previa específica
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

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("alarm_id", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                                triggerAt,
                                pendingIntent);
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                                triggerAt,
                                pendingIntent);
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

    // Cancela la alarma previamente programada (si existe) para un requestCode específico
    private void cancelExistingAlarm(int requestCode) {
        try {
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
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

    // mantener el método sin parámetro para compatibilidad (cancelar id=0)
    private void cancelExistingAlarm() { cancelExistingAlarm(0); }

    // Workaround helper: construye OkHttpClient que confía en todos los certificados y acepta cualquier hostname.
    // ADVERTENCIA: inseguro. Usar solo para pruebas si no podés corregir el certificado en el servidor.
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
}
