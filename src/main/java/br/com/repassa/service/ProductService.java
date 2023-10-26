package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.ProductDTO;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.resource.client.ProductRestClient;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class ProductService {

    @Inject
    @RestClient
    ProductRestClient productRestClient;

    public ProductDTO validateProductIDResponse(String productId, String tokenAuth) throws RepassaException {
        try {
            Response response = productRestClient.validateProductId(productId, tokenAuth);
            Log.info("FOUND PRODUCT ID: ");
            return response.readEntity(ProductDTO.class);
        } catch (ClientWebApplicationException e) {
            throw new RepassaException(PhotoError.PRODUCT_ID_INVALIDO);
        }
    }
}
