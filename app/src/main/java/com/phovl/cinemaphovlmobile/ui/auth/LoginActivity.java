package com.phovl.cinemaphovlmobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.AuthResponse;
import com.phovl.cinemaphovlmobile.model.LoginRequest;
import com.phovl.cinemaphovlmobile.network.RetrofitClient;
import com.phovl.cinemaphovlmobile.network.ApiService;
import com.phovl.cinemaphovlmobile.session.SessionManager;
import com.phovl.cinemaphovlmobile.ui.main.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView txtRegister;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Si ya hay sesión mandar directamente a main
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnLogin = findViewById(R.id.btn_login);
        txtRegister = findViewById(R.id.txt_register);

        btnLogin.setOnClickListener(v -> login());

        txtRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void login() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        LoginRequest request = new LoginRequest(email, password);

        api.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    AuthResponse auth = response.body();
                    String token = auth.getToken();
                    String name = auth.getName();

                    // Intentamos extraer id_usuario del JWT (payload)
                    int userId = extractUserIdFromJwt(token);

                    if (userId != -1) {
                        sessionManager.saveSession(token, name, userId);
                    } else {
                        // Si no pudimos extraer id, guardamos sin id (compatibilidad)
                        sessionManager.saveSession(token, name);
                    }

                    Toast.makeText(LoginActivity.this, "Login exitoso", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "login failure", t);
                Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Decodifica el payload del JWT (sin verificar) y extrae "id_usuario" o "id".
     * Retorna -1 si no se encuentra.
     */
    private int extractUserIdFromJwt(String jwt) {
        if (jwt == null) return -1;
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return -1;
            String payload = parts[1];
            // Base64 URL decode
            byte[] decoded = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String json = new String(decoded);
            JSONObject obj = new JSONObject(json);
            if (obj.has("id_usuario")) {
                return obj.getInt("id_usuario");
            } else if (obj.has("id")) {
                return obj.getInt("id");
            } else if (obj.has("userId")) {
                return obj.getInt("userId");
            }
        } catch (IllegalArgumentException | JSONException e) {
            Log.w(TAG, "No se pudo extraer id del JWT: " + e.getMessage());
        }
        return -1;
    }
}
