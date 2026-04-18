package com.tuyweb.web_adso_ia.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VentaEstado estado;

  @Column(name = "fecha_venta", nullable = false)
  private LocalDateTime fechaVenta;

  @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<VentaDetalle> detalles = new ArrayList<>();

  @PrePersist
  private void prePersist() {
    if (estado == null) {
      estado = VentaEstado.CREADA;
    }
    if (total == null) {
      total = BigDecimal.ZERO;
    }
    if (fechaVenta == null) {
      fechaVenta = LocalDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public VentaEstado getEstado() {
    return estado;
  }

  public void setEstado(VentaEstado estado) {
    this.estado = estado;
  }

  public LocalDateTime getFechaVenta() {
    return fechaVenta;
  }

  public void setFechaVenta(LocalDateTime fechaVenta) {
    this.fechaVenta = fechaVenta;
  }

  public List<VentaDetalle> getDetalles() {
    return detalles;
  }

  public void setDetalles(List<VentaDetalle> detalles) {
    this.detalles = detalles;
  }
}
