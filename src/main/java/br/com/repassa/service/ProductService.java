package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
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

    public void verifyProduct(String productId, String tokenAuth) throws RepassaException {
        try {
            Log.info("Verificando se o produto existe no microsservico product-ms");
            productRestClient.verifyProduct(productId, tokenAuth);
        } catch (ClientWebApplicationException e) {
            throw new RepassaException(PhotoError.PRODUCT_ID_INVALIDO);
        }
    }
}
