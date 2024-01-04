package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.HistoryDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.PhotoDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.PhotographyDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.UserSystem;
import br.com.repassa.dto.PhotographyUpdateDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.resource.client.HistoryClient;
import io.quarkus.runtime.util.StringUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);
    private static final String AUTHORIZATION = "Authorization";

    @RestClient
    HistoryClient historyClient;

    @Context
    HttpHeaders headers;

    public void save(PhotosManager photosManager, UserPrincipalDTO loggerUser, HttpHeaders headers) {

        List<GroupPhotos> groupPhotos = photosManager.getGroupPhotos();
        List<HistoryDTO> histories = new ArrayList<>();

        List<HistoryDTO> historyDTOS = historiesObjsBuilder(loggerUser, groupPhotos, histories);
        LOGGER.debug("Salvando no History as photos");
        historyDTOS.forEach(historyDTO ->
                historyClient.updateHistory(historyDTO,headers.getHeaderString(AUTHORIZATION)));
    }

    private static List<HistoryDTO> historiesObjsBuilder(UserPrincipalDTO loggerUser, List<GroupPhotos> groupPhotos, List<HistoryDTO> histories) {
        groupPhotos
                .forEach(groupPhoto -> {
                    String productId = groupPhoto.getProductId();
                    String bagID = productId.substring(0, productId.length() - 3);
                    List<Photo> photos = groupPhoto.getPhotos();

                    List<PhotoDTO> foto = new ArrayList<>();
                    photos.forEach(photo -> {
                                PhotoDTO photoDTO = new PhotoDTO();
                                photoDTO.setName(photo.getNamePhoto());
                                photoDTO.setId(photo.getId());
                                photoDTO.setType(photo.getTypePhoto().toString());
                                photoDTO.setUrl(photo.getUrlPhoto());
                                foto.add(photoDTO);
                            });

                    PhotographyDTO photographyDTO = PhotographyDTO.builder()
                            .date(LocalDateTime.now().toString())
                            .photos(foto)
                            .productId(Long.valueOf(productId))
                            .userSystem(UserSystem.builder()
                                    .id(loggerUser.getId())
                                    .build())
                            .build();

                    HistoryManagement historyManagement = new HistoryManagement();
                    HistoryDTO historyDTO = historyManagement.addPhotography(bagID, photographyDTO);
                    histories.add(historyDTO);
                });
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
