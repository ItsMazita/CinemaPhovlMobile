package com.phovl.cinemaphovlmobile.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("nombre")   // <-- el backend espera "nombre"
    private String nombre;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    public RegisterRequest(String nombre, String email, String password) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }
}
