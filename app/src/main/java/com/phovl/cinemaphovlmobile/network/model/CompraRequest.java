package com.phovl.cinemaphovlmobile.network.model;

import java.util.List;

public class CompraRequest {
    private int id_funcion;
    private List<Integer> id_asientos;
    private int id_usuario;

    public CompraRequest(int id_funcion, List<Integer> id_asientos, int id_usuario) {
        this.id_funcion = id_funcion;
        this.id_asientos = id_asientos;
        this.id_usuario = id_usuario;
    }

    public int getId_funcion() { return id_funcion; }
    public List<Integer> getId_asientos() { return id_asientos; }
    public int getId_usuario() { return id_usuario; }
}
