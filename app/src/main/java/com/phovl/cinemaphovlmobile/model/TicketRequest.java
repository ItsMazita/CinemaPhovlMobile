package com.phovl.cinemaphovlmobile.model;

/*
 Request para comprar ticket:
 { id_funcion, id_asiento, id_usuario }
*/
public class TicketRequest {
    private int id_funcion;
    private int id_asiento;
    private int id_usuario;

    public TicketRequest(int id_funcion, int id_asiento, int id_usuario) {
        this.id_funcion = id_funcion;
        this.id_asiento = id_asiento;
        this.id_usuario = id_usuario;
    }

    public int getId_funcion() {
        return id_funcion;
    }

    public int getId_asiento() {
        return id_asiento;
    }

    public int getId_usuario() {
        return id_usuario;
    }
}
