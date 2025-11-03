package com.lacalandria.applacalandria;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

public class Main extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Buscar vistas (asegúrate de que activity_main.xml tenga tvHello y opcionalmente btnAction)
        TextView tvHello = findViewById(R.id.tvHello);

        if (tvHello != null) {
            // texto inicial opcional
            tvHello.setText(tvHello.getText());
        }

    }
}