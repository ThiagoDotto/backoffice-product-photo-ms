package br.com.repassa.resource.client;

import br.com.backoffice_repassa_utils_lib.dto.history.HistoryDTO;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/v1/histories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "history-resource")
public interface HistoryClient {


    @POST
    @Path("/update")
    void updateHistory(@RequestBody HistoryDTO historyDTO);

}
