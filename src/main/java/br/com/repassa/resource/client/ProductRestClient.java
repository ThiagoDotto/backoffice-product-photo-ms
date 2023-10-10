package br.com.repassa.resource.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import br.com.repassa.dto.ProductDTO;

@Path(value = "/api/v1/product")
@RegisterRestClient(configKey = "product-resource")
public interface ProductRestClient {
    @GET
    @Operation(summary = "Valida id de produto ", description = "Endpoint usado para validar id de produto.")
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    ProductDTO validateProductId(@QueryParam("productId") String productId);
}
