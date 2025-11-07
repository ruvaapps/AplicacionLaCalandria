package com.lacalandria.applacalandria;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ControlCercoActivity extends AppCompatActivity {
    private static final String TAG = "ControlCercoActivity";

    private CheckBox cbRojaSi, cbRojaNo;
    private CheckBox cbVerdeSi, cbVerdeNo;
    private Button btnGuardar;
    private Button btnVolver;
    private Button btnVerCercos;
    private Button btnInstructivo;
    private TextView tvTimestamp;

    // Nuevo: botón y texto para QR + variable que guarda la descripción del tramo escaneado
    private Button btnEscanearQR;
    private TextView tvTramoDesc;
    private String scannedTramo = "";

    private static final String PREFS = "control_cerco";
    private static final int SCAN_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_cerco);

        try {
            // Timestamp
            final String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            tvTimestamp = findViewById(R.id.tvTimestamp);
            if (tvTimestamp != null) tvTimestamp.setText(currentTimestamp);

            // Top buttons: mantener comportamiento similar a Home (opcional ocultar botón Control si se desea)
            Button topControl = findViewById(R.id.btnControlCerco);
            Button topCargar = findViewById(R.id.btnCargarContingencia);
            if (topControl != null) {
                // Si estamos ya en ControlCerco, evitar reabrir: opcional, aquí no hacemos nada
                topControl.setOnClickListener(v -> {
                    Toast.makeText(ControlCercoActivity.this, "Ya estás en Control Cerco", Toast.LENGTH_SHORT).show();
                });
            }
            if (topCargar != null) {
                topCargar.setOnClickListener(v -> {
                    Toast.makeText(ControlCercoActivity.this, "Cargar Contingencia seleccionado", Toast.LENGTH_SHORT).show();
                });
            }

            // Volver (usa layout positioning; no sobrescribimos background)
            btnVolver = findViewById(R.id.btnVolver);
            if (btnVolver != null) {
                btnVolver.setOnClickListener(v -> {
                    // volver a Home sin limpiar pila (mantener comportamiento anterior)
                    finish();
                });
            }

            // Nueva: enlazar btnMapa y btnInstructivo directamente (ids añadidos en el layout)
            Button btnMapa = findViewById(R.id.btnMapa);
            btnInstructivo = findViewById(R.id.btnInstructivo);

            if (btnMapa != null) {
                btnMapa.setOnClickListener(v -> {
                    // animación breve
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80)).start();
                    try {
                        DisplayMetrics dm = getResources().getDisplayMetrics();
                        int maxWidth = dm.widthPixels - (int)(dm.density * 48);

                        Bitmap bmp = decodeSampledBitmapFromResource(getResources(), R.drawable.cercos, maxWidth, maxWidth);
                        if (bmp != null) {
                            ImageView iv = new ImageView(ControlCercoActivity.this);
                            iv.setAdjustViewBounds(true);
                            iv.setImageBitmap(bmp);

                            AlertDialog.Builder builder = new AlertDialog.Builder(ControlCercoActivity.this);
                            builder.setView(iv);
                            builder.setPositiveButton("Cerrar", (dialog, which) -> {
                                try { bmp.recycle(); } catch (Exception ignore) {}
                            });
                            builder.setOnDismissListener(dialog -> {
                                try { bmp.recycle(); } catch (Exception ignore) {}
                            });
                            builder.show();
                        } else {
                            Toast.makeText(ControlCercoActivity.this, "Imagen 'cercos' no encontrada", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Error mostrando plano", t);
                        Toast.makeText(ControlCercoActivity.this, "Error mostrando plano: " + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (btnInstructivo != null) {
                btnInstructivo.setOnClickListener(v -> {
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80)).start();
                    String mensaje = "Instructivo:\n\n"
                            + "1. Seleccione el tramo.\n"
                            + "2. Marque Si/No según la luz Roja y Verde.\n"
                            + "3. Presione Guardar para enviar el registro al servidor.";
                    new AlertDialog.Builder(ControlCercoActivity.this)
                            .setTitle("Instructivo")
                            .setMessage(mensaje)
                            .setPositiveButton("Cerrar", null)
                            .show();
                });
            }

            // Nuevo: referencias al botón de escaneo y TextView de tramo
            btnEscanearQR = findViewById(R.id.btnEscanearQR);
            tvTramoDesc = findViewById(R.id.tvTramoDesc);
            if (tvTramoDesc != null) tvTramoDesc.setText("Tramo: (sin seleccionar)");

            if (btnEscanearQR != null) {
                btnEscanearQR.setOnClickListener(v -> {
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80)).start();
                    // Intent a ZXing (si no está instalado se hace fallback)
                    try {
                        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                        startActivityForResult(intent, SCAN_REQUEST);
                    } catch (ActivityNotFoundException e) {
                        // fallback: pedir al usuario que pegue el contenido del QR
                        final EditText input = new EditText(ControlCercoActivity.this);
                        new AlertDialog.Builder(ControlCercoActivity.this)
                                .setTitle("Ingresar código QR")
                                .setMessage("No se encontró app de escaneo. Pega aquí el contenido del QR:")
                                .setView(input)
                                .setPositiveButton("Aceptar", (dialog, which) -> {
                                    String value = input.getText().toString().trim();
                                    if (!value.isEmpty()) {
                                        handleScannedValue(value);
                                    } else {
                                        Toast.makeText(ControlCercoActivity.this, "Contenido vacío", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                });
            }

            // CheckBoxes Roja
            cbRojaSi = findViewById(R.id.cbRojaSi);
            cbRojaNo = findViewById(R.id.cbRojaNo);
            if (cbRojaSi != null && cbRojaNo != null) {
                cbRojaSi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked && cbRojaNo.isChecked()) cbRojaNo.setChecked(false);
                    }
                });
                cbRojaNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked && cbRojaSi.isChecked()) cbRojaSi.setChecked(false);
                    }
                });
            }

            // CheckBoxes Verde
            cbVerdeSi = findViewById(R.id.cbVerdeSi);
            cbVerdeNo = findViewById(R.id.cbVerdeNo);
            if (cbVerdeSi != null && cbVerdeNo != null) {
                cbVerdeSi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked && cbVerdeNo.isChecked()) cbVerdeNo.setChecked(false);
                    }
                });
                cbVerdeNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked && cbVerdeSi.isChecked()) cbVerdeSi.setChecked(false);
                    }
                });
            }

            // Botón Guardar: ahora usa scannedTramo (o texto mostrado)
            btnGuardar = findViewById(R.id.btnGuardar);
            if (btnGuardar != null) {
                btnGuardar.setOnClickListener(v -> {
                    String tramo = scannedTramo != null && !scannedTramo.isEmpty()
                            ? scannedTramo
                            : (tvTramoDesc != null ? tvTramoDesc.getText().toString() : "");

                    boolean rojaSi = cbRojaSi != null && cbRojaSi.isChecked();
                    boolean rojaNo = cbRojaNo != null && cbRojaNo.isChecked();
                    boolean verdeSi = cbVerdeSi != null && cbVerdeSi.isChecked();
                    boolean verdeNo = cbVerdeNo != null && cbVerdeNo.isChecked();

                    // Guardar localmente
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("tramo", tramo);
                    editor.putBoolean("roja_si", rojaSi);
                    editor.putBoolean("roja_no", rojaNo);
                    editor.putBoolean("verde_si", verdeSi);
                    editor.putBoolean("verde_no", verdeNo);
                    editor.putString("timestamp", currentTimestamp);
                    editor.apply();

                    ControlCercoRecord record = new ControlCercoRecord(tramo, rojaSi, rojaNo, verdeSi, verdeNo, currentTimestamp);
                    sendToServer(record);
                    Toast.makeText(ControlCercoActivity.this, "Registro guardado localmente", Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Throwable t) {
            Log.e(TAG, "Fallo inicializando ControlCercoActivity", t);
            Toast.makeText(this, "Error al abrir Control Cerco: " + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // helper: decodifica y escala bitmap desde recursos
    private Bitmap decodeSampledBitmapFromResource(android.content.res.Resources res, int resId, int reqWidth, int reqHeight) {
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeResource(res, resId, options);
        } catch (Throwable t) {
            Log.e(TAG, "decodeSampledBitmapFromResource error", t);
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // Nuevo: método helper para enviar registro al servidor usando Retrofit
    private void sendToServer(ControlCercoRecord record) {
        try {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://www.vajillas.lovar.com.ar/") // Ajustá si tu archivo PHP está en subdirectorio
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService service = retrofit.create(ApiService.class);
            Call<ApiResponse> call = service.sendControlCerco(record);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().success) {
                            Toast.makeText(ControlCercoActivity.this, "Registro guardado en servidor", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ControlCercoActivity.this, "Error servidor: " + response.body().message, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String msg = response != null ? response.message() : "respuesta nula";
                        Toast.makeText(ControlCercoActivity.this, "Respuesta inválida: " + msg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(ControlCercoActivity.this, "Fallo conexión: " + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error al intentar enviar: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    // Manejar resultado de escaneo ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCAN_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String contents = data.getStringExtra("SCAN_RESULT");
                if (contents != null) {
                    handleScannedValue(contents);
                } else {
                    Toast.makeText(this, "No se obtuvo resultado del escaneo", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Procesa el contenido del QR: aquí asumimos que el QR contiene la 'descripción del tramo'.
    private void handleScannedValue(String value) {
        // Si el QR contiene una estructura más compleja, parseala aquí. Por ahora se usa el texto directo.
        scannedTramo = value;
        if (tvTramoDesc != null) tvTramoDesc.setText("Tramo: " + scannedTramo);
        Toast.makeText(this, "Tramo escaneado: " + scannedTramo, Toast.LENGTH_SHORT).show();
    }
}
