package com.phovl.cinemaphovlmobile.model;

import java.util.List;

public class PeliculaConFunciones {
    private String titulo;
    private String sinopsis;
    private String clasificacion;
    private String duracion;
    private String carteleraUrl;
    private List<Funcion> funcionesDobladas;
    private List<Funcion> funcionesSubtituladas;

    public PeliculaConFunciones(String titulo, String sinopsis, String clasificacion,
                                String duracion, String carteleraUrl,
                                List<Funcion> funcionesDobladas, List<Funcion> funcionesSubtituladas) {
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.clasificacion = clasificacion;
        this.duracion = duracion;
        this.carteleraUrl = carteleraUrl;
        this.funcionesDobladas = funcionesDobladas;
        this.funcionesSubtituladas = funcionesSubtituladas;
    }

    public String getTitulo() { return titulo; }
    public String getSinopsis() { return sinopsis; }
    public String getClasificacion() { return clasificacion; }
    public String getDuracion() { return duracion; }
    public String getCarteleraUrl() { return carteleraUrl; }
    public List<Funcion> getFuncionesDobladas() { return funcionesDobladas; }
    public List<Funcion> getFuncionesSubtituladas() { return funcionesSubtituladas; }
}
