package com.tuyweb.web_adso_ia.resources;

import com.tuyweb.web_adso_ia.model.Carrito;
import com.tuyweb.web_adso_ia.model.CarritoEstado;
import com.tuyweb.web_adso_ia.model.CarritoItem;
import com.tuyweb.web_adso_ia.model.Producto;
import com.tuyweb.web_adso_ia.model.Usuario;
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

@Path("carritos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CarritoResource extends BaseResource {

    @GET
    @Path("usuario/{usuarioId}")
    public Response getCarritoByUsuario(@PathParam("usuarioId") Long usuarioId) {
        return executeInTransaction(() -> {
            Usuario usuario = em.find(Usuario.class, usuarioId);
            if (usuario == null) {
                return notFound("Usuario no encontrado.");
            }

            Carrito carrito = findOrCreateOpenCarrito(usuario);
            return Response.ok(DtoMapper.carrito(carrito)).build();
        });
    }

    @POST
    @Path("usuario/{usuarioId}/items")
    public Response addItem(@PathParam("usuarioId") Long usuarioId, AddItemRequest request) {
        if (request == null || request.productoId == null || request.cantidad == null) {
            return badRequest("productoId y cantidad son obligatorios.");
        }

        if (request.cantidad <= 0) {
            return badRequest("La cantidad debe ser mayor que cero.");
        }

        return executeInTransaction(() -> {
            Usuario usuario = em.find(Usuario.class, usuarioId);
            if (usuario == null) {
                return notFound("Usuario no encontrado.");
            }

            Producto producto = em.find(Producto.class, request.productoId);
            if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
                return notFound("Producto no disponible.");
            }

            Carrito carrito = findOrCreateOpenCarrito(usuario);

            CarritoItem itemExistente = carrito.getItems()
                    .stream()
                    .filter(item -> item.getProducto().getId().equals(producto.getId()))
                    .findFirst()
                    .orElse(null);

            int cantidadTotal = request.cantidad;
            if (itemExistente != null) {
                cantidadTotal = itemExistente.getCantidad() + request.cantidad;
            }

            if (cantidadTotal > producto.getStock()) {
                return badRequest("No hay stock suficiente para la cantidad solicitada.");
            }

            if (itemExistente == null) {
                CarritoItem nuevoItem = new CarritoItem();
                nuevoItem.setCarrito(carrito);
                nuevoItem.setProducto(producto);
                nuevoItem.setCantidad(request.cantidad);
                nuevoItem.setPrecioUnitario(producto.getPrecio());
                em.persist(nuevoItem);
                carrito.getItems().add(nuevoItem);
            } else {
                itemExistente.setCantidad(cantidadTotal);
            }

            em.flush();
            Carrito carritoActualizado = findOpenCarritoWithItems(usuarioId);
            return Response.ok(DtoMapper.carrito(carritoActualizado)).build();
        });
    }

    @PUT
    @Path("usuario/{usuarioId}/items/{itemId}")
    public Response updateItem(@PathParam("usuarioId") Long usuarioId,
            @PathParam("itemId") Long itemId,
            UpdateItemRequest request) {
        if (request == null || request.cantidad == null) {
            return badRequest("La cantidad es obligatoria.");
        }

        if (request.cantidad <= 0) {
            return badRequest("La cantidad debe ser mayor que cero.");
        }

        return executeInTransaction(() -> {
            Usuario usuario = em.find(Usuario.class, usuarioId);
            if (usuario == null) {
                return notFound("Usuario no encontrado.");
            }

            Carrito carrito = findOpenCarritoWithItems(usuarioId);
            if (carrito == null) {
                return notFound("Carrito no encontrado.");
            }

            CarritoItem item = carrito.getItems()
                    .stream()
                    .filter(current -> current.getId().equals(itemId))
                    .findFirst()
                    .orElse(null);

            if (item == null) {
                return notFound("Item no encontrado en el carrito.");
            }

            if (request.cantidad > item.getProducto().getStock()) {
                return badRequest("No hay stock suficiente para actualizar el item.");
            }

            item.setCantidad(request.cantidad);
            em.flush();

            Carrito carritoActualizado = findOpenCarritoWithItems(usuarioId);
            return Response.ok(DtoMapper.carrito(carritoActualizado)).build();
        });
    }

    @DELETE
    @Path("usuario/{usuarioId}/items/{itemId}")
    public Response deleteItem(@PathParam("usuarioId") Long usuarioId, @PathParam("itemId") Long itemId) {
        return executeInTransaction(() -> {
            Usuario usuario = em.find(Usuario.class, usuarioId);
            if (usuario == null) {
                return notFound("Usuario no encontrado.");
            }

            Carrito carrito = findOpenCarritoWithItems(usuarioId);
            if (carrito == null) {
                return notFound("Carrito no encontrado.");
            }

            boolean removed = carrito.getItems().removeIf(item -> item.getId().equals(itemId));
            if (!removed) {
                return notFound("Item no encontrado en el carrito.");
            }

            em.flush();
            Carrito carritoActualizado = findOpenCarritoWithItems(usuarioId);
            return Response.ok(DtoMapper.carrito(carritoActualizado)).build();
        });
    }

    @DELETE
    @Path("usuario/{usuarioId}/items")
    public Response clearItems(@PathParam("usuarioId") Long usuarioId) {
        return executeInTransaction(() -> {
            Usuario usuario = em.find(Usuario.class, usuarioId);
            if (usuario == null) {
                return notFound("Usuario no encontrado.");
            }

            Carrito carrito = findOpenCarritoWithItems(usuarioId);
            if (carrito == null) {
                return notFound("Carrito no encontrado.");
            }

            carrito.getItems().clear();
            em.flush();
            return Response.ok(DtoMapper.carrito(carrito)).build();
        });
    }

    private Carrito findOrCreateOpenCarrito(Usuario usuario) {
        Carrito carrito = findOpenCarritoWithItems(usuario.getId());
        if (carrito != null) {
            return carrito;
        }

        Carrito nuevoCarrito = new Carrito();
        nuevoCarrito.setUsuario(usuario);
        nuevoCarrito.setEstado(CarritoEstado.ABIERTO);
        em.persist(nuevoCarrito);
        em.flush();

        return findOpenCarritoWithItems(usuario.getId());
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

    public static class AddItemRequest {
        public Long productoId;
        public Integer cantidad;
    }

    public static class UpdateItemRequest {
        public Integer cantidad;
    }
}
