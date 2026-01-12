package com.phovl.cinemaphovlmobile.ui.main;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.adapter.AsientoAdapter;
import com.phovl.cinemaphovlmobile.model.Asiento;

import java.util.ArrayList;
import java.util.List;

public class SeleccionAsientosActivity extends AppCompatActivity {

    private RecyclerView recyclerAsientos;
    private TextView txtBoletos;
    private Button btnConfirmar;
    private AsientoAdapter asientoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion_asientos);

        // Recibir límite de boletos desde BoletosActivity
        int limiteBoletos = getIntent().getIntExtra("totalBoletos", 0);

        recyclerAsientos = findViewById(R.id.recycler_asientos);
        txtBoletos = findViewById(R.id.txtBoletos);
        btnConfirmar = findViewById(R.id.btn_confirmar);

        recyclerAsientos.setLayoutManager(new GridLayoutManager(this, 12)); // 12 columnas

        List<Asiento> asientos = generarAsientos();

        asientoAdapter = new AsientoAdapter(asientos, seleccionados -> {
            txtBoletos.setText("Boletos seleccionados: " + seleccionados.size());
            btnConfirmar.setEnabled(seleccionados.size() == limiteBoletos);
        }, limiteBoletos);

        recyclerAsientos.setAdapter(asientoAdapter);

        btnConfirmar.setOnClickListener(v -> {
            int cantidad = asientoAdapter.getSeleccionados().size();
            if (cantidad == limiteBoletos) {
                Toast.makeText(this, "Confirmaste " + cantidad + " asiento(s)", Toast.LENGTH_SHORT).show();
                // Aquí podrías ir a la pantalla de pago o siguiente paso
            } else {
                Toast.makeText(this, "Debes seleccionar exactamente " + limiteBoletos + " asientos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Genera todos los asientos disponibles
    private List<Asiento> generarAsientos() {
        List<Asiento> lista = new ArrayList<>();
        String[] filas = {"A","B","C","D","E","F","G","H","I","J","K","L"};
        for (String fila : filas) {
            for (int i = 1; i <= 12; i++) {
                lista.add(new Asiento(fila + i, false)); // todos disponibles
            }
        }
        return lista;
    }
}
