package br.com.repassa.resource;

import br.com.repassa.service.healthcheck.HealthCheckService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/health")
@Slf4j
public class HealthCheckResource {

    @Inject
    HealthCheckService healthCheckService;

    @GET
    public Response checkHealth() {
        log.info("Verificando se o servico esta ativo");
        HealthCheckResponse response = healthCheckService.call();
        if (response.getStatus().equals(HealthCheckResponse.Status.UP)) {
            return Response.noContent().build();
        } else {
            return Response.serverError().build();
        }
    }
}
