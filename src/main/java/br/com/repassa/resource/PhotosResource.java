package br.com.repassa.resource;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.*;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.service.PhotosService;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Photos", description = "Gerenciar Photos")
@Produces()
@Path("/api/v1/photos")
public class PhotosResource {
    @Inject
    PhotosService photosService;

    @Inject
    JsonWebToken token;

    @Context
    HttpHeaders headers;

    @GET
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Busca por photos", description = "Buscar todas as Photos.")
    @Path("/search")
    public PhotosManager getAllPhotos(@QueryParam("date") String date) {
        return photosService.searchPhotos(date, token.getClaim("name"));
    }

    @GET
    @RolesAllowed({"admin", "CADASTRO DE PRODUTOS.CADASTRAR_PRODUTOS", "HISTÓRICO DE PROCESSAMENTO DA SACOLA.VISUALIZAR_DETALHES"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Busca as fotos do produto", description = "Busca as fotos pelo id do produto.")
    @Path("/getByProductId")
    public ProductPhotoListDTO getPhotoByProductId(@QueryParam("productId") String productId) throws RepassaException {
        return photosService.findPhotoByProductId(productId);
    }

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Processa os IDs das fotos", description = "Processa de forma automatica os IDs atraves dos codigos de barra")
    @Path("/processBarCode")
    public PhotosManager processBarCode(@RequestBody ProcessBarCodeRequestDTO req) throws RepassaException {
        String tokenAuth = headers.getHeaderString("Authorization");
        return photosService.processBarCode(req, token.getClaim("name"), tokenAuth);
    }

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar fotos por filtro", description = "As fotos existentes no Bucket, poderá ser buscadas através de filtros pré-configurados.")
    @Path("/filter-and-persist")
    public Response getAllDate(@QueryParam("date") String date) throws RepassaException {
        PhotoFilterDTO filter = new PhotoFilterDTO(date);
        photosService.filterAndPersist(filter, token.getClaim("name"));
        return Response.ok().build();
    }

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Validar ID's do(s) grupo(s) de foto(s)", description = "Irá validar os ID's identificado (da etiqueta) ou inserido manualmente dos grupos de fotos.")
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "ID's Aceito", content = @Content(mediaType = "application/json", schema = @Schema(implementation = IdentificatorsDTO.class, type = SchemaType.ARRAY))),
            @APIResponse(responseCode = "400", description = "A lista informada, contém algum ID Inválido.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = IdentificatorsDTO.class, type = SchemaType.ARRAY), examples = @ExampleObject(name = "Erro de Validação", value = "[{\"productId\":\"123\",\"groupId\":\"456\", \"valid\":false, \"message\":\"O ID 16549 está sendo utilizado por outro Grupo.\"}]")))
    })
    @Path("/validate-identificators")
    public Response validateIds(@RequestBody List<IdentificatorsDTO> identificators) throws Exception {
        String tokenAuth = headers.getHeaderString("Authorization");
        List<IdentificatorsDTO> identificatorsValidated = photosService.validateIdentificators(identificators,
                tokenAuth, false);

        List<IdentificatorsDTO> response = identificatorsValidated.stream()
                .filter(identificator -> !identificator.getValid()).collect(Collectors.toList());

        if (response.isEmpty()) {
            return Response.status(Response.Status.ACCEPTED)
                    .entity(identificatorsValidated)
                    .build();
        }

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(identificatorsValidated)
                .build();
    }

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Finaliza Gerencia de Fotos",
            description = "endpoint usado para finalizar o procosesso de gerencia de fotos")
    @Path("/finish-manager-bags")
    public void finishManagerPhotos(@RequestBody FinishPhotoManagerDTO finishPhotoManagerDTO) throws Exception {
        UserPrincipalDTO userPrincipalDTO = UserPrincipalDTO.builder()
                .id(this.token.getClaim(Claims.sub))
                .email(this.token.getClaim(Claims.email))
                .firtName(this.token.getName())
                .build();
        photosService.finishManagerPhotos(finishPhotoManagerDTO.getId(), userPrincipalDTO, headers);
    }

    @PUT
    @Operation(summary = "Atualizar Tipo da Foto", description = "Endpoint com finalidade para atualizar o Tipo da foto.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "Objecto aceito", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChangeTypePhotoDTO.class, type = SchemaType.ARRAY))),
    })
    @Path("/change-type-photo")
    public Response updateProductStepOne(@RequestBody @Valid ChangeTypePhotoDTO changeTypePhotoDTO)
            throws RepassaException {

        return Response.ok(photosService.changeStatusPhoto(changeTypePhotoDTO)).build();
    }
}
