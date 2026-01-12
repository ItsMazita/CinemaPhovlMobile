package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.phovl.cinemaphovlmobile.R;

public class BoletosActivity extends AppCompatActivity {

    private int ninos = 0, adultos = 0, estudiantes = 0, mayores = 0;

    private final int PRECIO_NINOS = 55;
    private final int PRECIO_ADULTOS = 85;
    private final int PRECIO_ESTUDIANTES = 65;
    private final int PRECIO_MAYORES = 60;

    private TextView txtCantidadNinos, txtSubtotalNinos;
    private TextView txtCantidadAdultos, txtSubtotalAdultos;
    private TextView txtCantidadEstudiantes, txtSubtotalEstudiantes;
    private TextView txtCantidadMayores, txtSubtotalMayores;
    private TextView txtTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boletos);

        int idFuncion = getIntent().getIntExtra("idFuncion", 0);

        txtTotal = findViewById(R.id.txtTotal);
        Button btnContinuar = findViewById(R.id.btn_continuar);

        // Referencias Niños
        txtCantidadNinos = findViewById(R.id.txtCantidadNinos);
        txtSubtotalNinos = findViewById(R.id.txtSubtotalNinos);
        Button btnSumarNinos = findViewById(R.id.btnSumarNinos);
        Button btnRestarNinos = findViewById(R.id.btnRestarNinos);

        // Referencias Adultos
        txtCantidadAdultos = findViewById(R.id.txtCantidadAdultos);
        txtSubtotalAdultos = findViewById(R.id.txtSubtotalAdultos);
        Button btnSumarAdultos = findViewById(R.id.btnSumarAdultos);
        Button btnRestarAdultos = findViewById(R.id.btnRestarAdultos);

        // Referencias Estudiantes
        txtCantidadEstudiantes = findViewById(R.id.txtCantidadEstudiantes);
        txtSubtotalEstudiantes = findViewById(R.id.txtSubtotalEstudiantes);
        Button btnSumarEstudiantes = findViewById(R.id.btnSumarEstudiantes);
        Button btnRestarEstudiantes = findViewById(R.id.btnRestarEstudiantes);

        // Referencias Mayores
        txtCantidadMayores = findViewById(R.id.txtCantidadMayores);
        txtSubtotalMayores = findViewById(R.id.txtSubtotalMayores);
        Button btnSumarMayores = findViewById(R.id.btnSumarMayores);
        Button btnRestarMayores = findViewById(R.id.btnRestarMayores);

        // Listeners
        btnSumarNinos.setOnClickListener(v -> { ninos++; actualizarUI(); });
        btnRestarNinos.setOnClickListener(v -> { if (ninos > 0) ninos--; actualizarUI(); });

        btnSumarAdultos.setOnClickListener(v -> { adultos++; actualizarUI(); });
        btnRestarAdultos.setOnClickListener(v -> { if (adultos > 0) adultos--; actualizarUI(); });

        btnSumarEstudiantes.setOnClickListener(v -> { estudiantes++; actualizarUI(); });
        btnRestarEstudiantes.setOnClickListener(v -> { if (estudiantes > 0) estudiantes--; actualizarUI(); });

        btnSumarMayores.setOnClickListener(v -> { mayores++; actualizarUI(); });
        btnRestarMayores.setOnClickListener(v -> { if (mayores > 0) mayores--; actualizarUI(); });

        btnContinuar.setOnClickListener(v -> {
            int totalBoletos = ninos + adultos + estudiantes + mayores;

            Intent intent = new Intent(BoletosActivity.this, SeleccionAsientosActivity.class);
            intent.putExtra("idFuncion", idFuncion);
            intent.putExtra("totalBoletos", totalBoletos);
            startActivity(intent);
        });

        actualizarUI();
    }

    private void actualizarUI() {
        txtCantidadNinos.setText(String.valueOf(ninos));
        txtSubtotalNinos.setText("Subtotal: $" + (ninos * PRECIO_NINOS) + " MXN");

        txtCantidadAdultos.setText(String.valueOf(adultos));
        txtSubtotalAdultos.setText("Subtotal: $" + (adultos * PRECIO_ADULTOS) + " MXN");

        txtCantidadEstudiantes.setText(String.valueOf(estudiantes));
        txtSubtotalEstudiantes.setText("Subtotal: $" + (estudiantes * PRECIO_ESTUDIANTES) + " MXN");

        txtCantidadMayores.setText(String.valueOf(mayores));
        txtSubtotalMayores.setText("Subtotal: $" + (mayores * PRECIO_MAYORES) + " MXN");

        int total = (ninos * PRECIO_NINOS) + (adultos * PRECIO_ADULTOS) +
                (estudiantes * PRECIO_ESTUDIANTES) + (mayores * PRECIO_MAYORES);

        txtTotal.setText("$" + total + " MXN");
    }
}
