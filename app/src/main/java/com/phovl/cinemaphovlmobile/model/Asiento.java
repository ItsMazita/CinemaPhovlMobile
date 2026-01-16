package com.phovl.cinemaphovlmobile.model;

import java.util.Objects;

public class Asiento {
    private int idAsientoDb;   // id_asiento en la BD
    private String label;      // "A1" para mostrar
    private boolean ocupado;

    public Asiento(int idAsientoDb, String label, boolean ocupado) {
        this.idAsientoDb = idAsientoDb;
        this.label = label;
        this.ocupado = ocupado;
    }

    public int getIdAsientoDb() { return idAsientoDb; }
    public String getId() { return label; }
    public boolean isOcupado() { return ocupado; }
    public void setOcupado(boolean ocupado) { this.ocupado = ocupado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asiento)) return false;
        Asiento asiento = (Asiento) o;
        return idAsientoDb == asiento.idAsientoDb;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAsientoDb);
    }
}
