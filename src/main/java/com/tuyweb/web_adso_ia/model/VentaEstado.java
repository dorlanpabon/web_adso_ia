package com.tuyweb.web_adso_ia.model;

public enum VentaEstado {
    CREADA,
    PAGADA,
    CANCELADA;

    public static VentaEstado fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El estado de venta es obligatorio.");
        }

        try {
            return VentaEstado.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado invalido. Usa CREADA, PAGADA o CANCELADA.");
        }
    }
}
