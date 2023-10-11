package br.com.repassa.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.client.PhotoClient;
import br.com.repassa.dto.IdentificatorsDTO;
import br.com.repassa.dto.PhotoFilterDTO;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.dto.ProductDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.resource.client.ProductRestClient;

@ApplicationScoped
public class PhotosService {

    private static final Logger LOG = LoggerFactory.getLogger(PhotosService.class);

    private static final String URL_ERROR_IMAGE = "https://backoffice-triage-photo-dev.s3.amazonaws.com/invalidPhoto.png";

    @Inject
    @RestClient
    ProductRestClient productRestClient;

    @Inject
    PhotoClient photoClient;

    public void filterAndPersist(final PhotoFilterDTO filter, final String name) throws RepassaException {

        LOG.info("Filter", filter.toString());
        String fieldFiltered = "id, bag_id, edited_by, image_id, imagem_name, is_valid, notes, original_image_url, size_photo, thumbnail_base64, upload_date";
        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();
        username = username.replaceAll("\\s", "+");
        username = username.replaceAll("[^a-zA-Z0-9+]", "");

        LOG.info("Fetered by Name: " + username.toString());

        Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();

        expressionAttributeValues.put(":upload_date", filter.getDate());
        expressionAttributeValues.put(":edited_by", username);

        List<PhotoFilterResponseDTO> photoFilterResponseDTOS = this.photoClient.listItem(fieldFiltered,
                expressionAttributeValues);

        persistPhotoManager(photoFilterResponseDTOS);
    }

    public PhotosManager searchPhotos(String date, String name) {

        String fieldFiltered = "id, editor, groupPhotos, upload_date, statusManagerPhotos";
        Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();

        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();
        username = username.replaceAll("\\s", "+");
        username = username.replaceAll("[^a-zA-Z0-9+]", "");

        LOG.info("Fetered by Name: " + username.toString());

        expressionAttributeValues.put(":statusManagerPhotos", StatusManagerPhotos.STARTED.name());
        expressionAttributeValues.put(":editor", username);
        expressionAttributeValues.put(":upload_date", date);

        try {
            return photoClient.getPhotos(fieldFiltered, expressionAttributeValues);
        } catch (RepassaException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public List<IdentificatorsDTO> validateIdentificators(List<IdentificatorsDTO> identificators, String tokenAuth)
            throws Exception {
        if (identificators.isEmpty()) {
            throw new RepassaException(PhotoError.VALIDATE_IDENTIFICATORS_EMPTY);
        }

        List<IdentificatorsDTO> response = new ArrayList<>();

        identificators.forEach(identificator -> {
            try {
                // ProductDTO productDTO =
                // validateProductIDResponse(identificator.getProductId(), tokenAuth);

                // System.out.println("AQUI: " + productDTO.getProductId());

                PhotosManager photosManager = photoClient.findByProductId(identificator.getProductId());

                if (photosManager == null) {
                    identificator.setValid(true);
                    identificator.setMessage("ID Disponível");

                    LOG.info("ProductiID " + identificator.getProductId() + " não encontrado.");
                } else {
                    if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.STARTED) {
                        // Verifica se encontrou outro Grupo com o mesmo ID
                        List<GroupPhotos> foundGroupPhotos = photosManager.getGroupPhotos()
                                .stream()
                                .filter(group -> identificator.getProductId().equals(group.getProductId())
                                        && group.getId() != identificator.getGroupId())
                                .collect(Collectors.toList());

                        if (foundGroupPhotos.size() >= 2) {
                            identificator.setMessage(
                                    "O ID " + identificator.getProductId() + " está sendo utilizando em outro Grupo.");
                        } else if (foundGroupPhotos.size() == 1
                                && !foundGroupPhotos.get(0).getId().equals(identificator.getGroupId())) {
                            identificator.setMessage(
                                    "O ID " + identificator.getProductId() + " está sendo utilizando em outro Grupo.");
                        } else {
                            identificator.setValid(true);
                            identificator.setMessage("ID Disponível");
                        }
                    } else if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.FINISHED) {
                        identificator.setMessage("O ID " + identificator.getProductId()
                                + " está sendo utilizando em outro Grupo com status Finalizado.");
                    }
                }

                response.add(identificator);
            } catch (RepassaException repassaException) {
                identificator.setMessage("O ID " + identificator.getProductId() + " é inválido");
                response.add(identificator);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return response;
    }

    @Transactional
    public void persistPhotoManager(List<PhotoFilterResponseDTO> resultList) throws RepassaException {
        LOG.info("Iniciando processo de persistencia");

        var photoManager = new PhotosManager();
        List<GroupPhotos> groupPhotos = new ArrayList<>();
        List<Photo> photos = new ArrayList<>(4);
        var managerGroupPhotos = new ManagerGroupPhotos(groupPhotos);

        resultList.forEach(photosFilter -> {

            var photo = Photo.builder().namePhoto(photosFilter.getImageName())
                    .sizePhoto(photosFilter.getSizePhoto())
                    .id(photosFilter.getId())
                    .urlPhoto(photosFilter.getOriginalImageUrl())
                    .base64(photosFilter.getThumbnailBase64()).build();

            photoManager.setEditor(photosFilter.getEditedBy());
            photoManager.setDate(photosFilter.getUploadDate());
            photoManager.setId(UUID.randomUUID().toString());

            photos.add(photo);

            if (photos.size() == 4) {
                managerGroupPhotos.addPhotos(photos, true);
                photos.clear();

            } else if (photos.size() < 4
                    && (resultList.size() - managerGroupPhotos.getTotalPhotos()) == photos.size()) {

                while (photos.size() < 4) {
                    createPhotosError(photos);
                }
                managerGroupPhotos.addPhotos(photos, false);
            }
        });

        photoManager.setStatusManagerPhotos(StatusManagerPhotos.STARTED);
        photoManager.setGroupPhotos(groupPhotos);

        try {
            photoManager.getGroupPhotos().forEach(x -> {
                x.setId(UUID.randomUUID().toString());
            });

            photoClient.savePhotosManager(photoManager);
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_PERSISTIR);
        }
    }

    private ProductDTO validateProductIDResponse(String productId, String tokenAuth) throws RepassaException {
        try {
            Response response = productRestClient.validateProductId(productId, tokenAuth);

            return response.readEntity(ProductDTO.class);
        } catch (ClientWebApplicationException e) {
            throw new RepassaException(PhotoError.PRODUCT_ID_INVALIDO);
        }
    }

    private void createPhotosError(List<Photo> photos) {
        Photo photoError = Photo.builder().urlPhoto(URL_ERROR_IMAGE).namePhoto("error").base64("")
                .sizePhoto("0").build();
        photos.add(photoError);
    }
}
