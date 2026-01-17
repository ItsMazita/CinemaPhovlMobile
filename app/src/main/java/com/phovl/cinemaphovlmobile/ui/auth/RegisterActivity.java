package com.phovl.cinemaphovlmobile.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.RegisterRequest;
import com.phovl.cinemaphovlmobile.model.RegisterResponse;
import com.phovl.cinemaphovlmobile.network.ApiService;
import com.phovl.cinemaphovlmobile.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtName = findViewById(R.id.edt_name);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        edtConfirmPassword = findViewById(R.id.edt_confirm_password);
        btnRegister = findViewById(R.id.btn_register);

        // Validación en tiempo real del email (muestra error si no es válido)
        edtEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                String email = s == null ? "" : s.toString().trim();
                if (email.isEmpty()) {
                    edtEmail.setError(null);
                } else if (!isValidEmail(email)) {
                    edtEmail.setError("Ingresa un correo válido");
                } else {
                    edtEmail.setError(null);
                }
                updateRegisterButtonState();
            }
        });

        // También actualizamos el estado del botón cuando cambian otros campos
        TextWatcher enableWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { updateRegisterButtonState(); }
        };
        edtName.addTextChangedListener(enableWatcher);
        edtPassword.addTextChangedListener(enableWatcher);
        edtConfirmPassword.addTextChangedListener(enableWatcher);

        btnRegister.setOnClickListener(v -> register());

        // Inicialmente deshabilitado hasta que haya datos mínimos válidos
        updateRegisterButtonState();
    }

    private void updateRegisterButtonState() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirm = edtConfirmPassword.getText().toString();

        boolean enabled = !name.isEmpty()
                && !email.isEmpty()
                && isValidEmail(email)
                && !password.isEmpty()
                && !confirm.isEmpty();
        btnRegister.setEnabled(enabled);
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void register() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirm = edtConfirmPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            edtEmail.setError("Ingresa un correo válido");
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aquí: solo mostramos un Toast si las contraseñas no coinciden.
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Opcional: deshabilitar botón para evitar múltiples envíos
        btnRegister.setEnabled(false);

        RegisterRequest request = new RegisterRequest(name, email, password);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                btnRegister.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this,
                            response.body().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Error al registrar: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
