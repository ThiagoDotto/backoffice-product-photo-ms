package br.com.repassa.resource;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.DinamicPaginationDTO;
import br.com.repassa.dto.ProductResponseDTO;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.service.PhotosService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Tag(name = "Photos", description = "Gerenciar Photos")
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/v1/photos")
public class PhotoPaginationResource {

    @Inject
    PhotosService photosService;

    @Inject
    JsonWebToken token;

    @GET
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})

    @Operation(summary = "Busca por photos por Paginação", description = "Buscar todas as Photos.")
    @Path("/search")
    public Response getAllPhotos(@QueryParam("date") String date,
                                 @QueryParam("lastEvaluatedKey") String lastEvaluatedKey,
                                 @QueryParam("pageSize") @DefaultValue("10") int pageSize) throws RepassaException {
        try {

            long inicio = System.currentTimeMillis();
            System.out.println("inicio da busca (searchPhotos) de fotos por usuário " + inicio);

            String name = token.getClaim("name");
            DinamicPaginationDTO dinamicPaginationDTO = photosService.searchPhotosPagination(date, name, pageSize, lastEvaluatedKey);

            long fim = System.currentTimeMillis();
            System.out.println("FIM da busca (searchPhotos) de fotos por usuário " + fim);
            System.out.printf("total de segundos %.3f ms%n", (fim - inicio) / 1000d);

            return Response.ok(dinamicPaginationDTO).status(Response.Status.OK).build();
        } catch (RepassaException exception) {
            if (exception.getRepassaUtilError().getErrorCode().equals(PhotoError.PHOTOMANAGER_FINISHED.getErrorCode())) {
                return Response
                        .ok(ProductResponseDTO.builder().message(ProductResponseDTO.PHOTOMANAGER_FINISHED).build())
                        .status(Response.Status.OK)
                        .build();
            }

            throw new RepassaException(exception.getRepassaUtilError());
        }
    }
}
