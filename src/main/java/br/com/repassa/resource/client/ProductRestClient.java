package br.com.repassa.resource.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(value = "/v1/product")
@RegisterRestClient(configKey = "product-resource")
public interface ProductRestClient {
    @GET
    @Path("/verifyproduct")
    @Produces(MediaType.APPLICATION_JSON)
    Response verifyProduct(@QueryParam("productId") String productId, @HeaderParam("Authorization") String token);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/photographystatus")
    Response updatePhotographyStatus(@QueryParam("productId") Long productId, @HeaderParam("Authorization") String token);
}
