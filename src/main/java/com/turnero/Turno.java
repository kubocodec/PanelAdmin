package com.turnero;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)

public class Turno {
    private int id;
    private String numero;
    private Categoria categoria;

    // Getters obligatorios
    public int getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public Categoria getCategoria() {
        return categoria;
    }

}
