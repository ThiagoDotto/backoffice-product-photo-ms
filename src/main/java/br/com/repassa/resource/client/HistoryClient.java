package br.com.repassa.resource.client;

import br.com.backoffice_repassa_utils_lib.dto.history.HistoryDTO;
import br.com.repassa.dto.PhotographyUpdateDTO;
import br.com.repassa.dto.history.HistoryResponseDTO;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/v1/histories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "history-resource")
public interface HistoryClient {


    @POST
    @Path("/update")
    void updateHistory(@RequestBody HistoryDTO historyDTO, @HeaderParam("Authorization") String token);

    @PUT
    @Path("/photographystatus")
    void updatePhotographyhistory(@RequestBody PhotographyUpdateDTO photographyUpdateDTO, @HeaderParam("Authorization") String token);

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @GET
    @Path("/findbag")
    Response getInfoBag(@QueryParam("bagId") String bagId, @HeaderParam("Authorization") String token);

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @GET
    @Path("/find")
    HistoryResponseDTO findHistory(@DefaultValue("0") @RestQuery("page") int page,
                                   @DefaultValue("40") @RestQuery("size") int size,
                                   @QueryParam("bagId") String bagId,
                                   @QueryParam("email") String email,
                                   @QueryParam("statusBag") String statusBag,
                                   @QueryParam("receiptDate") String receiptDate,
                                   @QueryParam("receiptDateSecondary") String receiptDateSecondary,
                                   @QueryParam("partner") String partner,
                                   @QueryParam("photographyStatus") String photographyStatus,
                                   @QueryParam("api") String api,
                                   @HeaderParam("Authorization") String token);
}
