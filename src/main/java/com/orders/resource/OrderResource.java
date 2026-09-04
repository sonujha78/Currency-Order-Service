package com.orders.resource;

import com.orders.dto.OrderRequestDTO;
import com.orders.dto.OrderResponseDTO;
import com.orders.service.OrderService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/orders")
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(@Valid OrderRequestDTO request) {
        OrderResponseDTO response = orderService.createOrder(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
