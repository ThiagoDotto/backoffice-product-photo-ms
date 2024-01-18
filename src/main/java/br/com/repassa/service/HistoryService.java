package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.FinishedTriageDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.HistoryDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.ProductDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.PhotoDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.PhotographyDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.StepDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.UserSystem;
import br.com.backoffice_repassa_utils_lib.dto.history.enums.BagStatus;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.PhotographyUpdateDTO;
import br.com.repassa.dto.history.HistoryResponseDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.resource.client.HistoryClient;
import io.quarkus.runtime.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Slf4j
public class HistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);
    private static final String AUTHORIZATION = "Authorization";

    @RestClient
    HistoryClient historyClient;

    @Context
    HttpHeaders headers;

    public void save(PhotosManager photosManager, UserPrincipalDTO loggerUser, HttpHeaders headers) throws RepassaException {

        List<GroupPhotos> groupPhotos = photosManager.getGroupPhotos();
        List<HistoryDTO> histories = new ArrayList<>();

        List<HistoryDTO> historyDTOS = historiesObjsBuilder(loggerUser, groupPhotos, histories);
        LOGGER.debug("Salvando no History as photos");
        historyDTOS.forEach(historyDTO -> historyClient.updateHistory(historyDTO,headers.getHeaderString(AUTHORIZATION)));
    }

    public HistoryDTO findHistoryForBagId(String bagId) throws RepassaException {
        String tokenAuth = headers.getHeaderString(AUTHORIZATION);
        Response historyResponse = historyClient.getInfoBag(bagId, tokenAuth);
        isEmpty(historyResponse);
        return historyResponse.readEntity(HistoryDTO.class);
    }

    public HistoryResponseDTO findHistorys(int page, int size, String bagId, String email, String statusBag, String receiptDate, String receiptDateSecundary, String partner, String photographyStatus, String api){
        return historyClient.findHistory(page, size, bagId, email, statusBag, receiptDate, receiptDateSecundary,
                partner, photographyStatus, api, headers.getHeaderString(AUTHORIZATION));
    }

    private void isEmpty(Response historyResponse) throws RepassaException {
        log.info("Validando se o bagId existe no banco de dados");
        if (Response.Status.NO_CONTENT.getStatusCode() == historyResponse.getStatus()) {
            log.info("bagId nao foi encontrado, codigo invalido");
            throw new RepassaException(PhotoError.SACOLA_NAO_ENCONTRADA);
        }
        if (Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() == historyResponse.getStatus()) {
            log.info("Erro ao acessar o history-ms");
            throw new RepassaException(PhotoError.ERRO_AO_BUSCAR_SACOLAS);
        }
    }

    private List<HistoryDTO> historiesObjsBuilder(UserPrincipalDTO loggerUser, List<GroupPhotos> groupPhotos, List<HistoryDTO> histories) throws RepassaException {
        HistoryDTO historyDTO = new HistoryDTO();

        for(GroupPhotos group : groupPhotos){
            String productId = group.getProductId();
            String bagID = productId.substring(0, productId.length() - 3);

            if(!bagID.equalsIgnoreCase(String.valueOf(historyDTO.getBagId()))){
                historyDTO = findHistoryForBagId(bagID);
            }

            List<ProductDTO> productDTOS;
            if(Objects.isNull(historyDTO.getStepDTO()) || Objects.isNull(historyDTO.getStepDTO().getPhotographs()) || Objects.isNull(historyDTO.getStepDTO().getPhotographs().getProducts())){
                productDTOS = new ArrayList<>();
            }else{
                productDTOS = historyDTO.getStepDTO().getPhotographs().getProducts();
            }

            List<Photo> photos = group.getPhotos();
            List<PhotoDTO> foto = new ArrayList<>();
            photos.forEach(photo -> {
                PhotoDTO photoDTO = new PhotoDTO();
                photoDTO.setName(photo.getNamePhoto());
                photoDTO.setId(photo.getId());
                photoDTO.setType(photo.getTypePhoto().toString());
                photoDTO.setUrl(photo.getUrlPhoto());
                foto.add(photoDTO);
            });

            ProductDTO productDTO = new ProductDTO();
            productDTO.setProductId(Long.valueOf(productId));
            productDTO.setPhotos(foto);
            productDTOS.add(productDTO);

            PhotographyDTO photographyDTO = PhotographyDTO.builder()
                    .date(LocalDateTime.now().toString())
                    .products(productDTOS)
                    .userSystem(UserSystem.builder()
                            .id(loggerUser.getId())
                            .build())
                    .build();

            if(!Objects.isNull(historyDTO.getStepDTO())){
                historyDTO.getStepDTO().setPhotographs(photographyDTO);
            }else{
                StepDTO stepDTO = StepDTO
                        .builder()
                        .photographs(photographyDTO)
                        .build();
                historyDTO.setStepDTO(stepDTO);
            }
            histories.add(historyDTO);
        }

        if(!histories.isEmpty()){
            for(HistoryDTO history : histories){
                FinishedTriageDTO finishedTriageDTO = Objects.nonNull(history.getStepDTO()) ? history.getStepDTO().getFinishedTriageDTO() : null;
                if(Objects.nonNull(finishedTriageDTO)){
                    int qtyApproved = finishedTriageDTO.getQtyApprovedItem();
                    int qtyProductPhoto = Objects.nonNull(history.getStepDTO().getPhotographs()) ? history.getStepDTO().getPhotographs().getProducts().size() : 0;

                    if(qtyProductPhoto >= qtyApproved)
                        history.setStatusBag(BagStatus.PHOTOGRAPHY_REVIEW);
                }
            }
        }
        return histories;
    }

    public void savePhotographyStatusInHistory(Long bagId, String status, String qty){
        PhotographyUpdateDTO photographyUpdateDTO = PhotographyUpdateDTO.builder()
                .photographyUpdateDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .photographyStatus(status)
                .photographyFinishedQty("0")
                .bagId(bagId)
                .build();
        if(!StringUtil.isNullOrEmpty(qty)){
            photographyUpdateDTO.setPhotographyFinishedQty(qty);
        }
        String tokenAuth = headers.getHeaderString(AUTHORIZATION);
        historyClient.updatePhotographyhistory(photographyUpdateDTO, tokenAuth);
    }
}
