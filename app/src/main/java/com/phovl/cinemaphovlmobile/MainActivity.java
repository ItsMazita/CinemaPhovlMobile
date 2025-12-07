package com.phovl.cinemaphovlmobile;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerEstrenos;
    private RecyclerView recyclerReestrenos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Referencias a los RecyclerView
        recyclerEstrenos = findViewById(R.id.recycler_estrenos);
        recyclerReestrenos = findViewById(R.id.recycler_reestrenos);

        // Layout horizontal
        recyclerEstrenos.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerReestrenos.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Datos de prueba
        List<String> estrenos = new ArrayList<>();
        estrenos.add("Five Nights at Freddy's 2");
        estrenos.add("Avatar: The Seed Bearer");
        estrenos.add("Zootopia 2");
        estrenos.add("The King of Kings");

        List<String> reestrenos = new ArrayList<>();
        reestrenos.add("Inside Out 2");
        reestrenos.add("Oppenheimer");
        reestrenos.add("Barbie");
        reestrenos.add("Spider-Man: No Way Home");
        reestrenos.add("Guardians of the Galaxy Vol. 3");

        // Adaptadores
        PeliculasAdapter adapterEstrenos = new PeliculasAdapter(estrenos);
        PeliculasAdapter adapterReestrenos = new PeliculasAdapter(reestrenos);

        recyclerEstrenos.setAdapter(adapterEstrenos);
        recyclerReestrenos.setAdapter(adapterReestrenos);
    }
}
