package com.phovl.cinemaphovlmobile.model;

/*
 Modelo que refleja la respuesta del endpoint:
 GET /api/funciones/:id_pelicula/:id_sucursal/:fecha
 Campos devueltos por la API: id_funcion, hora_inicio, idioma, sala

 Se añadió un constructor sobrecargado para mantener compatibilidad con
 código previo que instanciaba Funcion con 3 parámetros (id, hora, idioma).
*/
public class Funcion {
    private int id_funcion;
    private String hora_inicio;
    private String idioma;
    private String sala;

    // Constructor principal (4 parámetros) — corresponde a la API
    public Funcion(int id_funcion, String hora_inicio, String idioma, String sala) {
        this.id_funcion = id_funcion;
        this.hora_inicio = hora_inicio;
        this.idioma = idioma;
        this.sala = sala;
    }

    // Constructor sobrecargado (3 parámetros) — compatibilidad con código antiguo
    // Asigna sala como cadena vacía cuando no se proporciona.
    public Funcion(int id_funcion, String hora_inicio, String idioma) {
        this(id_funcion, hora_inicio, idioma, "");
    }

    public int getId_funcion() {
        return id_funcion;
    }

    public String getHora_inicio() {
        return hora_inicio;
    }

    public String getIdioma() {
        return idioma;
    }

    public String getSala() {
        return sala;
    }

    // Métodos de compatibilidad con el código existente que usaba getId() y getHora()
    public int getId() {
        return id_funcion;
    }

    public String getHora() {
        return hora_inicio;
    }
}
