package com.phovl.cinemaphovlmobile.model;

public class Funcion {
    private int id;
    private String hora;
    private String idioma;

    public Funcion(int id, String hora, String idioma) {
        this.id = id;
        this.hora = hora;
        this.idioma = idioma;
    }

    public int getId() { return id; }
    public String getHora() { return hora; }
    public String getIdioma() { return idioma; }
}
