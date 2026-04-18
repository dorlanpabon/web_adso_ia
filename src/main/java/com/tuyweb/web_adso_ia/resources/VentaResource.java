package com.tuyweb.web_adso_ia.resources;

import com.tuyweb.web_adso_ia.model.Carrito;
import com.tuyweb.web_adso_ia.model.CarritoEstado;
import com.tuyweb.web_adso_ia.model.CarritoItem;
import com.tuyweb.web_adso_ia.model.Usuario;
import com.tuyweb.web_adso_ia.model.Venta;
import com.tuyweb.web_adso_ia.model.VentaDetalle;
import com.tuyweb.web_adso_ia.model.VentaEstado;
import com.tuyweb.web_adso_ia.util.DtoMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("ventas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class VentaResource extends BaseResource {

  @GET
  public Response getAll() {
    List<Map<String, Object>> ventas = em.createQuery(
        "SELECT DISTINCT v FROM Venta v JOIN FETCH v.usuario LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto ORDER BY v.id DESC",
        Venta.class)
        .getResultList()
        .stream()
        .map(DtoMapper::venta)
        .collect(Collectors.toList());

    return Response.ok(ventas).build();
  }

  @GET
  @Path("{ventaId}")
  public Response getById(@PathParam("ventaId") Long ventaId) {
    Venta venta = findVentaConDetalles(ventaId);
    if (venta == null) {
      return notFound("Venta no encontrada.");
    }

    return Response.ok(DtoMapper.venta(venta)).build();
  }

  @GET
  @Path("usuario/{usuarioId}")
  public Response getByUsuario(@PathParam("usuarioId") Long usuarioId) {
    List<Map<String, Object>> ventas = em.createQuery(
        "SELECT DISTINCT v FROM Venta v JOIN FETCH v.usuario LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.usuario.id = :usuarioId ORDER BY v.id DESC",
        Venta.class)
        .setParameter("usuarioId", usuarioId)
        .getResultList()
        .stream()
        .map(DtoMapper::venta)
        .collect(Collectors.toList());

    return Response.ok(ventas).build();
  }

  @POST
  @Path("usuario/{usuarioId}/checkout")
  public Response checkout(@PathParam("usuarioId") Long usuarioId) {
    return executeInTransaction(() -> {
      Usuario usuario = em.find(Usuario.class, usuarioId);
      if (usuario == null) {
        return notFound("Usuario no encontrado.");
      }

      Carrito carrito = findOpenCarritoWithItems(usuarioId);
      if (carrito == null || carrito.getItems().isEmpty()) {
        return badRequest("El carrito esta vacio.");
      }

      for (CarritoItem item : carrito.getItems()) {
        if (!Boolean.TRUE.equals(item.getProducto().getActivo())) {
          return badRequest("El producto " + item.getProducto().getNombre() + " no esta activo.");
        }
        if (item.getCantidad() > item.getProducto().getStock()) {
          return badRequest("Stock insuficiente para el producto " + item.getProducto().getNombre() + ".");
        }
      }

      Venta venta = new Venta();
      venta.setUsuario(usuario);
      venta.setEstado(VentaEstado.CREADA);
      venta.setTotal(BigDecimal.ZERO);
      em.persist(venta);

      BigDecimal total = BigDecimal.ZERO;

      for (CarritoItem item : carrito.getItems()) {
        item.getProducto().setStock(item.getProducto().getStock() - item.getCantidad());

        VentaDetalle detalle = new VentaDetalle();
        detalle.setVenta(venta);
        detalle.setProducto(item.getProducto());
        detalle.setNombreProducto(item.getProducto().getNombre());
        detalle.setCantidad(item.getCantidad());
        detalle.setPrecioUnitario(item.getPrecioUnitario());
        detalle.setSubtotal(item.getSubtotal());
        em.persist(detalle);

        venta.getDetalles().add(detalle);
        total = total.add(detalle.getSubtotal());
      }

      venta.setTotal(total);
      carrito.setEstado(CarritoEstado.CERRADO);
      carrito.getItems().clear();

      Carrito nuevoCarrito = new Carrito();
      nuevoCarrito.setUsuario(usuario);
      nuevoCarrito.setEstado(CarritoEstado.ABIERTO);
      em.persist(nuevoCarrito);

      em.flush();

      Venta ventaCreada = findVentaConDetalles(venta.getId());
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("mensaje", "Venta generada correctamente.");
      response.put("venta", DtoMapper.venta(ventaCreada));
      response.put("nuevoCarritoId", nuevoCarrito.getId());

      return Response.status(Response.Status.CREATED)
          .entity(response)
          .build();
    });
  }

  @PUT
  @Path("{ventaId}/estado")
  public Response updateEstado(@PathParam("ventaId") Long ventaId, EstadoRequest request) {
    if (request == null || isBlank(request.estado)) {
      return badRequest("El estado es obligatorio.");
    }

    return executeInTransaction(() -> {
      Venta venta = findVentaConDetalles(ventaId);
      if (venta == null) {
        return notFound("Venta no encontrada.");
      }

      VentaEstado nuevoEstado = VentaEstado.fromString(request.estado);

      if (venta.getEstado() == VentaEstado.CANCELADA && nuevoEstado != VentaEstado.CANCELADA) {
        return badRequest("No se puede cambiar una venta cancelada a otro estado.");
      }

      if (venta.getEstado() != VentaEstado.CANCELADA && nuevoEstado == VentaEstado.CANCELADA) {
        for (VentaDetalle detalle : venta.getDetalles()) {
          detalle.getProducto().setStock(detalle.getProducto().getStock() + detalle.getCantidad());
        }
      }

      venta.setEstado(nuevoEstado);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("mensaje", "Estado de venta actualizado correctamente.");
      response.put("venta", DtoMapper.venta(venta));
      return Response.ok(response).build();
    });
  }

  @DELETE
  @Path("{ventaId}")
  public Response delete(@PathParam("ventaId") Long ventaId) {
    return executeInTransaction(() -> {
      Venta venta = findVentaConDetalles(ventaId);
      if (venta == null) {
        return notFound("Venta no encontrada.");
      }

      if (venta.getEstado() != VentaEstado.CANCELADA) {
        return badRequest("Solo se pueden eliminar ventas en estado CANCELADA.");
      }

      em.remove(venta);
      return Response.ok(message("Venta eliminada correctamente.")).build();
    });
  }

  private Venta findVentaConDetalles(Long ventaId) {
    return em.createQuery(
        "SELECT DISTINCT v FROM Venta v JOIN FETCH v.usuario LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.id = :ventaId",
        Venta.class)
        .setParameter("ventaId", ventaId)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  private Carrito findOpenCarritoWithItems(Long usuarioId) {
    return em.createQuery(
        "SELECT DISTINCT c FROM Carrito c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.producto WHERE c.usuario.id = :usuarioId AND c.estado = :estado",
        Carrito.class)
        .setParameter("usuarioId", usuarioId)
        .setParameter("estado", CarritoEstado.ABIERTO)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  public static class EstadoRequest {
    public String estado;
  }
}
