package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.session.SessionManager;
import com.phovl.cinemaphovlmobile.ui.auth.LoginActivity;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Session
        sessionManager = new SessionManager(this);

        // Boton
        Button btnLogout = findViewById(R.id.btn_logout);

        // Mostrar nombre de usuario
        SessionManager sessionManager = new SessionManager(this);

        TextView UserName = findViewById(R.id.user_name);
        UserName.setText(sessionManager.getUserName());


        // Cerrar sesión
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
