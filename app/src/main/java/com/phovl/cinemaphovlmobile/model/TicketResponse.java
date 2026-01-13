package com.phovl.cinemaphovlmobile.model;

/*
 Respuesta del endpoint /api/tickets/comprar:
 { message: "Compra exitosa", qr: "..." }
*/
public class TicketResponse {
    private String message;
    private String qr;

    public String getMessage() {
        return message;
    }

    public String getQr() {
        return qr;
    }
}
