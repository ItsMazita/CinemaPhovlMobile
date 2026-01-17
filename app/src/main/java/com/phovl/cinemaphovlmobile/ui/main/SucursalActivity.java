package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.adapter.PeliculaAdapter;
import com.phovl.cinemaphovlmobile.model.Funcion;
import com.phovl.cinemaphovlmobile.model.Pelicula;
import com.phovl.cinemaphovlmobile.model.PeliculaConFunciones;
import com.phovl.cinemaphovlmobile.network.ApiService;
import com.phovl.cinemaphovlmobile.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 Pantalla de sucursal: muestra películas y sus funciones para la sucursal seleccionada.
 Recibe extras: "idSucursal" (int) y "nombreSucursal" (String).
*/
public class SucursalActivity extends AppCompatActivity {

    private static final String TAG = "SucursalActivity";

    private int idSucursal;
    private String nombreSucursal;

    private TextView txtHeaderTitle;
    private ImageButton btnBack;
    private RecyclerView recyclerPeliculas;
    private ProgressBar progressBar;

    private List<PeliculaConFunciones> peliculasConFunciones = new ArrayList<>();
    private PeliculaAdapter peliculaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sucursal);

        // Extras
        idSucursal = getIntent().getIntExtra("idSucursal", -1);
        nombreSucursal = getIntent().getStringExtra("nombreSucursal");

        txtHeaderTitle = findViewById(R.id.txt_sucursal_title);
        btnBack = findViewById(R.id.btn_back_sucursal);
        recyclerPeliculas = findViewById(R.id.recycler_peliculas_sucursal);
        progressBar = findViewById(R.id.progress_sucursal);

        // Header
        if (nombreSucursal != null && !nombreSucursal.isEmpty()) {
            txtHeaderTitle.setText(nombreSucursal);
        } else {
            txtHeaderTitle.setText(getString(R.string.app_name));
        }

        btnBack.setOnClickListener(v -> finish());

        // RecyclerView con PeliculaAdapter (muestra título, sinopsis y horarios)
        recyclerPeliculas.setLayoutManager(new LinearLayoutManager(this));
        peliculaAdapter = new PeliculaAdapter(peliculasConFunciones, this::onHorarioClick);
        recyclerPeliculas.setAdapter(peliculaAdapter);

        // Cargar películas y funciones para esta sucursal
        cargarPeliculasConFunciones();
    }

    // Nueva firma: recibe funcion y titulo
    private void onHorarioClick(Funcion funcion, String tituloPelicula) {
        Intent intent = new Intent(SucursalActivity.this, BoletosActivity.class);
        intent.putExtra("idFuncion", funcion.getId());
        intent.putExtra("nombrePelicula", tituloPelicula); // <-- pasa el título
        startActivity(intent);
    }


    private void cargarPeliculasConFunciones() {
        showLoading(true);

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getPeliculas().enqueue(new Callback<List<Pelicula>>() {
            @Override
            public void onResponse(Call<List<Pelicula>> call, Response<List<Pelicula>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pelicula> peliculas = response.body();
                    if (peliculas.isEmpty()) {
                        showEmpty("No hay películas disponibles");
                        return;
                    }

                    peliculasConFunciones.clear();
                    final int total = peliculas.size();
                    final int[] pendientes = {total};
                    String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    for (Pelicula p : peliculas) {
                        int idPelicula = p.getId_pelicula();
                        api.getFunciones(idPelicula, idSucursal, fechaHoy).enqueue(new Callback<List<Funcion>>() {
                            @Override
                            public void onResponse(Call<List<Funcion>> call, Response<List<Funcion>> response) {
                                List<Funcion> funciones = new ArrayList<>();
                                if (response.isSuccessful() && response.body() != null) {
                                    funciones = response.body();
                                }

                                List<Funcion> dobladas = new ArrayList<>();
                                List<Funcion> subtituladas = new ArrayList<>();
                                for (Funcion f : funciones) {
                                    String idioma = f.getIdioma() != null ? f.getIdioma().toLowerCase() : "";
                                    if (idioma.contains("dobl")) {
                                        dobladas.add(f);
                                    } else {
                                        subtituladas.add(f);
                                    }
                                }

                                PeliculaConFunciones pcf = new PeliculaConFunciones(
                                        p.getTitulo(),
                                        p.getSinopsis(),
                                        p.getClasificacion(),
                                        String.valueOf(p.getDuracion()),
                                        p.getCarteleraUrl(),
                                        dobladas,
                                        subtituladas
                                );

                                synchronized (peliculasConFunciones) {
                                    peliculasConFunciones.add(pcf);
                                }

                                pendientes[0]--;
                                if (pendientes[0] == 0) {
                                    runOnUiThread(() -> {
                                        peliculaAdapter.notifyDataSetChanged();
                                        showLoading(false);
                                    });
                                }
                            }

                            @Override
                            public void onFailure(Call<List<Funcion>> call, Throwable t) {
                                Log.e(TAG, "Error getFunciones for pelicula " + idPelicula + ": " + t.getMessage());
                                PeliculaConFunciones pcf = new PeliculaConFunciones(
                                        p.getTitulo(),
                                        p.getSinopsis(),
                                        p.getClasificacion(),
                                        String.valueOf(p.getDuracion()),
                                        p.getCarteleraUrl(),
                                        new ArrayList<>(),
                                        new ArrayList<>()
                                );

                                synchronized (peliculasConFunciones) {
                                    peliculasConFunciones.add(pcf);
                                }

                                pendientes[0]--;
                                if (pendientes[0] == 0) {
                                    runOnUiThread(() -> {
                                        peliculaAdapter.notifyDataSetChanged();
                                        showLoading(false);
                                    });
                                }
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "getPeliculas no successful. code=" + response.code());
                    showEmpty("Error al cargar películas: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Pelicula>> call, Throwable t) {
                Log.e(TAG, "Error cargarPeliculas: " + t.getMessage(), t);
                showEmpty("Error de conexión al cargar películas");
            }
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerPeliculas.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }

    private void showEmpty(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            recyclerPeliculas.setVisibility(View.GONE);
            Toast.makeText(SucursalActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }
}
