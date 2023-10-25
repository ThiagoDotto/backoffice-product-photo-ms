package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.*;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.entity.dynamo.PhotoProcessed;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.enums.StatusProduct;
import br.com.repassa.enums.TypeError;
import br.com.repassa.enums.TypePhoto;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.resource.client.PhotoClient;
import br.com.repassa.resource.client.ProductRestClient;
import br.com.repassa.resource.client.RekognitionBarClient;
import br.com.repassa.service.dynamo.PhotoProcessingService;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class PhotosService {

    private static final Logger LOG = LoggerFactory.getLogger(PhotosService.class);

    private static final String URL_ERROR_IMAGE = "https://backoffice-triage-photo-dev.s3.amazonaws.com/invalidPhoto.png";

    @Inject
    @RestClient
    ProductRestClient productRestClient;

    @Inject
    PhotoClient photoClient;

    @Inject
    HistoryService historyService;

    @Inject
    PhotoProcessingService photoProcessingService;

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

    public PhotosManager processBarCode(ProcessBarCodeRequestDTO processBarCodeRequestDTO, String user,
                                        String tokenAuth) throws RepassaException {

        RekognitionClient rekognitionClient = new RekognitionBarClient().openConnection();
        List<IdentificatorsDTO> validateIds = new ArrayList<>();

        processBarCodeRequestDTO.getGroupPhotos().forEach(item -> item.getPhotos().forEach(photo -> {
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
                    String productId = extractNumber(textDetection.detectedText());

                    validateIds.add(IdentificatorsDTO.builder()
                            .groupId(item.getId())
                            .productId(productId)
                            .build());
                    foundText = true;
                    break;
                }

                if (!foundText) {
                    validateIds.add(IdentificatorsDTO.builder()
                            .groupId(item.getId())
                            .productId(null)
                            .valid(false)
                            .build());
                }
            }
        }));

        rekognitionClient.close();

        if (validateIds.isEmpty()) {
            throw new RepassaException(PhotoError.REKOGNITION_PHOTO_EMPTY);
        }

        try {
            validateIdentificators(validateIds, tokenAuth, true);
        } catch (Exception e) {
            throw new RepassaException(PhotoError.REKOGNITION_ERROR);
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
                AttributeValue.builder().s(StatusManagerPhotos.IN_PROGRESS.name()).build());
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
    public List<IdentificatorsDTO> validateIdentificators(List<IdentificatorsDTO> identificators, String tokenAuth,
                                                          Boolean isOcr)
            throws Exception {
        if (identificators.isEmpty()) {
            throw new RepassaException(PhotoError.VALIDATE_IDENTIFICATORS_EMPTY);
        }

        List<IdentificatorsDTO> response = new ArrayList<>();

        identificators.forEach(identificator -> {
            try {
                PhotosManager photosManager = null;

                if (identificator.getProductId() == null) {
                    if (isOcr) {
                        identificator
                                .setMessage("ID de Produto não reconhecido. Verifique a etiqueta e tente novamente.");
                    } else {
                        identificator
                                .setMessage("ID de Produto não reconhecido. Verifique a etiqueta e informe novamente.");
                    }

                    identificator.setValid(false);
                } else {
                    LOG.info("PRODUCT_ID: " + identificator.getProductId());

                    validateProductIDResponse(identificator.getProductId(), tokenAuth);

                    photosManager = photoClient.findByProductId(identificator.getProductId());

                    LOG.info("PRODUCT_ID 2: " + identificator.getProductId());

                    if (photosManager == null) {
                        identificator.setValid(true);
                        identificator.setMessage("ID de Produto disponível");
                    } else {
                        if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.IN_PROGRESS) {
                            // Verifica se encontrou outro Grupo com o mesmo ID
                            List<GroupPhotos> foundGroupPhotos = photosManager.getGroupPhotos()
                                    .stream()
                                    .filter(group -> identificator.getProductId().equals(group.getProductId())
                                            && !identificator.getGroupId().equals(group.getId()))
                                    .collect(Collectors.toList());

                            if (foundGroupPhotos.size() >= 2) {
                                identificator.setMessage(
                                        "O ID do Produto " + identificator.getProductId()
                                                + " está sendo utilizado em outro Grupo.");
                                identificator.setValid(false);
                                photosManager = photoClient.findByGroupId(identificator.getGroupId());

                            } else if (foundGroupPhotos.size() == 1
                                    && !foundGroupPhotos.get(0).getId().equals(identificator.getGroupId())) {
                                identificator.setMessage(
                                        "O ID do Produto " + identificator.getProductId()
                                                + " está sendo utilizado em outro Grupo.");
                                identificator.setValid(false);

                                photosManager = photoClient.findByGroupId(identificator.getGroupId());
                            } else {
                                identificator.setValid(true);
                                identificator.setMessage("ID de Produto disponível");
                            }
                        } else if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.FINISHED) {
                            identificator.setMessage("O ID do Produto " + identificator.getProductId()
                                    + " está sendo utilizado em outro Grupo com status Finalizado.");
                            identificator.setValid(false);
                        }
                    }
                }
                // Se não econtrar um PHOTO_MANAGER com base no PRODUCT_ID informado, será feito
                // uma nova busca, informando o GROUP_ID
                if (photosManager == null) {
                    PhotosManager photoManagerGroup = photoClient.findByGroupId(identificator.getGroupId());
                    updatePhotoManager(photoManagerGroup, identificator);
                } else {
                    updatePhotoManager(photosManager, identificator);
                }

                response.add(identificator);
            } catch (RepassaException repassaException) {
                if (repassaException.getRepassaUtilError().getErrorCode().equals(PhotoError.PRODUCT_ID_INVALIDO
                        .getErrorCode())) {
                    identificator.setMessage("O ID " + identificator.getProductId() + " é inválido");
                    identificator.setValid(false);
                    response.add(identificator);
                } else {
                    LOG.info("Erro ao persistir o ID: " + repassaException.getMessage());

                    identificator.setMessage("Houve um erro ao processar o ID " + identificator.getProductId());
                    identificator.setValid(false);
                    response.add(identificator);
                }

                PhotosManager photosManager = null;
                try {
                    photosManager = photoClient.findByProductId(identificator.getProductId());
                    if (photosManager == null) {
                        PhotosManager photoManagerGroup = photoClient.findByGroupId(identificator.getGroupId());
                        updatePhotoManager(photoManagerGroup, identificator);
                    } else {
                        updatePhotoManager(photosManager, identificator);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return response;
    }

    private void updatePhotoManager(PhotosManager photoManager, IdentificatorsDTO identificator)
            throws RepassaException {
        LOG.info("PHOTOMANAGER UPDATE: " + photoManager.getId());
        if (photoManager != null) {
            photoManager.getGroupPhotos().forEach(group -> {
                if (group.getId().equals(identificator.getGroupId())) {
                    group.setProductId(identificator.getProductId());

                    if (identificator.getValid()) {
                        group.setIdError(null);
                    } else {
                        group.setIdError(TypeError.ID_ERROR.name());
                    }
                }
            });

            photoClient.savePhotosManager(photoManager);
        } else {
            throw new RepassaException(PhotoError.PHOTO_MANAGER_IS_NULL);
        }
    }

    public ChangeTypePhotoDTO changeStatusPhoto(ChangeTypePhotoDTO data) throws RepassaException {
        try {
            PhotosManager photoManager = photoClient.findByImageAndGroupId(data.getPhotoId(), data.getGroupId());
            AtomicBoolean updatePhotoManager = new AtomicBoolean(Boolean.FALSE);

            if (photoManager == null) {
                throw new RepassaException(PhotoError.PHOTO_MANAGER_IS_NULL);
            }

            photoManager.getGroupPhotos().forEach(group -> {

                if (group.getId().equals(data.getGroupId())) {

                    group.getPhotos().forEach(photo -> {

                        if (Objects.nonNull(photo.getId()) && photo.getId().equals(data.getPhotoId())) {
                            photo.setTypePhoto(data.getTypePhoto());
                            updatePhotoManager.set(Boolean.TRUE);
                        }
                    });
                }
            });

            if (updatePhotoManager.get()) {
                photoClient.savePhotosManager(photoManager);

                return new ChangeTypePhotoDTO(
                        data.getPhotoId(),
                        data.getGroupId(),
                        data.getTypePhoto(),
                        "Foto atualizada com sucesso.");
            }

            return new ChangeTypePhotoDTO(
                    data.getPhotoId(),
                    data.getGroupId(),
                    data.getTypePhoto(),
                    "Não encontrou informações suficientes para atualizar o Status da Foto.");
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ALTERAR_STATUS_INVALIDO);
        }
    }

    @Transactional
    public void persistPhotoManager(List<PhotoFilterResponseDTO> resultList) throws RepassaException {
        LOG.info("Iniciando processo de persistencia");

        var photoManager = new PhotosManager();
        List<GroupPhotos> groupPhotos = new ArrayList<>();
        List<Photo> photos = new ArrayList<>(4);
        var managerGroupPhotos = new ManagerGroupPhotos(groupPhotos);

        AtomicInteger count = new AtomicInteger();
        AtomicBoolean isPhotoValid = new AtomicBoolean(Boolean.TRUE);

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
            count.set(count.get() + 1);

            if (!Boolean.valueOf(photosFilter.getIsValid())) {
                isPhotoValid.set(Boolean.FALSE);
            }

            if (photos.size() == 4) {
                managerGroupPhotos.addPhotos(photos, isPhotoValid);
                photos.clear();
                count.set(0);
                isPhotoValid.set(Boolean.TRUE);

            } else if (photos.size() < 4
                    && (resultList.size() - managerGroupPhotos.getTotalPhotos()) == photos.size()) {

                while (photos.size() < 4) {
                    createPhotosError(photos);
                }
                managerGroupPhotos.addPhotos(photos, new AtomicBoolean(Boolean.FALSE));
            }
        });

        photoManager.setStatusManagerPhotos(StatusManagerPhotos.IN_PROGRESS);
        photoManager.setGroupPhotos(groupPhotos);

        persistPhotoManagerDynamoDB(photoManager);

    }

    @Transactional
    public void finishManagerPhotos(String id, UserPrincipalDTO loggerUser, HttpHeaders headers) throws Exception {

        if (Objects.isNull(id)) {
            throw new RepassaException(PhotoError.OBJETO_VAZIO);
        }

        var photosManager = photoClient.findById(id);

        if (Objects.isNull(photosManager)) {
            throw new RepassaException(PhotoError.OBJETO_VAZIO);
        }

        photosManager.setStatusManagerPhotos(StatusManagerPhotos.FINISHED);
        AtomicBoolean existError = new AtomicBoolean(false);
        photosManager.getGroupPhotos().forEach(group -> {

            if (group.getIdError() != null || group.getImageError() != null) {
                existError.set(true);
            }

            group.setStatusProduct(StatusProduct.FINISHED);
            group.setUpdateDate(LocalDateTime.now().toString());
        });

        if (existError.get()) {
            throw new RepassaException(PhotoError.GROUP_ERROR);
        }

        try {
            photoClient.savePhotosManager(photosManager);
            historyService.save(photosManager, loggerUser, headers);
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

            Log.info("FOUND PRODUCT ID: ");

            return response.readEntity(ProductDTO.class);
        } catch (ClientWebApplicationException e) {
            throw new RepassaException(PhotoError.PRODUCT_ID_INVALIDO);
        }
    }

    private String extractNumber(String value) {
        // Define a expressão regular para encontrar números
        Pattern pattern = Pattern.compile("\\d+");

        // Cria um Matcher para a string de entrada
        Matcher matcher = pattern.matcher(value);

        // Inicializa uma string vazia para armazenar os números
        StringBuilder numbers = new StringBuilder();

        // Encontra todos os números e os adiciona à string de números
        while (matcher.find()) {
            numbers.append(matcher.group());
        }

        // Converte a string de números para um número inteiro (se necessário)
        if (!numbers.isEmpty()) {
            return numbers.toString();
        }

        return null;
    }

    private void createPhotosError(List<Photo> photos) {
        Photo photoError = Photo.builder().urlPhoto(URL_ERROR_IMAGE).namePhoto("error").base64("")
                .sizePhoto("0").build();
        photos.add(photoError);
    }


    public void teste(){
        PhotoProcessed p = new PhotoProcessed();
        p.setId(UUID.randomUUID().toString());
        p.setBagId("123");
        p.setEditedBy("TESTE");
        p.setSizePhoto("123456");
        p.setImageId(UUID.randomUUID().toString());
        p.setImageName("testeTT");
        p.setIsValid("true");
        p.setOriginalImageUrl("blablablabalbalbalbalabl");
        p.setThumbnailBase64("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");
        p.setUploadDate("2023-10-24T14:32:47.569Z");
        photoProcessingService.save(p);
    }
}
