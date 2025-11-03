package com.lacalandria.applacalandria;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView; // <-- agregado
import android.content.SharedPreferences;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date; // <-- agregado

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

    private static final String TAG = "AdminActivity";
    private static final String PREFS = "control_cerco";
    private static final String KEY_ALARM_LIST_JSON = "alarms_json"; // coincide con HomeActivity

    // Ajustá la baseUrl según tu servidor (debe terminar con /)
    private static final String BASE_URL = "http://www.vajillas.lovar.com.ar/";

    // Nuevo: formato y valor de fecha seleccionada (null = todas)
    private String selectedDate = null;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private ScrollView svRecords; // <-- nuevo campo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // --- NUEVO: coherencia visual con Home/Login ---
        TextView tvHelloTitle = findViewById(R.id.tvHello);
        if (tvHelloTitle != null) tvHelloTitle.setText("La Calandria System Control");

        // Cambiado: obtener referencia al ScrollView que envuelve llRecords
        final LinearLayout llRecords = findViewById(R.id.llRecords);
        svRecords = findViewById(R.id.svRecords); // <-- enlazado

        Button btnVolver = findViewById(R.id.btnVolverAdmin);
        Button btnConsultar = findViewById(R.id.btnConsultar);
        Button btnConsultarAlarmas = findViewById(R.id.btnConsultarAlarmas); // <-- nuevo

        // Nuevos views para fecha (ya presentes en layout)
        Button btnSelectDate = findViewById(R.id.btnSelectDate);
        TextView tvSelectedDate = findViewById(R.id.tvSelectedDate);
        Button btnClearDate = findViewById(R.id.btnClearDate);

        // Cargar último registro guardado localmente (mantener info previa)
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String tramo = prefs.getString("tramo", "(sin datos)");
        boolean rojaSi = prefs.getBoolean("roja_si", false);
        boolean rojaNo = prefs.getBoolean("roja_no", false);
        boolean verdeSi = prefs.getBoolean("verde_si", false);
        boolean verdeNo = prefs.getBoolean("verde_no", false);
        String timestamp = prefs.getString("timestamp", "(sin timestamp)");

        StringBuilder sb = new StringBuilder();
        sb.append("Último registro local:\n\n");
        sb.append("Timestamp: ").append(formatTimestampDisplay(timestamp)).append("\n"); // { changed }
        sb.append("Tramo: ").append(tramo).append("\n\n");
        sb.append("Luz Electricidad Roja: ").append(rojaSi ? "Sí" : (rojaNo ? "No" : "—")).append("\n");
        sb.append("Luz Verde Titila: ").append(verdeSi ? "Sí" : (verdeNo ? "No" : "—")).append("\n");

        // Mostrar el registro local como una fila coloreada según condiciones
        llRecords.removeAllViews();
        boolean localIsBad = (rojaNo) || (verdeNo);
        addRecordView(llRecords, sb.toString(), localIsBad);

        // Selección de fecha: mostrar DatePickerDialog
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final Calendar c = Calendar.getInstance();
                    int y = c.get(Calendar.YEAR);
                    int m = c.get(Calendar.MONTH);
                    int d = c.get(Calendar.DAY_OF_MONTH);
                    android.app.DatePickerDialog dp = new android.app.DatePickerDialog(AdminActivity.this,
                            (view, year, month, dayOfMonth) -> {
                                Calendar sel = Calendar.getInstance();
                                sel.set(Calendar.YEAR, year);
                                sel.set(Calendar.MONTH, month);
                                sel.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                selectedDate = sdf.format(sel.getTime());
                                tvSelectedDate.setText(selectedDate);
                            }, y, m, d);
                    dp.show();
                } catch (Exception e) {
                    Log.w(TAG, "Error mostrando DatePicker", e);
                    Toast.makeText(AdminActivity.this, "No se pudo seleccionar fecha", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Limpiar selección
        btnClearDate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                selectedDate = null;
                tvSelectedDate.setText("Todas");
            }
        });

        // Botón Consultar: recupera registros desde servidor y los muestra
        btnConsultar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // mostrar estado
                llRecords.removeAllViews();
                addRecordView(llRecords, "Consultando registros en el servidor...", false);
                fetchControlsFromServer(llRecords, selectedDate);
            }
        });

        // Botón nuevo: mostrar alarmas programadas guardadas en prefs
        if (btnConsultarAlarmas != null) {
            btnConsultarAlarmas.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    llRecords.removeAllViews();
                    addGroupHeader(llRecords, "Alarmas programadas");
                    displayScheduledAlarms(llRecords);
                }
            });
        }

        // Volver al Login y limpiar la pila para evitar volver con back
        if (btnVolver != null) {
            btnVolver.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    // Interfaz Retrofit: devolvemos ResponseBody para parseo manual (evitamos error Gson). Acepta optional date query.
    interface ControlsApi {
        @GET("get_controls.php")
        Call<ResponseBody> getControls(@Query("date") String date); // date = yyyy-MM-dd (opcional)
    }

    // Nuevo: representación de un slot de alarma
    static class AlarmSlot {
        int id;
        int hour;
        int minute;
        String label;
        AlarmSlot(int id, int hour, int minute, String label) {
            this.id = id; this.hour = hour; this.minute = minute; this.label = label;
        }
        int minutesOfDay() { return hour * 60 + minute; }
        String displayLabel() {
            String hh = String.format("%02d:%02d", hour, minute);
            return (label == null || label.isEmpty()) ? hh : (label + " (" + hh + ")");
        }
    }

    // Reemplaza la implementación antigua por esta que agrupa por slots
    private void fetchControlsFromServer(final LinearLayout llRecords, String date) {
        try {
            Log.d(TAG, "Iniciando consulta controls. date=" + (date==null ? "ALL" : date));
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ControlsApi api = retrofit.create(ControlsApi.class);
            Call<ResponseBody> call = api.getControls(date);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String bodyStr = response.body().string();
                            JSONObject root = new JSONObject(bodyStr);
                            boolean success = root.optBoolean("success", false);
                            if (!success) {
                                String msg = root.optString("message", "Respuesta success=false");
                                llRecords.removeAllViews();
                                addRecordView(llRecords, "Servidor respondió error: " + msg, true);
                                Log.w(TAG, "get_controls.php success=false: " + msg);
                                return;
                            }
                            JSONArray data = root.optJSONArray("data");
                            llRecords.removeAllViews();
                            if (data == null || data.length() == 0) {
                                addRecordView(llRecords, "No hay registros en el servidor para la fecha seleccionada.", false);
                                return;
                            }

                            // 1) Parsear alarmas desde prefs y generar lista ordenada de slots
                            java.util.List<AlarmSlot> slots = parseAlarmsFromPrefs();
                            Collections.sort(slots, Comparator.comparingInt(AlarmSlot::minutesOfDay));

                            if (slots.isEmpty()) {
                                // Si no hay slots, mostrar lista plana
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject r = data.getJSONObject(i);
                                    String timestamp = r.optString("timestamp", "-");
                                    String tramo = r.optString("tramo", "-");
                                    int roja_si = r.optInt("roja_si", 0);
                                    int roja_no = r.optInt("roja_no", 0);
                                    int verde_si = r.optInt("verde_si", 0);
                                    int verde_no = r.optInt("verde_no", 0);

                                    StringBuilder out = new StringBuilder();
                                    out.append("Timestamp: ").append(formatTimestampDisplay(timestamp)).append("\n"); // { changed }
                                    out.append("Tramo: ").append(tramo).append("\n");
                                    out.append("Roja: ").append(roja_si == 1 ? "Sí" : (roja_no == 1 ? "No" : "—")).append("\n");
                                    out.append("Verde: ").append(verde_si == 1 ? "Sí" : (verde_no == 1 ? "No" : "—")).append("\n");

                                    boolean isBad = (roja_no == 1) || (verde_no == 1);

                                    addRecordView(llRecords, out.toString(), isBad);
                                }
                                return;
                            }

                            // 2) Preparar grupos por slot label
                            // Map slotLabel -> list of record strings
                            Map<String, java.util.List<String>> groups = new HashMap<>();
                            for (AlarmSlot s : slots) groups.put(s.displayLabel(), new ArrayList<>());

                            // Extra group for records outside slots (optional)
                            final String OUT_OF_RANGE = "Fuera de rango";
                            groups.put(OUT_OF_RANGE, new ArrayList<>());

                            // 3) Para cada registro, asignar al slot correspondiente según hora
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject r = data.getJSONObject(i);
                                String timestamp = r.optString("timestamp", "-");
                                String tramo = r.optString("tramo", "-");
                                int roja_si = r.optInt("roja_si", 0);
                                int roja_no = r.optInt("roja_no", 0);
                                int verde_si = r.optInt("verde_si", 0);
                                int verde_no = r.optInt("verde_no", 0);

                                StringBuilder out = new StringBuilder();
                                out.append("Timestamp: ").append(formatTimestampDisplay(timestamp)).append("\n"); // { changed }
                                out.append("Tramo: ").append(tramo).append("\n");
                                out.append("Roja: ").append(roja_si == 1 ? "Sí" : (roja_no == 1 ? "No" : "—")).append("\n");
                                out.append("Verde: ").append(verde_si == 1 ? "Sí" : (verde_no == 1 ? "No" : "—")).append("\n");

                                boolean isBad = (roja_no == 1) || (verde_no == 1);

                                // calcular minutos del día del timestamp
                                int minutes = getMinutesFromTimestamp(timestamp);
                                if (minutes < 0) {
                                    groups.get(OUT_OF_RANGE).add(out.toString() + "|bad:" + isBad);
                                    continue;
                                }

                                // encontrar slot: start <= minutes < next_start (wrap-around last->first)
                                boolean assigned = false;
                                for (int sidx = 0; sidx < slots.size(); sidx++) {
                                    AlarmSlot s = slots.get(sidx);
                                    AlarmSlot next = slots.get((sidx + 1) % slots.size());
                                    int start = s.minutesOfDay();
                                    int end = next.minutesOfDay();
                                    boolean inRange;
                                    if (start < end) {
                                        inRange = (minutes >= start && minutes < end);
                                    } else {
                                        // wrap to next day
                                        inRange = (minutes >= start) || (minutes < end);
                                    }
                                    if (inRange) {
                                        groups.get(s.displayLabel()).add(out.toString() + "|bad:" + isBad);
                                        assigned = true;
                                        break;
                                    }
                                }
                                if (!assigned) {
                                    groups.get(OUT_OF_RANGE).add(out.toString() + "|bad:" + isBad);
                                }
                            }

                            // 4) Renderizar: para cada slot en orden mostrar header + registros
                            for (AlarmSlot s : slots) {
                                String header = s.displayLabel();
                                java.util.List<String> recs = groups.get(header);
                                // Añadimos siempre el header aunque recs sea vacío
                                addGroupHeader(llRecords, header);
                                if (recs == null || recs.isEmpty()) {
                                    // mostrar texto indicativo cuando no hay registros en ese rango
                                    addRecordView(llRecords, "No hay registros en este rango.", false);
                                } else {
                                    for (String rec : recs) {
                                        boolean isBad = rec.endsWith("|bad:true") || rec.contains("|bad:true");
                                        String txt = rec.replaceAll("\\|bad:(true|false)$", "");
                                        addRecordView(llRecords, txt, isBad);
                                    }
                                }
                            }
                             // Mostrar fuera de rango al final si hay
                             java.util.List<String> outside = groups.get(OUT_OF_RANGE);
                             if (outside != null && !outside.isEmpty()) {
                                 addGroupHeader(llRecords, OUT_OF_RANGE);
                                 for (String rec : outside) {
                                     boolean isBad = rec.endsWith("|bad:true") || rec.contains("|bad:true");
                                     String txt = rec.replaceAll("\\|bad:(true|false)$", "");
                                     addRecordView(llRecords, txt, isBad);
                                 }
                             }

                        } else {
                            String msg = "Error en respuesta servidor: code=" + (response != null ? response.code() : -1);
                            Log.w(TAG, msg);
                            llRecords.removeAllViews();
                            addRecordView(llRecords, "Error consultando servidor.\n" + msg, true);
                        }
                    } catch (Exception ex) {
                        Log.w(TAG, "Fallo al parsear respuesta servidor: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(), ex);
                        llRecords.removeAllViews();
                        addRecordView(llRecords, "Fallo al parsear datos del servidor: " + ex.getClass().getSimpleName(), true);
                    } finally {
                        try { if (response != null && response.body() != null) response.body().close(); } catch (Exception ignore) {}
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.w(TAG, "Fallo al consultar servidor: " + t.getClass().getSimpleName() + " - " + t.getMessage(), t);
                    llRecords.removeAllViews();
                    addRecordView(llRecords, "Fallo al consultar el servidor: " + t.getClass().getSimpleName(), true);
                    Toast.makeText(AdminActivity.this, "Fallo red: " + t.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando consulta controls", e);
            llRecords.removeAllViews();
            addRecordView(llRecords, "Error iniciando consulta: " + e.getClass().getSimpleName(), true);
        }
    }

    // Helper: parsear lista de alarmas guardada en prefs (KEY_ALARM_LIST_JSON)
    private java.util.List<AlarmSlot> parseAlarmsFromPrefs() {
        java.util.List<AlarmSlot> list = new ArrayList<>();
        try {
            String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ALARM_LIST_JSON, null);
            if (json == null || json.trim().isEmpty()) return list;
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int id = o.optInt("id", i);
                int hour = o.optInt("alarm_hour", 9);
                int min = o.optInt("alarm_min", 0);
                String label = o.optString("label", null);
                list.add(new AlarmSlot(id, hour, min, label));
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo parsear alarms from prefs: " + e.getMessage(), e);
        }
        return list;
    }

    // Helper: extrae minutos desde timestamp "yyyy-MM-dd HH:mm:ss", devuelve -1 si no parsea
    private int getMinutesFromTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.length() < 13) return -1;
            // simple split to fast parse: "YYYY-MM-DD HH:MM:SS"
            String[] parts = timestamp.split(" ");
            if (parts.length < 2) return -1;
            String timePart = parts[1];
            String[] hhmm = timePart.split(":");
            if (hhmm.length < 2) return -1;
            int hh = Integer.parseInt(hhmm[0]);
            int mm = Integer.parseInt(hhmm[1]);
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) return -1;
            return hh * 60 + mm;
        } catch (Exception e) {
            Log.w(TAG, "getMinutesFromTimestamp parse error: " + e.getMessage());
            return -1;
        }
    }

    // Helper: añade un encabezado de grupo visualmente diferenciado
    private void addGroupHeader(LinearLayout container, String title) {
        try {
            TextView tv = new TextView(this);
            tv.setText(title);
            tv.setTextSize(14f);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            int pad = (int) (10 * getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad);
            tv.setBackgroundColor(android.graphics.Color.parseColor("#1565C0")); // azul oscuro para header
            tv.setTextColor(android.graphics.Color.WHITE);

            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = (int) (8 * getResources().getDisplayMetrics().density);
            lp.setMargins(0, margin, 0, 0);
            tv.setLayoutParams(lp);

            container.addView(tv);
        } catch (Exception e) {
            Log.w(TAG, "Error creando header de grupo", e);
        }
    }

    // Helper: agrega una fila (TextView) al LinearLayout con fondo rojo o verde según isBad
    private void addRecordView(LinearLayout container, String text, boolean isBad) {
        try {
            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextSize(16f);
            int pad = (int) (12 * getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad);

            int bgColor = isBad ? Color.parseColor("#D84315") /*rojo oscuro*/ : Color.parseColor("#C8E6C9") /*verde claro*/;
            int textColor = isBad ? Color.WHITE : Color.BLACK;
            tv.setBackgroundColor(bgColor);
            tv.setTextColor(textColor);

            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = (int) (8 * getResources().getDisplayMetrics().density);
            lp.setMargins(0, margin, 0, 0);
            tv.setLayoutParams(lp);

            container.addView(tv);

            // Si existe el ScrollView, desplazar al final para mostrar la fila recién añadida
            if (svRecords != null) {
                svRecords.post(new Runnable() {
                    @Override
                    public void run() {
                        svRecords.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "Error creando vista de registro", e);
        }
    }

    // { new helper: formatea timestamp a "dd/MM/yyyy HH:mm" }
    private String formatTimestampDisplay(String timestamp) {
        if (timestamp == null) return "-";
        timestamp = timestamp.trim();
        if (timestamp.isEmpty()) return "-";
        // Intentar parsear formatos comunes: "yyyy-MM-dd HH:mm:ss" preferentemente
        final String[] patternsIn = { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm" };
        final SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        for (String p : patternsIn) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(p, Locale.getDefault());
                Date d = in.parse(timestamp);
                if (d != null) return outFmt.format(d);
            } catch (Exception ignore) { /* try next */ }
        }
        // fallback: intentar extraer tokens si viene otro formato, por ejemplo "YYYY-MM-DD HH:MM:SS"
        try {
            String[] parts = timestamp.split(" ");
            if (parts.length >= 2) {
                String datePart = parts[0];
                String timePart = parts[1];
                String[] dateTokens = datePart.split("-");
                String[] timeTokens = timePart.split(":");
                if (dateTokens.length >= 3 && timeTokens.length >= 2) {
                    return String.format(Locale.getDefault(), "%s/%s/%s %s:%s",
                            dateTokens[2], dateTokens[1], dateTokens[0], timeTokens[0], timeTokens[1]);
                }
            }
        } catch (Exception ignore) {}
        // si no se pudo parsear, devolver original
        return timestamp;
    }

    // Muestra en llRecords la lista de alarmas guardada en prefs (KEY_ALARM_LIST_JSON)
    private void displayScheduledAlarms(LinearLayout container) {
        try {
            String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ALARM_LIST_JSON, null);
            if (json == null || json.trim().isEmpty()) {
                addRecordView(container, "No hay alarmas programadas.", false);
                return;
            }
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) {
                addRecordView(container, "No hay alarmas programadas.", false);
                return;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int id = o.optInt("id", i);
                int hour = o.optInt("alarm_hour", 0);
                int minute = o.optInt("alarm_min", 0);
                int active = o.optInt("active", 0);
                String label = o.optString("label", "");
                String hh = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                String text = String.format(Locale.getDefault(), "%s  —  %s  (%s)", hh, (label == null || label.isEmpty()) ? "Sin etiqueta" : label, (active==1) ? "Activo" : "Inactivo");
                // usar estilo neutro (no marcar como isBad)
                addRecordView(container, text, false);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error mostrando alarmas desde prefs: " + e.getMessage(), e);
            addRecordView(container, "Error leyendo alarmas.", true);
        }
    }
}
