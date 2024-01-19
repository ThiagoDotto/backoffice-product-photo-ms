package br.com.repassa.resource.client;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/findproducts")
    Response findBagsForProduct(@DefaultValue("0") @RestQuery("page") int page,
                                @DefaultValue("40") @RestQuery("size") int size,
                                @QueryParam("bagId") String bagId,
                                @HeaderParam("Authorization") String token);

}
