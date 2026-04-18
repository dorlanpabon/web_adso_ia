package com.tuyweb.web_adso_ia.model;

public enum RolUsuario {
  ADMIN,
  CLIENTE;

  public static RolUsuario fromString(String value) {
    if (value == null || value.isBlank()) {
      return CLIENTE;
    }

    try {
      return RolUsuario.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Rol invalido. Usa ADMIN o CLIENTE.");
    }
  }
}
