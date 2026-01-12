package com.phovl.cinemaphovlmobile.model;

public class Asiento {
    private String id;
    private boolean ocupado;

    public Asiento(String id, boolean ocupado) {
        this.id = id;
        this.ocupado = ocupado;
    }

    public String getId() { return id; }
    public boolean isOcupado() { return ocupado; }
}
