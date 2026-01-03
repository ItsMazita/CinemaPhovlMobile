package com.phovl.cinemaphovlmobile.network;

import com.phovl.cinemaphovlmobile.model.LoginRequest;
import com.phovl.cinemaphovlmobile.model.RegisterRequest;
import com.phovl.cinemaphovlmobile.model.AuthResponse;
import com.phovl.cinemaphovlmobile.model.RegisterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    // LOGIN devuelve token
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // REGISTER solo devuelve mensaje
    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
}
