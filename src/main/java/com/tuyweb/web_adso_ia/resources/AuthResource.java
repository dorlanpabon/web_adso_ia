package com.tuyweb.web_adso_ia.resources;

import com.tuyweb.web_adso_ia.model.Carrito;
import com.tuyweb.web_adso_ia.model.CarritoEstado;
import com.tuyweb.web_adso_ia.model.RolUsuario;
import com.tuyweb.web_adso_ia.model.Usuario;
import com.tuyweb.web_adso_ia.util.DtoMapper;
import com.tuyweb.web_adso_ia.util.PasswordUtil;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Path("auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource extends BaseResource {

  @POST
  @Path("register")
  public Response register(RegisterRequest request) {
    if (request == null || isBlank(request.nombre) || isBlank(request.correo) || isBlank(request.password)) {
      return badRequest("nombre, correo y password son obligatorios.");
    }

    return executeInTransaction(() -> {
      String correoNormalizado = request.correo.trim().toLowerCase(Locale.ROOT);
      Usuario existente = findByCorreo(correoNormalizado);

      if (existente != null) {
        return conflict("El correo ya esta registrado.");
      }

      Usuario usuario = new Usuario();
      usuario.setNombre(request.nombre.trim());
      usuario.setCorreo(correoNormalizado);
      usuario.setPasswordHash(PasswordUtil.hashPassword(request.password));
      usuario.setRol(RolUsuario.fromString(request.rol));
      em.persist(usuario);

      Carrito carrito = new Carrito();
      carrito.setUsuario(usuario);
      carrito.setEstado(CarritoEstado.ABIERTO);
      em.persist(carrito);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("mensaje", "Usuario registrado correctamente.");
      response.put("usuario", DtoMapper.usuario(usuario));
      response.put("carritoId", carrito.getId());

      return Response.status(Response.Status.CREATED)
          .entity(response)
          .build();
    });
  }

  @POST
  @Path("login")
  public Response login(LoginRequest request) {
    if (request == null || isBlank(request.correo) || isBlank(request.password)) {
      return badRequest("correo y password son obligatorios.");
    }

    String correoNormalizado = request.correo.trim().toLowerCase(Locale.ROOT);
    Usuario usuario = findByCorreo(correoNormalizado);

    if (usuario == null || !PasswordUtil.verifyPassword(request.password, usuario.getPasswordHash())) {
      return unauthorized("Credenciales invalidas.");
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("mensaje", "Inicio de sesion exitoso.");
    response.put("usuario", DtoMapper.usuario(usuario));

    return Response.ok(response).build();
  }

  private Usuario findByCorreo(String correo) {
    return em.createQuery("SELECT u FROM Usuario u WHERE lower(u.correo) = :correo", Usuario.class)
        .setParameter("correo", correo.toLowerCase(Locale.ROOT))
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  public static class RegisterRequest {
    public String nombre;
    public String correo;
    public String password;
    public String rol;
  }

  public static class LoginRequest {
    public String correo;
    public String password;
  }
}
