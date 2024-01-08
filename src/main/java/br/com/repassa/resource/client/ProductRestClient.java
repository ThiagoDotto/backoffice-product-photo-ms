package br.com.repassa.resource.client;

import br.com.repassa.config.HeaderFactory;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(value = "/v1/product")
@RegisterRestClient(configKey = "product-resource")
@RegisterClientHeaders(HeaderFactory.class)
public interface ProductRestClient {
    @GET
    @Path("/verifyproduct")
    @Produces(MediaType.APPLICATION_JSON)
    Response verifyProduct(@QueryParam("productId") String productId);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/photographystatus")
    Response updatePhotographyStatus(@QueryParam("productId") Long productId);
}
