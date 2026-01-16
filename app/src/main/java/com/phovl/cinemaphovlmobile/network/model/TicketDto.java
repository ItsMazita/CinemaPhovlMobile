package com.phovl.cinemaphovlmobile.network.model;

public class TicketDto {
    public int id_ticket;
    public int id_usuario;
    public int id_asiento;
    public String codigo_qr;
    public String pelicula; // opcional según tu SELECT
    public String fecha;    // opcional
    public String hora_inicio; // opcional
}
