package br.com.repassa.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.ChangeTypePhotoDTO;
import br.com.repassa.dto.IdentificatorsDTO;
import br.com.repassa.dto.ImageDTO;
import br.com.repassa.dto.PhotoFilterDTO;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.dto.ProcessBarCodeRequestDTO;
import br.com.repassa.dto.ProductDTO;
import br.com.repassa.dto.ProductPhotoDTO;
import br.com.repassa.dto.ProductPhotoListDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.enums.StatusProduct;
import br.com.repassa.enums.TypeError;
import br.com.repassa.enums.TypePhoto;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.resource.client.AwsS3Client;
import br.com.repassa.resource.client.PhotoClient;
import br.com.repassa.resource.client.ProductRestClient;
import br.com.repassa.resource.client.RekognitionBarClient;
import io.quarkus.logging.Log;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectTextRequest;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.TextDetection;

@ApplicationScoped
public class PhotosService {

    private static final Logger LOG = LoggerFactory.getLogger(PhotosService.class);

    private static final String URL_ERROR_IMAGE = "https://backoffice-triage-photo-dev.s3.amazonaws.com/invalidPhoto.png";

    @ConfigProperty(name = "s3.aws.bucket-name")
    String bucketName;

    @ConfigProperty(name = "s3.aws.error-image")
    String errorImage;

    @Inject
    @RestClient
    ProductRestClient productRestClient;

    @Inject
    PhotoClient photoClient;

    @Inject
    HistoryService historyService;

    @Inject
    AwsS3Client awsS3Client;

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

    @Transactional
    public PhotosManager insertImage(ImageDTO imageDTO, String name) throws RepassaException {

        var photosValidate = new PhotosValidate();
        // TODO: Validar a foto
        photosValidate.validatePhotos(imageDTO);
        // TODO: Salvar no S3( buscar do triage)
        var objectKey = photosValidate.validatePathBucket(name, imageDTO.getDate());
        imageDTO.getPhotoBase64().forEach(photo -> {

            try {
                awsS3Client.uploadBase64FileToS3(bucketName, objectKey.concat(
                        photo.getName() + "." + photo.getType()), photo.getBase64());
            } catch (RepassaException e) {
                throw new RuntimeException(e);
            }
        });
        // TODO: Salvar no Dynamo
        // 1- PhotoProcessingTable
        // 2- PhotosManager
        return new PhotosManager();
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

    public ProductPhotoListDTO findPhotoByProductId(String productId) throws RepassaException {
        LOG.info("Busca de fotos para o productId: {}", productId);
        try {
            final var photoManager = photoClient.findByProductId(productId);

            if (Objects.isNull(photoManager) || Objects.isNull(photoManager.getGroupPhotos())) {
                return ProductPhotoListDTO.builder().photos(List.of()).build();
            }

            final var lastGroupPhotoIndex = photoManager.getGroupPhotos().size() - 1;

            final var productPhotoDTOList = photoManager.getGroupPhotos().get(lastGroupPhotoIndex).getPhotos().stream()
                    .map(p -> ProductPhotoDTO.builder()
                            .id(p.getId())
                            .typePhoto(p.getTypePhoto().toString())
                            .sizePhoto(p.getSizePhoto())
                            .namePhoto(p.getNamePhoto())
                            .urlPhoto(p.getUrlPhoto())
                            .build())
                    .toList();

            LOG.info("Busca de fotos para o productId {} realizada com sucesso", productId);
            return ProductPhotoListDTO.builder().photos(productPhotoDTOList).build();
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_BUSCAR_IMAGENS);
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
        Photo photoError = Photo.builder().urlPhoto(errorImage).namePhoto("error").base64("")
                .sizePhoto("0").build();
        photos.add(photoError);
    }

}
