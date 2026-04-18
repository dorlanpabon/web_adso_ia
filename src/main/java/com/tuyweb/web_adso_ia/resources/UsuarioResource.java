package com.tuyweb.web_adso_ia.resources;

import com.tuyweb.web_adso_ia.model.Carrito;
import com.tuyweb.web_adso_ia.model.RolUsuario;
import com.tuyweb.web_adso_ia.model.Usuario;
import com.tuyweb.web_adso_ia.util.DtoMapper;
import com.tuyweb.web_adso_ia.util.PasswordUtil;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Path("usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioResource extends BaseResource {

  @GET
  public Response getAll() {
    List<Map<String, Object>> usuarios = em.createQuery("SELECT u FROM Usuario u ORDER BY u.id", Usuario.class)
        .getResultList()
        .stream()
        .map(DtoMapper::usuario)
        .collect(Collectors.toList());

    return Response.ok(usuarios).build();
  }

  @GET
  @Path("{id}")
  public Response getById(@PathParam("id") Long id) {
    Usuario usuario = em.find(Usuario.class, id);
    if (usuario == null) {
      return notFound("Usuario no encontrado.");
    }

    return Response.ok(DtoMapper.usuario(usuario)).build();
  }

  @PUT
  @Path("{id}")
  public Response update(@PathParam("id") Long id, UpdateUsuarioRequest request) {
    if (request == null) {
      return badRequest("No se recibieron datos para actualizar.");
    }

    return executeInTransaction(() -> {
      Usuario usuario = em.find(Usuario.class, id);
      if (usuario == null) {
        return notFound("Usuario no encontrado.");
      }

      if (!isBlank(request.nombre)) {
        usuario.setNombre(request.nombre.trim());
      }

      if (!isBlank(request.correo)) {
        String correoNormalizado = request.correo.trim().toLowerCase(Locale.ROOT);
        Usuario usuarioConCorreo = findByCorreo(correoNormalizado);
        if (usuarioConCorreo != null && !usuarioConCorreo.getId().equals(usuario.getId())) {
          return conflict("El correo ya esta en uso por otro usuario.");
        }
        usuario.setCorreo(correoNormalizado);
      }

      if (!isBlank(request.rol)) {
        usuario.setRol(RolUsuario.fromString(request.rol));
      }

      if (!isBlank(request.password)) {
        usuario.setPasswordHash(PasswordUtil.hashPassword(request.password));
      }

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("mensaje", "Usuario actualizado correctamente.");
      response.put("usuario", DtoMapper.usuario(usuario));
      return Response.ok(response).build();
    });
  }

  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") Long id) {
    return executeInTransaction(() -> {
      Usuario usuario = em.find(Usuario.class, id);
      if (usuario == null) {
        return notFound("Usuario no encontrado.");
      }

      Long ventas = em.createQuery("SELECT COUNT(v) FROM Venta v WHERE v.usuario.id = :usuarioId", Long.class)
          .setParameter("usuarioId", id)
          .getSingleResult();

      if (ventas > 0) {
        return conflict("No se puede eliminar un usuario con ventas asociadas.");
      }

      List<Carrito> carritos = em.createQuery("SELECT c FROM Carrito c WHERE c.usuario.id = :usuarioId", Carrito.class)
          .setParameter("usuarioId", id)
          .getResultList();

      for (Carrito carrito : carritos) {
        em.remove(carrito);
      }

      em.remove(usuario);
      return Response.ok(message("Usuario eliminado correctamente.")).build();
    });
  }

  private Usuario findByCorreo(String correo) {
    return em.createQuery("SELECT u FROM Usuario u WHERE lower(u.correo) = :correo", Usuario.class)
        .setParameter("correo", correo.toLowerCase(Locale.ROOT))
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  public static class UpdateUsuarioRequest {
    public String nombre;
    public String correo;
    public String rol;
    public String password;
  }
}
