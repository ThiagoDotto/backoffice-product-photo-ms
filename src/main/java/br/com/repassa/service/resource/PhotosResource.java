package br.com.repassa.service.resource;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import br.com.repassa.service.entity.PhotosManager;
import br.com.repassa.service.dto.PhotoFilterDTO;
import br.com.repassa.service.service.PhotosService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;

@Tag(name = "Photos", description = "Gerenciar Photos")
@Path("/api/v1/photos")
public class PhotosResource {
    @Inject
    PhotosService photosService;

    @Inject
    JsonWebToken token;

    @GET
    @RolesAllowed({ "admin", "FOTOGRAFIA.GERENCIAR_FOTOS" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Busca por photos", description = "Buscar todas as Photos.")
    @Path("/search")
    public Response getAllPhotos(@QueryParam("date") String date) {
        return Response.ok(photosService.searchPhotos(date, token.getClaim("name"))).build();
    }

    @POST
    @RolesAllowed({ "admin", "FOTOGRAFIA.GERENCIAR_FOTOS" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar fotos por filtro", description = "As fotos existentes no Bucket, poderá ser buscadas através de filtros pré-configurados.")
    @Path("/filterAndPersist")
    public Response getAllDate(@QueryParam("date") String date) throws RepassaException {
        PhotoFilterDTO filter = new PhotoFilterDTO(date);
        photosService.filterAndPersist(filter, token.getClaim("name"));
        return Response.ok().build();
    }
}
