package com.tuyweb.web_adso_ia.resources;

import com.tuyweb.web_adso_ia.model.Producto;
import com.tuyweb.web_adso_ia.util.DtoMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("productos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProductoResource extends BaseResource {

  @GET
  public Response getAll(@QueryParam("soloActivos") @DefaultValue("false") boolean soloActivos) {
    String jpql = soloActivos
        ? "SELECT p FROM Producto p WHERE p.activo = true ORDER BY p.id"
        : "SELECT p FROM Producto p ORDER BY p.id";

    List<Map<String, Object>> productos = em.createQuery(jpql, Producto.class)
        .getResultList()
        .stream()
        .map(DtoMapper::producto)
        .collect(Collectors.toList());

    return Response.ok(productos).build();
  }

  @GET
  @Path("{id}")
  public Response getById(@PathParam("id") Long id) {
    Producto producto = em.find(Producto.class, id);
    if (producto == null) {
      return notFound("Producto no encontrado.");
    }
    return Response.ok(DtoMapper.producto(producto)).build();
  }

  @POST
  public Response create(ProductoRequest request) {
    if (request == null || isBlank(request.nombre) || request.precio == null || request.stock == null) {
      return badRequest("nombre, precio y stock son obligatorios.");
    }

    if (request.precio.compareTo(BigDecimal.ZERO) <= 0) {
      return badRequest("El precio debe ser mayor que cero.");
    }

    if (request.stock < 0) {
      return badRequest("El stock no puede ser negativo.");
    }

    return executeInTransaction(() -> {
      Producto producto = new Producto();
      producto.setNombre(request.nombre.trim());
      producto.setDescripcion(request.descripcion);
      producto.setPrecio(request.precio);
      producto.setStock(request.stock);
      producto.setActivo(request.activo == null ? Boolean.TRUE : request.activo);
      em.persist(producto);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("mensaje", "Producto creado correctamente.");
      response.put("producto", DtoMapper.producto(producto));
      return Response.status(Response.Status.CREATED)
          .entity(response)
          .build();
    });
  }

  @PUT
  @Path("{id}")
  public Response update(@PathParam("id") Long id, ProductoRequest request) {
    if (request == null) {
      return badRequest("No se recibieron datos para actualizar.");
    }

    return executeInTransaction(() -> {
      Producto producto = em.find(Producto.class, id);
      if (producto == null) {
        return notFound("Producto no encontrado.");
      }

      if (!isBlank(request.nombre)) {
        producto.setNombre(request.nombre.trim());
      }

      if (request.descripcion != null) {
        producto.setDescripcion(request.descripcion);
      }

      if (request.precio != null) {
        if (request.precio.compareTo(BigDecimal.ZERO) <= 0) {
          return badRequest("El precio debe ser mayor que cero.");
        }
        producto.setPrecio(request.precio);
      }

      if (request.stock != null) {
        if (request.stock < 0) {
          return badRequest("El stock no puede ser negativo.");
        }
        producto.setStock(request.stock);
      }

      if (request.activo != null) {
        producto.setActivo(request.activo);
      }

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("mensaje", "Producto actualizado correctamente.");
      response.put("producto", DtoMapper.producto(producto));
      return Response.ok(response).build();
    });
  }

  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") Long id) {
    return executeInTransaction(() -> {
      Producto producto = em.find(Producto.class, id);
      if (producto == null) {
        return notFound("Producto no encontrado.");
      }

      producto.setActivo(Boolean.FALSE);
      return Response.ok(message("Producto inactivado correctamente.")).build();
    });
  }

  public static class ProductoRequest {
    public String nombre;
    public String descripcion;
    public BigDecimal precio;
    public Integer stock;
    public Boolean activo;
  }
}
