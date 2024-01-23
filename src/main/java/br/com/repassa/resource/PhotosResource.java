package br.com.repassa.resource;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.*;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.service.PhotosService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.jboss.resteasy.reactive.RestQuery;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Photos", description = "Gerenciar Photos")
@Path("/api/v1/photos")
public class PhotosResource {
    @Inject
    PhotosService photosService;

    @Inject
    JsonWebToken token;

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Adiciona nova imagem",
            description = "endpoint usado para adicionar uma nova imagem.")
    public Response insertImage(@Valid @RequestBody ImageDTO image) throws RepassaException {
        return Response.ok(photosService.insertImage(image, token.getClaim("name"))).build();
    }

    @GET
//    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Busca por photos", description = "Buscar todas as Photos.")
    @Path("/search")
    public Response getAllPhotos(@QueryParam("date") String date,
                                 @QueryParam("lastEvaluatedKey") String lastEvaluatedKey,
                                 @QueryParam("pageSize") @DefaultValue("10") int pageSize) throws RepassaException {
        try {

            long inicio = System.currentTimeMillis();
            System.out.println("inicio da busca (searchPhotos) de fotos por usuário " + inicio);

            DinamicPaginationDTO dinamicPaginationDTO = photosService.searchPhotosPagination(date, token.getClaim("name"),  pageSize, lastEvaluatedKey);
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

    @GET
    @Operation(summary = "Buscar Sacolas no historico", description = "Endpoint usado para buscar sacolas por filtro.")
    @Path("/findbags")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "CADASTRO DE PRODUTOS.CONSULTAR_SACOLAS"})
    public Response findBagsForProduct(@DefaultValue("0") @RestQuery("page") int page,
                                       @DefaultValue("40") @RestQuery("size") int size,
                                       @QueryParam("bagId") String bagId,
                                       @QueryParam("email") String email,
                                       @QueryParam("statusBag") String statusBag,
                                       @QueryParam("receiptDate") String receiptDate,
                                       @QueryParam("receiptDateSecondary") String receiptDateSecondary,
                                       @QueryParam("partner") String partner,
                                       @QueryParam("photographyStatus") String photographyStatus) throws RepassaException {
        return Response.ok(photosService.findBagsForPhoto(page, size, bagId, email, statusBag, receiptDate, receiptDateSecondary, partner, photographyStatus)).build();
    }

    @GET
    @Operation(summary = "Buscar produtos por sacola", description = "Endpoint usado para buscar produtos presentes em uma sacola.")
    @Path("/findproducts")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "FOTOGRAFIA.CONSULTAR_SACOLAS"})
    public Response findBagsForProduct(@DefaultValue("0") @RestQuery("page") int page,
                                       @DefaultValue("40") @RestQuery("size") int size,
                                       @QueryParam("bagId") String bagId) throws RepassaException, JsonProcessingException {
        return Response.ok(photosService.findProductsByBagId(page, size, bagId)).build();
    }

    @GET
    @RolesAllowed({"admin", "CADASTRO DE PRODUTOS.CADASTRAR_PRODUTOS", "HISTÓRICO DE PROCESSAMENTO DA SACOLA.VISUALIZAR_DETALHES"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Busca as fotos do produto", description = "Busca as fotos pelo id do produto.")
    @Path("/getbyproductid")
    public ProductPhotoListDTO getPhotoByProductId(@QueryParam("productId") String productId) throws RepassaException {
        return photosService.findPhotoByProductId(productId);
    }

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Processa os IDs das fotos", description = "Processa de forma automatica os IDs atraves dos codigos de barra")
    @Path("/processBarCode")
    public PhotosManager processBarCode(@RequestBody ProcessBarCodeRequestDTO req) throws RepassaException {
        return photosService.processBarCode(req, token.getClaim("name"));
    }

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar fotos por filtro", description = "As fotos existentes no Bucket, poderá ser buscadas através de filtros pré-configurados.")
    @Path("/filter-and-persist")
    public Response getAllDate(@QueryParam("date") String date) throws RepassaException {
        PhotoFilterDTO filter = new PhotoFilterDTO(date);
        long inicio = System.currentTimeMillis();
        System.out.println("inicio da busca (filter-and-persist) de fotos por usuário " + inicio);
        photosService.filterAndPersist(filter, token.getClaim("name"));
        long fim = System.currentTimeMillis();
        System.out.println("FIM da busca (filter-and-persist) de fotos por usuário " + fim);
        System.out.println("total " + (fim - inicio));
        return Response.ok().build();
    }

    @POST
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Validar ID's do(s) grupo(s) de foto(s)", description = "Irá validar os ID's identificado (da etiqueta) ou inserido manualmente dos grupos de fotos.")
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "ID's Aceito",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdentificatorsDTO.class, type = SchemaType.ARRAY))),
            @APIResponse(responseCode = "400", description = "A lista informada, contém algum ID Inválido.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdentificatorsDTO.class, type = SchemaType.ARRAY),
                            examples = @ExampleObject(name = "Erro de Validação", value = "[{\"productId\":\"123\",\"groupId\":\"456\", \"valid\":false, \"message\":\"O ID 16549 está sendo utilizado por outro Grupo.\"}]")))
    })
    @Path("/validate-identificators")
    public Response validateIds(@RequestBody List<IdentificatorsDTO> identificators) throws Exception {
        List<IdentificatorsDTO> identificatorsValidated = photosService.validateIdentificators(identificators,
                false);

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
            description = "endpoint usado para finalizar o processo de gerencia de fotos")
    @Path("/finish-manager-bags")
    public void finishManagerPhotos(@RequestBody FinishPhotoManagerDTO finishPhotoManagerDTO) throws Exception {
        UserPrincipalDTO userPrincipalDTO = UserPrincipalDTO.builder()
                .id(this.token.getClaim(Claims.sub))
                .email(this.token.getClaim(Claims.email))
                .firtName(this.token.getName())
                .build();
        photosService.finishManager(finishPhotoManagerDTO.getId(), userPrincipalDTO);
    }

    @PUT
    @Operation(summary = "Atualizar Tipo da Foto", description = "Endpoint com finalidade para atualizar o Tipo da foto.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "Objecto aceito", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChangeTypePhotoDTO.class, type = SchemaType.ARRAY))),
    })
    @Path("/change-type-photo")
    public Response updateProductStepOne(@RequestBody @Valid ChangeTypePhotoDTO changeTypePhotoDTO)
            throws RepassaException {

        return Response.ok(photosService.changeStatusPhoto(changeTypePhotoDTO)).build();
    }

    @DELETE
    @Operation(summary = "Deleta uma imagem do S3 e Dynamo", description = "Endpoint com finalidade para deletar a foto.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    public Response deletePhoto(@QueryParam("idPhoto") String idPhoto) throws Exception {
        UserPrincipalDTO userPrincipalDTO = UserPrincipalDTO.builder()
                .id(this.token.getClaim(Claims.sub))
                .email(this.token.getClaim(Claims.email))
                .firtName(this.token.getClaim("name"))
                .build();
        photosService.deletePhoto(idPhoto, userPrincipalDTO);
        return Response.ok().build();
    }

    @DELETE
    @Operation(summary = "Deleta os Grupos do Foto", description = "Endpoint com finalidade para deletar a grupo de foto.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @Path("/group")
    public Response deleteGroupsOfPhoto(@QueryParam("groupId") String groupId) throws Exception {
        photosService.deleteGroupsOfPhoto(groupId);
        return Response.ok().build();
    }

    @DELETE
    @Operation(summary = "Deleta os Grupos do Foto", description = "Endpoint com finalidade para deletar todos os grupos vinculado no PhotoManager.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "FOTOGRAFIA.GERENCIAR_FOTOS"})
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "Objecto aceito", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeleteGroupPhotosDTO.class, type = SchemaType.ARRAY))),
    })
    @Path("/photo-manager")
    public Response deletePhotoManager(@QueryParam("photoManagerId") String photoManagerId) throws Exception {
        photosService.deletePhotoManager(photoManagerId);
        return Response.ok().build();
    }
}
