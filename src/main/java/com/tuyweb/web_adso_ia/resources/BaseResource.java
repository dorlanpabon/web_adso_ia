package com.tuyweb.web_adso_ia.resources;

import com.tuyweb.web_adso_ia.util.JpaUtil;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class BaseResource {

  protected EntityManager em = JpaUtil.createEntityManager();

  @PreDestroy
  private void closeEntityManager() {
    if (em != null && em.isOpen()) {
      em.close();
    }
  }

  protected Response executeInTransaction(Supplier<Response> action) {
    EntityTransaction transaction = em.getTransaction();

    try {
      transaction.begin();
      Response response = action.get();

      if (response == null) {
        if (transaction.isActive()) {
          transaction.rollback();
        }
        return internalServerError(new IllegalStateException("No se genero una respuesta valida."));
      }

      if (response.getStatus() >= 400) {
        if (transaction.isActive()) {
          transaction.rollback();
        }
        return response;
      }

      transaction.commit();
      return response;
    } catch (IllegalArgumentException ex) {
      if (transaction.isActive()) {
        transaction.rollback();
      }
      return badRequest(ex.getMessage());
    } catch (Exception ex) {
      if (transaction.isActive()) {
        transaction.rollback();
      }
      return internalServerError(ex);
    }
  }

  protected Response badRequest(String message) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(message(message))
        .build();
  }

  protected Response notFound(String message) {
    return Response.status(Response.Status.NOT_FOUND)
        .entity(message(message))
        .build();
  }

  protected Response conflict(String message) {
    return Response.status(Response.Status.CONFLICT)
        .entity(message(message))
        .build();
  }

  protected Response unauthorized(String message) {
    return Response.status(Response.Status.UNAUTHORIZED)
        .entity(message(message))
        .build();
  }

  protected Response internalServerError(Exception ex) {
    String message = ex.getMessage() == null || ex.getMessage().isBlank()
        ? "Se produjo un error interno."
        : ex.getMessage();

    return Response.serverError()
        .entity(message(message))
        .build();
  }

  protected Map<String, Object> message(String message) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("mensaje", message);
    return response;
  }

  protected boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
