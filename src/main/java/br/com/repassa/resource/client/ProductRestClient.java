package br.com.repassa.resource.client;

import br.com.repassa.config.HeaderFactory;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/findproducts")
    Response findBagsForProduct(@DefaultValue("0") @RestQuery("page") int page,
                                @DefaultValue("40") @RestQuery("size") int size,
                                @QueryParam("bagId") String bagId);
}
