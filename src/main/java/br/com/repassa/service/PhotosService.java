package br.com.repassa.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import br.com.repassa.enums.*;

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
import br.com.repassa.client.RekognitionBarClient;
import br.com.repassa.dto.IdentificatorsDTO;
import br.com.repassa.dto.PhotoFilterDTO;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.dto.ProcessBarCodeRequestDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.dto.ProductDTO;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.exception.PhotoError;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectTextRequest;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.TextDetection;
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
        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();
        username = username.replaceAll("\\s", "+");
        username = username.replaceAll("[^a-zA-Z0-9+]", "");

        LOG.info("Fetered by Name: " + username.toString());

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":upload_date", AttributeValue.builder().s(filter.getDate()).build());
        expressionAttributeValues.put(":edited_by", AttributeValue.builder().s(username).build());

        List<PhotoFilterResponseDTO> photoFilterResponseDTOS = this.photoClient.listItem(expressionAttributeValues);

        persistPhotoManager(photoFilterResponseDTOS);
    }

    public PhotosManager processBarCode(ProcessBarCodeRequestDTO processBarCodeRequestDTO, String user, String tokenAuth) {

        RekognitionClient rekognitionClient = new RekognitionBarClient().openConnection();
        List<IdentificatorsDTO> validateIds = new ArrayList<>();

        processBarCodeRequestDTO.getGroupPhotos().forEach(item ->
                item.getPhotos().forEach(photo -> {
                    if (Objects.equals(photo.getTypePhoto(), TypePhoto.ETIQUETA)) {
                        String url = photo.getUrlPhotoBarCode();
                        String bucket = url.split("\\.")[0].replace("https://", "");
                        String pathImage = url.split("\\.com/")[1].replace("+", " ");

                        DetectTextRequest decReq = DetectTextRequest.builder()
                                .image(Image.builder()
                                        .s3Object(S3Object.builder()
                                                .bucket(bucket)
                                                .name(pathImage)
                                                .build())
                                        .build())
                                .build();

                        DetectTextResponse decRes = rekognitionClient.detectText(decReq);

                        boolean foundText = false;
                        for (TextDetection textDetection : decRes.textDetections()) {
                            validateIds.add(IdentificatorsDTO.builder()
                                    .groupId(item.getId())
                                    .productId(textDetection.detectedText())
                                    .build());
                            foundText = true;
                            break;
                        }

                        if (!foundText) {
                            try {
                                var photoManager = photoClient.findByImageId(photo.getIdPhoto());
                                updatePhotoManagerByPhoto(photoManager, item.getId(), photo.getIdPhoto(), photo.getTypePhoto().name());
                                validateIds.add(IdentificatorsDTO.builder()
                                        .groupId(item.getId())
                                        .productId(null)
                                        .valid(false)
                                        .build());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                        }
                    }
                }));

        rekognitionClient.close();

        try {
            validateIdentificators(validateIds, tokenAuth);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return searchPhotos(processBarCodeRequestDTO.getDate(), user);
    }

    public PhotosManager searchPhotos(String date, String name) {

        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();
        username = username.replaceAll("\\s", "+");
        username = username.replaceAll("[^a-zA-Z0-9+]", "");

        LOG.info("Fetered by Name: " + username.toString());

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":statusManagerPhotos",
                AttributeValue.builder().s(StatusManagerPhotos.STARTED.name()).build());
        expressionAttributeValues.put(":editor", AttributeValue.builder().s(username).build());
        expressionAttributeValues.put(":upload_date", AttributeValue.builder().s(date).build());

        try {
            return photoClient.getPhotos(expressionAttributeValues);
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
                PhotosManager photosManager = null;

                if (identificator.getProductId() == null) {
                    identificator.setMessage("ID não encontrado na imagem.");
                } else {
                    ProductDTO productDTO = validateProductIDResponse(identificator.getProductId(), tokenAuth);

                    photosManager = photoClient.findByProductId(identificator.getProductId());

                    if (photosManager == null) {
                        identificator.setValid(true);
                        identificator.setMessage("ID Disponível");
                    } else {
                        if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.STARTED) {
                            // Verifica se encontrou outro Grupo com o mesmo ID
                            List<GroupPhotos> foundGroupPhotos = photosManager.getGroupPhotos()
                                    .stream()
                                    .filter(group -> identificator.getProductId().equals(group.getProductId()) && !identificator.getGroupId().equals(group.getId()))
                                    .collect(Collectors.toList());

                            if (foundGroupPhotos.size() >= 2) {
                                identificator.setMessage(
                                        "O ID " + identificator.getProductId() + " está sendo utilizado em outro Grupo.");
                                photosManager = photoClient.findByGroupId(identificator.getGroupId());

                            } else if (foundGroupPhotos.size() == 1
                                    && !foundGroupPhotos.get(0).getId().equals(identificator.getGroupId())) {
                                identificator.setMessage(
                                        "O ID " + identificator.getProductId() + " está sendo utilizado em outro Grupo.");

                                photosManager = photoClient.findByGroupId(identificator.getGroupId());
                            } else {
                                identificator.setValid(true);
                                identificator.setMessage("ID Disponível");
                            }
                        } else if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.FINISHED) {
                            identificator.setMessage("O ID " + identificator.getProductId()
                                    + " está sendo utilizado em outro Grupo com status Finalizado.");
                        }
                    }
                }

                // Se não econtrar um PHOTO_MANAGER com base no PRODUCT_ID informado, será feito uma nova busca, informando o GROUP_ID
                if (photosManager == null) {
                    PhotosManager photoManagerGroup = photoClient.findByGroupId(identificator.getGroupId());
                    updatePhotoManager(photoManagerGroup, identificator);
                } else {
                    updatePhotoManager(photosManager, identificator);
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

    private void updatePhotoManager(PhotosManager photoManager, IdentificatorsDTO identificator) {
        photoManager.getGroupPhotos().forEach(group -> {
            if (group.getId().equals(identificator.getGroupId())) {
                group.setProductId(identificator.getProductId());

                if (!identificator.getValid()) {
                    group.setIdError(TypeError.ID_ERROR.name());
                }
            }
        });

        photoClient.savePhotosManager(photoManager);
    }

    private void updatePhotoManagerByPhoto(PhotosManager photoManager, String groupId, String photoId, String typePhoto) {
        photoManager.getGroupPhotos().forEach(group -> {
            if (group.getId().equals(groupId)) {
                group.getPhotos().forEach(photo -> {
                    if (photo.getId().equals(photoId)) {
                        photo.setTypePhoto(TypePhoto.valueOf(typePhoto));
                    }
                });
            }
        });

        photoClient.savePhotosManager(photoManager);
    }

    @Transactional
    public void persistPhotoManager(List<PhotoFilterResponseDTO> resultList) throws RepassaException {
        LOG.info("Iniciando processo de persistencia");

        var photoManager = new PhotosManager();
        List<GroupPhotos> groupPhotos = new ArrayList<>();
        List<Photo> photos = new ArrayList<>(4);
        var managerGroupPhotos = new ManagerGroupPhotos(groupPhotos);

        AtomicInteger count = new AtomicInteger();

        resultList.forEach(photosFilter -> {

            var photo = Photo.builder().namePhoto(photosFilter.getImageName())
                    .sizePhoto(photosFilter.getSizePhoto())
                    .id(photosFilter.getId())
                    .typePhoto(TypePhoto.getPosition(count.get()))
                    .urlPhoto(photosFilter.getOriginalImageUrl())
                    .base64(photosFilter.getThumbnailBase64()).build();

            photoManager.setEditor(photosFilter.getEditedBy());
            photoManager.setDate(photosFilter.getUploadDate());
            photoManager.setId(UUID.randomUUID().toString());

            photos.add(photo);

            if (photos.size() == 4) {
                managerGroupPhotos.addPhotos(photos, Boolean.valueOf(photosFilter.getIsValid()));
                photos.clear();
                count.set(0);

            } else if (photos.size() < 4
                    && (resultList.size() - managerGroupPhotos.getTotalPhotos()) == photos.size()) {

                while (photos.size() < 4) {
                    createPhotosError(photos);
                }
                managerGroupPhotos.addPhotos(photos, false);
                count.set(count.get() + 1);

            }
        });

        photoManager.setStatusManagerPhotos(StatusManagerPhotos.STARTED);
        photoManager.setGroupPhotos(groupPhotos);

        persistPhotoManagerDynamoDB(photoManager);

    }

    @Transactional
    public String finishManagerPhotos(PhotosManager photosManager) throws RepassaException {

        if (Objects.isNull(photosManager)) {
            throw new RepassaException(PhotoError.OBJETO_VAZIO);
        }
        photosManager.setStatusManagerPhotos(StatusManagerPhotos.FINISHED);
        photosManager.getGroupPhotos().forEach(group -> {
            group.setStatusProduct(StatusProduct.FINALIZADO);
            group.setUpdateDate(LocalDateTime.now().toString());
        });

        try {
            photoClient.savePhotosManager(photosManager);
            return PhotoError.SUCESSO_AO_SALVAR.getErrorMessage();
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_SALVAR_NO_DYNAMO);
        }
    }

    private void persistPhotoManagerDynamoDB(PhotosManager photosManager) throws RepassaException {
        try {
            photoClient.savePhotosManager(photosManager);
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
