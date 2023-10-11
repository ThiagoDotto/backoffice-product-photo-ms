package br.com.repassa.resource.client;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path(value = "/v1/product")
@RegisterRestClient(configKey = "product-resource")
public interface ProductRestClient {
    @GET
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    Response validateProductId(@QueryParam("productId") String productId, @HeaderParam("Authorization") String token);
}
