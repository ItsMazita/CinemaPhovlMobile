package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.adapter.PosterAdapter;
import com.phovl.cinemaphovlmobile.model.Pelicula;
import com.phovl.cinemaphovlmobile.model.Sucursal;
import com.phovl.cinemaphovlmobile.network.ApiService;
import com.phovl.cinemaphovlmobile.network.RetrofitClient;
import com.phovl.cinemaphovlmobile.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 MainActivity: muestra portadas en 3 carruseles horizontales.
 NOTA IMPORTANTE: el título del header se mantiene siempre como el nombre de la app
 hasta que el usuario seleccione explícitamente una sucursal desde el Drawer.
*/
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SessionManager sessionManager;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView txtAppTitle;
    private ImageButton btnMenu, btnProfile;

    private RecyclerView recyclerEstrenos, recyclerReestrenos, recyclerPreventa;
    private PosterAdapter adapterEstrenos, adapterReestrenos, adapterPreventa;

    private List<Pelicula> estrenos = new ArrayList<>();
    private List<Pelicula> reestrenos = new ArrayList<>();
    private List<Pelicula> preventa = new ArrayList<>();

    private List<Sucursal> sucursales = new ArrayList<>();
    private int selectedSucursalId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, com.phovl.cinemaphovlmobile.ui.auth.LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        txtAppTitle = findViewById(R.id.txt_app_title);
        btnMenu = findViewById(R.id.btn_menu);
        btnProfile = findViewById(R.id.btn_profile);

        // Forzar título de la app al iniciar (solo el nombre, sin sucursal)
        txtAppTitle.setText(getString(R.string.app_name));

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        // RecyclerViews horizontales
        recyclerEstrenos = findViewById(R.id.recycler_estrenos);
        recyclerReestrenos = findViewById(R.id.recycler_reestrenos);
        recyclerPreventa = findViewById(R.id.recycler_preventa);

        recyclerEstrenos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerReestrenos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerPreventa.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapterEstrenos = new PosterAdapter(this, estrenos);
        adapterReestrenos = new PosterAdapter(this, reestrenos);
        adapterPreventa = new PosterAdapter(this, preventa);

        recyclerEstrenos.setAdapter(adapterEstrenos);
        recyclerReestrenos.setAdapter(adapterReestrenos);
        recyclerPreventa.setAdapter(adapterPreventa);

        // Si añadiste SpacesItemDecoration, puedes registrarlo aquí (opcional)
        // int spacingPx = (int) (8 * getResources().getDisplayMetrics().density + 0.5f);
        // recyclerEstrenos.addItemDecoration(new SpacesItemDecoration(spacingPx));
        // recyclerReestrenos.addItemDecoration(new SpacesItemDecoration(spacingPx));
        // recyclerPreventa.addItemDecoration(new SpacesItemDecoration(spacingPx));

        cargarSucursales();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Asegurar que el título del header permanezca con el nombre de la app
        if (txtAppTitle != null) {
            txtAppTitle.setText(getString(R.string.app_name));
        }
    }

    private void cargarSucursales() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getSucursales().enqueue(new Callback<List<Sucursal>>() {
            @Override
            public void onResponse(Call<List<Sucursal>> call, Response<List<Sucursal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sucursales.clear();
                    sucursales.addAll(response.body());
                    poblarMenuSucursales();
                    // IMPORTANTE: no cambiamos txtAppTitle aquí.
                    // Mantener el título de la app hasta que el usuario seleccione una sucursal.
                    cargarPeliculas();
                } else {
                    Log.e(TAG, "getSucursales no successful. code=" + response.code());
                    Toast.makeText(MainActivity.this, "Error al cargar sucursales", Toast.LENGTH_SHORT).show();
                    cargarPeliculas();
                }
            }

            @Override
            public void onFailure(Call<List<Sucursal>> call, Throwable t) {
                Log.e(TAG, "Error cargarSucursales: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Error de conexión al cargar sucursales", Toast.LENGTH_SHORT).show();
                cargarPeliculas();
            }
        });
    }

    private void poblarMenuSucursales() {
        Menu menu = navigationView.getMenu();
        menu.clear();
        menu.add(Menu.NONE, R.id.menu_inicio, Menu.NONE, "Inicio");
        for (Sucursal s : sucursales) {
            menu.add(Menu.NONE, s.getId(), Menu.NONE, s.getNombre());
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_inicio) {
                // Mantener siempre el título de la app cuando el usuario elige "Inicio"
                txtAppTitle.setText(getString(R.string.app_name));
                drawerLayout.closeDrawers();
                return true;
            }

            for (Sucursal s : sucursales) {
                if (s.getId() == id) {
                    // NO cambiar txtAppTitle aquí — solo abrir la pantalla de sucursal
                    selectedSucursalId = s.getId();

                    Intent intent = new Intent(MainActivity.this, SucursalActivity.class);
                    intent.putExtra("idSucursal", s.getId());
                    intent.putExtra("nombreSucursal", s.getNombre());
                    startActivity(intent);
                    break;
                }
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void cargarPeliculas() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getPeliculas().enqueue(new Callback<List<Pelicula>>() {
            @Override
            public void onResponse(Call<List<Pelicula>> call, Response<List<Pelicula>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pelicula> peliculas = response.body();
                    distribuirCategorias(peliculas);
                } else {
                    Log.e(TAG, "getPeliculas no successful. code=" + response.code());
                    Toast.makeText(MainActivity.this, "Error al cargar películas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Pelicula>> call, Throwable t) {
                Log.e(TAG, "Error cargarPeliculas: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Error de conexión al cargar películas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     Estrategia simple para asignar películas a categorías:
     - Estrenos: primeros 5
     - Reestrenos: siguientes 5
     - Pre‑Venta: resto
    */
    private void distribuirCategorias(List<Pelicula> peliculas) {
        estrenos.clear();
        reestrenos.clear();
        preventa.clear();

        int i = 0;
        for (Pelicula p : peliculas) {
            if (i < 5) {
                estrenos.add(p);
            } else if (i < 10) {
                reestrenos.add(p);
            } else {
                preventa.add(p);
            }
            i++;
        }

        if (estrenos.isEmpty() && !peliculas.isEmpty()) {
            estrenos.add(peliculas.get(0));
        }
        if (reestrenos.isEmpty() && peliculas.size() > 1) {
            reestrenos.add(peliculas.get(Math.min(1, peliculas.size()-1)));
        }
        if (preventa.isEmpty() && peliculas.size() > 2) {
            preventa.add(peliculas.get(Math.min(2, peliculas.size()-1)));
        }

        runOnUiThread(() -> {
            adapterEstrenos.notifyDataSetChanged();
            adapterReestrenos.notifyDataSetChanged();
            adapterPreventa.notifyDataSetChanged();
        });
    }
}
