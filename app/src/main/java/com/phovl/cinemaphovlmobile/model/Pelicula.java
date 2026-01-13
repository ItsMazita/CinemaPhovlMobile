package com.phovl.cinemaphovlmobile.model;

import com.google.gson.annotations.SerializedName;

/*
 Modelo que refleja la tabla pelicula en la API:
 id_pelicula, titulo, sinopsis, clasificacion, duracion, cartelera_url
*/
public class Pelicula {
    @SerializedName("id_pelicula")
    private int id_pelicula;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("sinopsis")
    private String sinopsis;

    @SerializedName("clasificacion")
    private String clasificacion;

    @SerializedName("duracion")
    private int duracion;

    @SerializedName("cartelera_url")
    private String cartelera_url;

    public Pelicula(int id_pelicula, String titulo, String sinopsis,
                    String clasificacion, int duracion, String cartelera_url) {
        this.id_pelicula = id_pelicula;
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.clasificacion = clasificacion;
        this.duracion = duracion;
        this.cartelera_url = cartelera_url;
    }

    public int getId_pelicula() {
        return id_pelicula;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public int getDuracion() {
        return duracion;
    }

    public String getCartelera_url() {
        return cartelera_url;
    }

    // Compatibilidad con nombres usados en la UI
    public String getCarteleraUrl() {
        return cartelera_url;
    }
}
