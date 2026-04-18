package com.tuyweb.web_adso_ia.util;

import com.tuyweb.web_adso_ia.model.Carrito;
import com.tuyweb.web_adso_ia.model.CarritoItem;
import com.tuyweb.web_adso_ia.model.Producto;
import com.tuyweb.web_adso_ia.model.Usuario;
import com.tuyweb.web_adso_ia.model.Venta;
import com.tuyweb.web_adso_ia.model.VentaDetalle;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DtoMapper {

  private DtoMapper() {
  }

  public static Map<String, Object> usuario(Usuario usuario) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", usuario.getId());
    data.put("nombre", usuario.getNombre());
    data.put("correo", usuario.getCorreo());
    data.put("rol", usuario.getRol());
    data.put("creadoEn", usuario.getCreadoEn());
    return data;
  }

  public static Map<String, Object> producto(Producto producto) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", producto.getId());
    data.put("nombre", producto.getNombre());
    data.put("descripcion", producto.getDescripcion());
    data.put("precio", producto.getPrecio());
    data.put("stock", producto.getStock());
    data.put("activo", producto.getActivo());
    data.put("creadoEn", producto.getCreadoEn());
    return data;
  }

  public static Map<String, Object> carritoItem(CarritoItem item) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("itemId", item.getId());
    data.put("productoId", item.getProducto().getId());
    data.put("producto", item.getProducto().getNombre());
    data.put("cantidad", item.getCantidad());
    data.put("precioUnitario", item.getPrecioUnitario());
    data.put("subtotal", item.getSubtotal());
    return data;
  }

  public static Map<String, Object> carrito(Carrito carrito) {
    List<Map<String, Object>> items = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;

    for (CarritoItem item : carrito.getItems()) {
      items.add(carritoItem(item));
      total = total.add(item.getSubtotal());
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("carritoId", carrito.getId());
    data.put("usuarioId", carrito.getUsuario().getId());
    data.put("estado", carrito.getEstado());
    data.put("creadoEn", carrito.getCreadoEn());
    data.put("items", items);
    data.put("total", total);
    return data;
  }

  public static Map<String, Object> ventaDetalle(VentaDetalle detalle) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("detalleId", detalle.getId());
    data.put("productoId", detalle.getProducto().getId());
    data.put("producto", detalle.getNombreProducto());
    data.put("cantidad", detalle.getCantidad());
    data.put("precioUnitario", detalle.getPrecioUnitario());
    data.put("subtotal", detalle.getSubtotal());
    return data;
  }

  public static Map<String, Object> venta(Venta venta) {
    List<Map<String, Object>> detalles = new ArrayList<>();

    for (VentaDetalle detalle : venta.getDetalles()) {
      detalles.add(ventaDetalle(detalle));
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("ventaId", venta.getId());
    data.put("usuarioId", venta.getUsuario().getId());
    data.put("estado", venta.getEstado());
    data.put("fechaVenta", venta.getFechaVenta());
    data.put("total", venta.getTotal());
    data.put("detalles", detalles);
    return data;
  }
}
