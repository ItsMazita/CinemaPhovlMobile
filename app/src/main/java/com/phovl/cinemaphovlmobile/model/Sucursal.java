package com.phovl.cinemaphovlmobile.model;

/*
 Modelo para sucursal:
 id_sucursal, nombre, direccion
*/
public class Sucursal {
    private int id_sucursal;
    private String nombre;
    private String direccion;

    public Sucursal(int id_sucursal, String nombre, String direccion) {
        this.id_sucursal = id_sucursal;
        this.nombre = nombre;
        this.direccion = direccion;
    }

    public int getId_sucursal() {
        return id_sucursal;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    // Compatibilidad con nombres más cortos
    public int getId() {
        return id_sucursal;
    }
}
