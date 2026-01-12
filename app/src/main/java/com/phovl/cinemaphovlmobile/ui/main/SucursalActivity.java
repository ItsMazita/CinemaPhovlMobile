package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.adapter.PeliculaAdapter;
import com.phovl.cinemaphovlmobile.model.Funcion;
import com.phovl.cinemaphovlmobile.model.PeliculaConFunciones;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SucursalActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PeliculaAdapter adapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sucursal);

        int idSucursal = getIntent().getIntExtra("idSucursal", 0);
        String nombreSucursal = getIntent().getStringExtra("nombreSucursal");

        TextView txtSucursal = findViewById(R.id.txtSucursal);
        if (nombreSucursal != null) {
            txtSucursal.setText(nombreSucursal);
        }

        recyclerView = findViewById(R.id.recycler_sections);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        btnMenu = findViewById(R.id.btn_menu);
        btnProfile = findViewById(R.id.btn_profile);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(SucursalActivity.this, ProfileActivity.class));
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_inicio) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.menu_sucursal_centro) {
                abrirSucursal(1, "Sucursal Centro");
            } else if (id == R.id.menu_sucursal_norte) {
                abrirSucursal(2, "Sucursal Norte");
            } else if (id == R.id.menu_sucursal_sur) {
                abrirSucursal(3, "Sucursal Sur");
            }

            drawerLayout.closeDrawers();
            return true;
        });

        getPeliculasPorSucursal(idSucursal, listaPeliculas -> {
            adapter = new PeliculaAdapter(listaPeliculas, funcion -> {
                Intent intent = new Intent(SucursalActivity.this, BoletosActivity.class);
                intent.putExtra("idFuncion", funcion.getId());
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        });
    }

    private void abrirSucursal(int idSucursal, String nombreSucursal) {
        Intent intent = new Intent(this, SucursalActivity.class);
        intent.putExtra("idSucursal", idSucursal);
        intent.putExtra("nombreSucursal", nombreSucursal);
        startActivity(intent);
    }

    private void getPeliculasPorSucursal(int idSucursal, PeliculasCallback callback) {
        List<Funcion> dobladas = Arrays.asList(
                new Funcion(1, "12:00", "doblada"),
                new Funcion(2, "3:00", "doblada")
        );

        List<Funcion> subtituladas = Arrays.asList(
                new Funcion(3, "5:00", "subtitulada"),
                new Funcion(4, "8:00", "subtitulada")
        );

        PeliculaConFunciones pelicula = new PeliculaConFunciones(
                "Avatar",
                "Los Na’vi exploran las regiones volcánicas de Pandora.",
                "PG-13",
                "2h 55m",
                "https://i.imgur.com/Fr9c3z2.jpeg",
                dobladas,
                subtituladas
        );

        callback.onResult(Collections.singletonList(pelicula));
    }

    interface PeliculasCallback {
        void onResult(List<PeliculaConFunciones> listaPeliculas);
    }
}
