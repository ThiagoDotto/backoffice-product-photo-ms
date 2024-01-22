package br.com.repassa.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.resource.client.ProductRestClient;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ProductService {
    @RestClient
    ProductRestClient productRestClient;

    public void verifyProduct(String productId) throws RepassaException {
        try {
            Log.info("Verificando se o produto existe no microsservico product-ms");
            productRestClient.verifyProduct(productId);
        } catch (ClientWebApplicationException e) {
            throw new RepassaException(PhotoError.PRODUCT_ID_INVALIDO);
        }
    }
}
