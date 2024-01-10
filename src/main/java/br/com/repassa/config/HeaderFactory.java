package br.com.repassa.config;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


@ApplicationScoped
public class HeaderFactory implements ClientHeadersFactory {
    HttpHeaders headers;

    @Inject
    public HeaderFactory(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        System.out.println("header factory ");
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        String authorization = headers.getHeaderString("Authorization");
        result.add("Authorization", authorization);
        return result;
    }
}
