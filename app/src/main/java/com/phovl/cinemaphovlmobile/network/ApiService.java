package com.phovl.cinemaphovlmobile.network;

import com.phovl.cinemaphovlmobile.model.AuthResponse;
import com.phovl.cinemaphovlmobile.model.Funcion;
import com.phovl.cinemaphovlmobile.model.LoginRequest;
import com.phovl.cinemaphovlmobile.model.Pelicula;
import com.phovl.cinemaphovlmobile.model.RegisterRequest;
import com.phovl.cinemaphovlmobile.model.RegisterResponse;
import com.phovl.cinemaphovlmobile.model.Sucursal;
import com.phovl.cinemaphovlmobile.network.model.CompraRequest;
import com.phovl.cinemaphovlmobile.network.model.CompraResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // AUTH
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    // PELICULAS
    @GET("api/peliculas")
    Call<List<Pelicula>> getPeliculas();

    // SUCURSALES
    @GET("api/sucursales")
    Call<List<Sucursal>> getSucursales();


    // COMPRA: usar CompraRequest / CompraResponse (lista de tickets)
    @POST("api/tickets/comprar")
    Call<CompraResponse> comprarTickets(@Body CompraRequest request);

    // FUNCIONES por pelicula, sucursal y fecha (YYYY-MM-DD)
    @GET("api/funciones/{id_pelicula}/{id_sucursal}/{fecha}")
    Call<List<Funcion>> getFunciones(
            @Path("id_pelicula") int idPelicula,
            @Path("id_sucursal") int idSucursal,
            @Path("fecha") String fecha
    );

    // Obtener asientos ocupados para una función (devuelve JSON array)
    @GET("api/asientos/funciones/{id_funcion}/asientos-ocupados")
    Call<ResponseBody> getAsientosOcupados(@Path("id_funcion") int idFuncion);

}
