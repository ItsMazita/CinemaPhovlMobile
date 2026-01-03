package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.adapter.SectionAdapter;
import com.phovl.cinemaphovlmobile.model.Section;
import com.phovl.cinemaphovlmobile.session.SessionManager;
import com.phovl.cinemaphovlmobile.ui.auth.LoginActivity;
import com.phovl.cinemaphovlmobile.ui.main.ProfileActivity;
import android.view.Gravity;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Protección de sesión
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        ImageButton btnProfile = findViewById(R.id.btn_profile);

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });


        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        ImageButton btnMenu = findViewById(R.id.btn_menu);

        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });


        // Menú lateral
        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.menu_inicio) {
                drawerLayout.closeDrawers();
                return true;

            } else if (id == R.id.menu_sucursal_centro) {
                irASucursal("CENTRO");

            } else if (id == R.id.menu_sucursal_norte) {
                irASucursal("NORTE");

            } else if (id == R.id.menu_sucursal_sur) {
                irASucursal("SUR");
            }

            drawerLayout.closeDrawers();
            return true;
        });


        // RecyclerView principal (SECCIONES)
        RecyclerView recyclerSections = findViewById(R.id.recycler_sections);
        recyclerSections.setLayoutManager(new LinearLayoutManager(this));

        List<Section> sections = new ArrayList<>();

        sections.add(new Section("Estrenos", List.of(
                "Five Nights at Freddy's 2",
                "Avatar: The Seed Bearer",
                "Zootopia 2"
        )));

        sections.add(new Section("Reestrenos", List.of(
                "Oppenheimer",
                "Barbie",
                "Spider-Man: No Way Home"
        )));

        sections.add(new Section("Preventa", List.of(
                "Deadpool 3",
                "Avengers: Secret Wars"
        )));

        recyclerSections.setAdapter(new SectionAdapter(sections));
    }

    // Método para las sucursales
    private void irASucursal(String sucursal) {

        // Abrir la activity de esa sucuarsal
    }

}
