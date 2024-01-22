package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.HistoryDTO;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.AwsConfig;
import br.com.repassa.dto.*;
import br.com.repassa.dto.history.BagsResponseDTO;
import br.com.repassa.dto.history.HistoryResponseDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.entity.dynamo.PhotoProcessed;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.enums.StatusProduct;
import br.com.repassa.enums.TypeError;
import br.com.repassa.enums.TypePhoto;
import br.com.repassa.exception.AwsPhotoError;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.repository.aws.PhotoManagerRepository;
import br.com.repassa.repository.aws.PhotoProcessingService;
import br.com.repassa.resource.client.AwsS3Client;
import br.com.repassa.resource.client.AwsS3RenovaClient;
import br.com.repassa.resource.client.ProductRestClient;
import br.com.repassa.service.rekognition.PhotoRemoveService;
import br.com.repassa.service.rekognition.RekognitionService;
import br.com.repassa.utils.CommonsUtil;
import br.com.repassa.utils.PhotoUtils;
import br.com.repassa.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.util.StringUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@ApplicationScoped
public class PhotosService {
    private static final Logger LOG = LoggerFactory.getLogger(PhotosService.class);
    AwsConfig awsConfig;
    ProductService productService;
    PhotoManagerRepository photoManagerRepository;
    PhotoProcessingService photoProcessingService;
    HistoryService historyService;
    AwsS3Client awsS3Client;
    AwsS3RenovaClient awsS3RenovaClient;
    ProductRestClient productRestClient;
    RekognitionService rekognitionService;
    PhotoRemoveService photoRemoveService;

    @Inject
    public PhotosService(AwsConfig awsConfig, ProductService productService, PhotoManagerRepository photoManagerRepository,
                         PhotoProcessingService photoProcessingService, HistoryService historyService, AwsS3Client awsS3Client,
                         AwsS3RenovaClient awsS3RenovaClient, RekognitionService rekognitionService,
                         PhotoRemoveService photoRemoveService, @RestClient ProductRestClient productRestClient) {
        this.awsConfig = awsConfig;
        this.productService = productService;
        this.photoManagerRepository = photoManagerRepository;
        this.photoProcessingService = photoProcessingService;
        this.historyService = historyService;
        this.awsS3Client = awsS3Client;
        this.awsS3RenovaClient = awsS3RenovaClient;
        this.productRestClient = productRestClient;
        this.rekognitionService = rekognitionService;
        this.photoRemoveService = photoRemoveService;
    }

    public void filterAndPersist(final PhotoFilterDTO filter, final String name) throws RepassaException {

        LOG.info("Filter {}", filter.getDate());
        String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(name));

        LOG.info("Fetered by Name: {}", username);
        List<PhotoFilterResponseDTO> photoFilterResponseDTOS = this.photoProcessingService
                .listItensByDateAndUser(filter.getDate(), username);

        persistPhotoManager(photoFilterResponseDTOS);
    }

    private List<String> extractBagIdFromDTO(List<IdentificatorsDTO> validateIds) {
        Set<String> modifiedProductIdsSet = new HashSet<>();
        for (IdentificatorsDTO dto : validateIds) {
            String productId = dto.getProductId();
            if(!StringUtil.isNullOrEmpty(productId) && productId.length() > 3){
                String modifiedProductId = productId.substring(0, productId.length() - 3);
                modifiedProductIdsSet.add(modifiedProductId);
            }
        }
        return new ArrayList<>(modifiedProductIdsSet);
    }

    private List<String> extractBagIdFromGroup(List<GroupPhotos> groupPhotos) {
        Set<String> modifiedProductIdsSet = new HashSet<>();
        for (GroupPhotos groupPhoto : groupPhotos) {
            String productId = groupPhoto.getProductId();
            if(!StringUtil.isNullOrEmpty(productId) && productId.length() > 3){
                String modifiedProductId = productId.substring(0, productId.length() - 3);
                modifiedProductIdsSet.add(modifiedProductId);
            }

        }
        return new ArrayList<>(modifiedProductIdsSet);
    }

    public PhotosManager processBarCode(ProcessBarCodeRequestDTO processBarCodeRequestDTO, String user) throws RepassaException {
        List<ProcessBarCodeRequestDTO.GroupPhoto> groupPhotos = processBarCodeRequestDTO.getGroupPhotos();

        List<IdentificatorsDTO> validateIds = rekognitionService.PhotosRecognition(groupPhotos);

        if (validateIds.isEmpty()) {
            throw new RepassaException(AwsPhotoError.REKOGNITION_PHOTO_EMPTY);
        }

        List<String> bagIds = extractBagIdFromDTO(validateIds);
        for (String bagId : bagIds) {
            historyService.savePhotographyStatusInHistory(Long.parseLong(bagId), "IN_PROGRESS", null);
        }

        try {
            validateIdentificators(validateIds, true);
        } catch (Exception e) {
            throw new RepassaException(AwsPhotoError.REKOGNITION_ERROR);
        }

        List<String> bagIds = extractBagIdFromDTO(validateIds);
        for(String bagId : bagIds){
                historyService.savePhotographyStatusInHistory(Long.parseLong(bagId), "IN_PROGRESS", null);
        }

        return searchPhotos(processBarCodeRequestDTO.getDate(), user);
    }

    public PhotosManager searchPhotos(String date, String name) throws RepassaException {
        String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(name));
        LOG.info("Fetered by Name: {}", username);

        long inicio = System.currentTimeMillis();
        System.out.println("inicio da busca (getByEditorUploadDateAndStatus) de fotos por usuário " + inicio);
        PhotosManager photosManager = photoManagerRepository.getByEditorUploadDateAndStatus(date, username);
        long fim = System.currentTimeMillis();
        System.out.println("FIM da busca (getByEditorUploadDateAndStatus) de fotos por usuário " + fim);
        System.out.println("total " + (fim - inicio));

        if (Objects.isNull(photosManager)) {
            PhotoFilterDTO photoFilterDTO = new PhotoFilterDTO();
            photoFilterDTO.setDate(date);

            filterAndPersist(photoFilterDTO, name);
            long inicio2 = System.currentTimeMillis();
            System.out.println("inicio da busca (getByEditorUploadDateAndStatus2) de fotos por usuário " + inicio2);
            photosManager = photoManagerRepository.getByEditorUploadDateAndStatus(date, username);
            long fim2 = System.currentTimeMillis();
            System.out.println("FIM da busca (getByEditorUploadDateAndStatus2) de fotos por usuário " + fim2);
            System.out.println("total " + (fim2 - inicio2));
        } else if (photosManager.getStatusManagerPhotos().equals(StatusManagerPhotos.FINISHED)) {
            throw new RepassaException(PhotoError.PHOTOMANAGER_FINISHED);
        }

        return photosManager;
    }

    @Transactional
    public List<IdentificatorsDTO> validateIdentificators(List<IdentificatorsDTO> identificators,
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

                    productService.verifyProduct(identificator.getProductId());

                    photosManager = photoManagerRepository.findByProductId(identificator.getProductId());

                    LOG.info("PRODUCT_ID 2: " + identificator.getProductId());

                    if (photosManager == null) {
                        identificator.setValid(true);
                        identificator.setMessage("ID Disponível");
                    } else {
                        if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.IN_PROGRESS) {
                            // Verifica se encontrou outro Grupo com o mesmo ID
                            List<GroupPhotos> foundGroupPhotos = photosManager.getGroupPhotos()
                                    .stream()
                                    .filter(group -> identificator.getProductId().equals(group.getProductId())
                                            && !identificator.getGroupId().equals(group.getId()))
                                    .collect(Collectors.toList());

                            if (foundGroupPhotos.size() >= 1) {
                                identificator.setMessage(
                                        "Essa peça já está com imagens associadas. A adição de novas imagens não é possível.");
                                identificator.setValid(false);
                                photosManager = photoManagerRepository.findByGroupId(identificator.getGroupId());

                            } else {
                                identificator.setValid(true);
                                identificator.setMessage("ID Disponível");
                            }
                        } else if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.FINISHED) {
                            identificator.setMessage(
                                    "Essa peça já está com imagens associadas. A adição de novas imagens não é possível.");
                            identificator.setValid(false);
                        }
                    }
                }
                // Se não econtrar um PHOTO_MANAGER com base no PRODUCT_ID informado, será feito
                // uma nova busca, informando o GROUP_ID
                if (photosManager == null) {
                    LOG.debug("GroupId {} ", identificator.getGroupId());
                    PhotosManager photoManagerGroup = photoManagerRepository.findByGroupId(identificator.getGroupId());
                    updatePhotoManager(photoManagerGroup, identificator);
                } else {
                    updatePhotoManager(photosManager, identificator);
                }

                response.add(identificator);
            } catch (RepassaException repassaException) {
                if (repassaException.getRepassaUtilError().getErrorCode().equals(PhotoError.PRODUCT_ID_INVALIDO
                        .getErrorCode())) {
                    identificator.setMessage("ID não encontrado");
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
                    photosManager = photoManagerRepository.findByProductId(identificator.getProductId());
                    if (photosManager == null) {
                        PhotosManager photoManagerGroup = photoManagerRepository
                                .findByGroupId(identificator.getGroupId());
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
        AtomicReference<String> urlImage = new AtomicReference<>(new String());
        String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(name));
        var objectKey = photosValidate.validatePathBucket(username, imageDTO.getDate());
        AtomicReference<PhotosManager> photosManager = new AtomicReference<>(new PhotosManager());

        for (var i = 0; i < imageDTO.getPhotoBase64().size(); i++) {
            PhotoBase64DTO photoBase64DTO = imageDTO.getPhotoBase64().get(i);
            Photo photo = Photo.builder().build();

            try {
                String[] nameAux = photoBase64DTO.getName().split("\\."); // [0] - Apenas o nome do arquivo
                String[] typeAux = photoBase64DTO.getType().split("\\/"); // [1] - Apenas a extensão do arquivo
                String newNameFile = nameAux[0] + "." + typeAux[1]; // Nome do arquivo com base na extensão que está sendo passada
                String newNameThumbnailFile = nameAux[0] + "_thumbnail" + "." + typeAux[1]; // Nome do arquivo com base na extensão que está sendo passada
                String objKey = objectKey.concat(newNameFile);
                String objThumbnailKey = objectKey.concat(newNameThumbnailFile);

                urlImage.set(awsConfig.getUrlBase() + "/" + objKey);

                PhotoInsertValidateDTO photoInsertValidate = photosValidate.validatePhoto(photoBase64DTO);

                String base64Data = PhotoUtils.extractDataBase64(photoBase64DTO.getBase64());
                String mimeType = PhotoUtils.getMimeTypeFromBase64(base64Data);

                photoBase64DTO.setName(newNameFile);
                photoBase64DTO.setUrlThumbNail(awsConfig.getUrlBase() + "/" + objThumbnailKey);

                if (photoInsertValidate.isValid()) {
                    awsS3Client.uploadBase64FileToS3(awsConfig.getBucketName(), objKey, base64Data, mimeType);

                    PhotoProcessed photoProcessed = this.savePhotoProcessingDynamo(photoBase64DTO, username, urlImage);
                    photosManager.set(savePhotoManager(imageDTO, urlImage.get(), photoProcessed));
                } else {
                    imageDTO.getPhotoBase64().set(i, photoInsertValidate.getPhoto());
                    photosManager.set(savePhotoManager(imageDTO, urlImage.get(), null));
                }

                boolean thumbnailExist = false;

                do {
                    thumbnailExist = awsS3Client.checkExistFileInBucket(awsConfig.getBucketName(), objThumbnailKey);

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        LOG.debug("Erro enquanto processa o Thumbnail ");
                    }
                } while (!thumbnailExist);
            } catch (RepassaException e) {
                LOG.debug("Erro ao tentar salvar as imagens para o GroupId {} ", imageDTO.getGroupId());
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return photosManager.get();
    }

    private void updatePhotoManager(PhotosManager photoManager, IdentificatorsDTO identificator)
            throws RepassaException {
        if (photoManager != null) {
            LOG.info("PHOTOMANAGER UPDATE: " + photoManager.getId());

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

            photoManagerRepository.savePhotosManager(photoManager);
        } else {
            throw new RepassaException(PhotoError.PHOTO_MANAGER_IS_NULL);
        }
    }

    public ChangeTypePhotoDTO changeStatusPhoto(ChangeTypePhotoDTO data) throws RepassaException {
        try {
            PhotosManager photoManager = photoManagerRepository.findByImageIdGroupId(data.getPhotoId(),
                    data.getGroupId());
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
                photoManagerRepository.savePhotosManager(photoManager);

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

        for (int pos = 0; pos < resultList.size(); pos++) {
            PhotoFilterResponseDTO photosFilter = resultList.get(pos);

            var photo = Photo.builder().namePhoto(photosFilter.getImageName())
                    .sizePhoto(photosFilter.getSizePhoto())
                    .id(photosFilter.getImageId())
                    .typePhoto(TypePhoto.getPosition(count.get()))
                    .urlPhoto(photosFilter.getOriginalImageUrl())
                    .urlThumbnail(photosFilter.getUrlThumbnail()).build();

            PhotoUtils.urlToBase64AndMimeType(photo.getUrlPhoto(), photo);

            CommonsUtil.validatePhoto(Long.valueOf(photo.getSizePhoto()), photo, awsConfig.getUrlBase());

            // Seta photoManager
            photoManager.setEditor(photosFilter.getEditedBy());
            photoManager.setDate(photosFilter.getUploadDate());
            photoManager.setId(UUID.randomUUID().toString());

            photos.add(photo);
            count.set(count.get() + 1);

            isPhotoValid.set(Boolean.parseBoolean(photosFilter.getIsValid()));

            if (photos.size() % 4 == 0) {
                managerGroupPhotos.addPhotos(photos, isPhotoValid);
                photos.clear();
                count.set(0);
                isPhotoValid.set(Boolean.TRUE);
            }

            if (photos.size() % 4 != 0 && resultList.size() - 1 == pos) {
                managerGroupPhotos.addPhotos(photos, isPhotoValid);
                photos.clear();
                count.set(0);
                isPhotoValid.set(Boolean.TRUE);
            }
        }
        photoManager.setStatusManagerPhotos(StatusManagerPhotos.IN_PROGRESS);
        photoManager.setGroupPhotos(groupPhotos);
        persistPhotoManagerDynamoDB(photoManager);
    }

    public void finishManager(String id, UserPrincipalDTO userPrincipalDTO) throws Exception {
        PhotosManager photosManager = finishManagerPhotos(id, userPrincipalDTO);
        updatePhotographyStatus(photosManager);
    }

    @Transactional
    public PhotosManager finishManagerPhotos(String id, UserPrincipalDTO userPrincipalDTO) throws Exception {
        if (Objects.isNull(id)) {
            throw new RepassaException(PhotoError.OBJETO_VAZIO);
        }

        var photosManager = photoManagerRepository.findById(id);

        if (Objects.isNull(photosManager)) {
            throw new RepassaException(PhotoError.OBJETO_VAZIO);
        }

        photosManager.setStatusManagerPhotos(StatusManagerPhotos.FINISHED);
        AtomicBoolean existError = new AtomicBoolean(false);
        AtomicBoolean existErrorProductId = new AtomicBoolean(false);
        photosManager.getGroupPhotos().forEach(group -> {

            if (group.getIdError() != null || group.getImageError() != null) {
                existError.set(true);
            }

            if (group.getProductId() == null) {
                existErrorProductId.set(true);
            }

            group.setStatusProduct(StatusProduct.FINISHED);
            group.setUpdateDate(LocalDateTime.now().toString());
        });

        if (existError.get()) {
            throw new RepassaException(PhotoError.GROUP_ERROR);
        }

        if (existErrorProductId.get()) {
            throw new RepassaException(PhotoError.PRODUCT_ID_NULL);
        }

        try {
            /**
             * Antes de salvar e finalizar as informações do Grupo, é necessário realizar a
             * movimentação das fotos entre os buckets
             */
            photosManager = moveBucket(photosManager, userPrincipalDTO);
            photoManagerRepository.savePhotosManager(photosManager);
            historyService.save(photosManager, userPrincipalDTO);
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_SALVAR_NO_DYNAMO);
        }

        List<GroupPhotos> groupPhotosList = photosManager.getGroupPhotos();
        int qtyFinisheds = 0;
        for (GroupPhotos groupPhotos : groupPhotosList) {
            if (groupPhotos.getStatusProduct().equals(StatusProduct.FINISHED))
                qtyFinisheds += 1;
        }
        List<String> bagIds = extractBagIdFromGroup(groupPhotosList);
        for (String bagId : bagIds) {
            try{
                historyService.savePhotographyStatusInHistory(Long.parseLong(bagId), "IN_PROGRESS", String.valueOf(qtyFinisheds));
            }catch (Exception e){
                throw new RepassaException(PhotoError.PHOTO_STATUS_ERROR);
            }
        }

        return photosManager;
    }

    public void updatePhotographyStatus(PhotosManager photosManager) {
        photosManager.getGroupPhotos().stream()
                .map(groupPhotos -> Long.parseLong(groupPhotos.getProductId()))
                .forEach(productId -> productRestClient.updatePhotographyStatus(productId));
    }

    public PhotosManager moveBucket(PhotosManager photosManager, UserPrincipalDTO userPrincipalDTO) {
        if (photosManager.getStatusManagerPhotos() == StatusManagerPhotos.FINISHED) {
            photosManager.getGroupPhotos().forEach(group -> {
                if (!group.getPhotos().isEmpty()) {
                    group.getPhotos().forEach(photo -> {
                        if (!photo.getUrlPhoto().isEmpty()) {
                            var photosValidate = new PhotosValidate();

                            // Chama o metodo para movimentar adicionar a imagem no bucket do Renova
                            String urlPhoto = movePhotoBucketRenova(photo.getUrlPhoto(), group.getProductId(), photo.getNamePhoto());
                            String urlThumbnail = movePhotoBucketRenova(photo.getUrlThumbnail(), group.getProductId(), photosValidate.generateThumbnailName(photo.getNamePhoto()));
                            /**
                             * Se ao mover a Foto, retornar SUCESSO, então poderá ser removida do bucket
                             * Seler Center
                             */
                            if (urlPhoto != null && urlThumbnail != null) {
                                photoRemoveService.remove(photo);

                                photo.setUrlPhoto(urlPhoto);
                                photo.setUrlThumbnail(urlThumbnail);
                            }
                        }
                    });
                }
            });
        }

        return photosManager;
    }

    public String movePhotoBucketRenova(String urlPhoto, String productId, String photoName) {
        try {
            // Abre uma conexão para a URL
            URL url = new URL(urlPhoto);
            InputStream is = url.openStream();

            // Lê os bytes da imagem
            byte[] imageBytes = is.readAllBytes();

            // Codifica os bytes para Base64
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);

            var photosValidate = new PhotosValidate();

            String objectKey = photosValidate.validatePathBucketRenova(productId, "original", photoName);
            String mimeType = PhotoUtils.getMimeTypeFromBase64(base64Data);

            return awsS3RenovaClient.uploadBase64FileToS3(awsConfig.getBucketNameRenova(), objectKey, base64Data, mimeType);
        } catch (Exception e) {
            LOG.error("Error 3" + e.getMessage());
            return null;
        }
    }

    public ProductPhotoListDTO findPhotoByProductId(String productId) throws RepassaException {
        LOG.info("Busca de fotos para o productId: {}", productId);
        try {
            final var photoManager = photoManagerRepository.findByProductId(productId);

            if (Objects.isNull(photoManager) || Objects.isNull(photoManager.getGroupPhotos())) {
                return ProductPhotoListDTO.builder().photos(List.of()).build();
            }

            final var groupPhotos = photoManager.getGroupPhotos().stream()
                    .filter(p -> productId.equals(p.getProductId()))
                    .findFirst()
                    .orElse(new GroupPhotos());

            LOG.info("Busca de fotos para o productId {} realizada com sucesso", productId);

            if (StatusProduct.FINISHED.equals(groupPhotos.getStatusProduct())) {
                var productPhotoDTOList = groupPhotos.getPhotos().stream()
                        .map(p -> ProductPhotoDTO.builder()
                                .id(p.getId())
                                .typePhoto(Objects.nonNull(p.getTypePhoto()) ? p.getTypePhoto().toString() : "")
                                .sizePhoto(p.getSizePhoto())
                                .namePhoto(p.getNamePhoto())
                                .urlPhoto(p.getUrlPhoto())
                                .build())
                        .toList();
                return ProductPhotoListDTO.builder().photos(productPhotoDTOList).build();
            } else {
                return ProductPhotoListDTO.builder().photos(List.of()).build();
            }
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_BUSCAR_IMAGENS);
        }
    }

    private void persistPhotoManagerDynamoDB(PhotosManager photosManager) throws RepassaException {
        try {
            photoManagerRepository.savePhotosManager(photosManager);
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_PERSISTIR);
        }
    }

    private PhotosManager savePhotoManager(ImageDTO imageDTO, String urlImage, PhotoProcessed photoProcessed)
            throws RepassaException {
        try {
            var photoManager = photoManagerRepository.findByGroupId(imageDTO.getGroupId());

            if (Objects.isNull(photoManager)) {
                throw new RepassaException(PhotoError.PHOTO_MANAGER_IS_NULL);
            }

            AtomicReference<String> urlImageTemp = new AtomicReference<>(urlImage);
            AtomicReference<Photo> photo = new AtomicReference<>(new Photo());
            photoManager.getGroupPhotos()
                    .forEach(groupPhotos -> {

                        if (Objects.equals(groupPhotos.getId(), imageDTO.getGroupId())) {
                            imageDTO.getPhotoBase64().forEach(photoTela -> {
                                String imageId = UUID.randomUUID().toString();

                                if (photoProcessed == null) {
                                    groupPhotos.setImageError(TypeError.IMAGE_ERROR.name());
                                    urlImageTemp.set(awsConfig.getErrorImage());
                                } else {
                                    groupPhotos.setImageError(null);
                                    imageId = photoProcessed.getImageId();
                                }

                                photo.set(Photo.builder()
                                        .id(imageId)
                                        .namePhoto(photoTela.getName())
                                        .urlPhoto(urlImageTemp.get())
                                        .sizePhoto(photoTela.getSize())
                                        .urlThumbnail(photoTela.getUrlThumbNail())
                                        .note(photoTela.getNote())
                                        .build());

                                groupPhotos.getPhotos().add(photo.get());
                            });
                        }

                    });

            photoManagerRepository.savePhotosManager(photoManager);

            return photoManager;
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_PERSISTIR);
        }
    }

    public PhotoProcessed savePhotoProcessingDynamo(PhotoBase64DTO photoBase64DTO, String username,
                                                    AtomicReference<String> urlImage) throws RepassaException {
        PhotoProcessed photoProcessed = new PhotoProcessed();

        photoProcessed.setEditedBy(username);
        photoProcessed.setIsValid("true");
        photoProcessed.setUploadDate(LocalDateTime.now().toString());
        photoProcessed.setId(UUID.randomUUID().toString());
        photoProcessed.setImageId(UUID.randomUUID().toString());
        photoProcessed.setSizePhoto(photoBase64DTO.getSize());
        photoProcessed.setImageName(photoBase64DTO.getName());
        photoProcessed.setUrlThumbnail(photoBase64DTO.getUrlThumbNail());
        photoProcessed.setOriginalImageUrl(urlImage.get());

        photoProcessingService.save(photoProcessed);
        return photoProcessed;
    }

    public void deletePhoto(String idPhoto, UserPrincipalDTO userPrincipalDTO) throws RepassaException {

        LOG.debug("Buscando Photo no Dynamo");
        PhotosManager photosManager = photoManagerRepository.findByImageId(idPhoto);
        if (Objects.isNull(photosManager)) {
            LOG.debug("Photo não encontrada no Dynamo");
            throw new RepassaException(PhotoError.PHOTO_MANAGER_IS_NULL);
        }

        try {
            photosManager.getGroupPhotos().forEach(groupPhotos -> {
                List<Photo> photos = groupPhotos.getPhotos();

                Iterator<Photo> iteratorPhotos = photos.iterator();
                while (iteratorPhotos.hasNext()) {
                    Photo photo = iteratorPhotos.next();
                    int sizePhoto = Integer.parseInt(photo.getSizePhoto());
                    if (isPhotoEqual(idPhoto, photo) && isEditorEquals(userPrincipalDTO, photosManager)) {
                        if (sizePhoto > 0) {
                            photoRemoveService.remove(photo);
                        }
                        iteratorPhotos.remove();
                    }
                }
            });
            LOG.debug("Atualizando o PhotosManager no Dynamo");
            photoManagerRepository.savePhotosManager(photosManager);
        } catch (RepassaException e) {
            throw new RepassaException(PhotoError.DELETE_PHOTO);
        }
    }

    public void deleteGroupsOfPhoto(String groupId) throws RepassaException {

        if (Objects.nonNull(groupId)) {
            PhotosManager photosManager = null;

            try {
                photosManager = photoManagerRepository.findByGroupId(groupId);
            } catch (Exception e) {
                throw new RepassaException(PhotoError.ERROR_SEARCH_DYNAMODB);
            }

            if (Objects.isNull(photosManager)) {
                LOG.debug("PhotoManager não encontrada no Banco de Dados");
                throw new RepassaException(PhotoError.PHOTO_MANAGER_IS_NULL);
            }

            PhotosManager finalPhotosManager = photosManager;
            finalPhotosManager.getGroupPhotos().forEach(groupPhotosPhotoManager -> {
                if (groupPhotosPhotoManager.getId().equals(groupId)) {
                    groupPhotosPhotoManager.getPhotos().forEach(photo -> {
                        photoRemoveService.remove(photo);
                    });
                }
            });

            finalPhotosManager.removeGroupById(groupId);

            LOG.debug("Atualizando o PhotosManager no Banco de Dados");
            photoManagerRepository.savePhotosManager(photosManager);
        }
    }

    public void deletePhotoManager(String photoManagerId) throws RepassaException {
        if (Objects.nonNull(photoManagerId)) {
            PhotosManager photosManager;
            try {
                photosManager = photoManagerRepository.findById(photoManagerId);
            } catch (Exception e) {
                throw new RepassaException(PhotoError.ERROR_SEARCH_DYNAMODB);
            }

            if (Objects.isNull(photosManager)) {
                LOG.debug("PhotoManager não encontrada no Banco de Dados");
                throw new RepassaException(PhotoError.PHOTO_MANAGER_IS_NULL);
            }

            photosManager.getGroupPhotos().forEach(groupPhotosPhotoManager -> {
                groupPhotosPhotoManager.getPhotos().forEach(photo -> {
                    photoRemoveService.remove(photo);
                });
            });

            photoManagerRepository.deletePhotoManager(photoManagerId);

        }
    }

    private static boolean isEditorEquals(UserPrincipalDTO userPrincipalDTO, PhotosManager photosManager) {
        String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(userPrincipalDTO.getFirtName()));
        return photosManager.getEditor().equals(username);
    }

    private static boolean isPhotoEqual(String idPhoto, Photo photo) {
        return Objects.nonNull(photo.getId()) && (photo.getId().equals(idPhoto));
    }

    public PhotoBagsResponseDTO findBagsForPhoto(int page, int size, String bagId, String email, String statusBag, String receiptDate, String receiptDateSecondary, String partner, String photographyStatus) throws RepassaException {
        List<BagsResponseDTO> history;
        BigInteger totalrecords;
        try {
            HistoryResponseDTO historyResponse = historyService.findHistorys(page, size, bagId, email, statusBag, receiptDate, receiptDateSecondary,
                    partner, photographyStatus, "MS-PHOTO");
            history = historyResponse.getBagsResponse();
            totalrecords = historyResponse.getTotalRecords();
        } catch (ClientWebApplicationException e) {
            throw new RepassaException(PhotoError.ERRO_AO_BUSCAR_SACOLAS);
        }

        List<BagsPhotoDTO> listSearch = new ArrayList<>();
        history.forEach(bagsResponseDTO -> {

            String photoQtyText = Objects.nonNull(bagsResponseDTO.getPhotographyQty()) ? bagsResponseDTO.getPhotographyQty() : "-";
            String totalQtytext = Objects.nonNull(bagsResponseDTO.getQtyApprovedItem()) ? bagsResponseDTO.getQtyApprovedItem() : "-";

            BagsPhotoDTO build = BagsPhotoDTO.builder()
                    .receiveDate(dateConverter(bagsResponseDTO.getReceivedDate()))
                    .registrationDate(dateConverter(bagsResponseDTO.getTriageDate()))
                    .bagId(Objects.nonNull(bagsResponseDTO.getBagId()) ? bagsResponseDTO.getBagId().toString() : "")
                    .bagStatus(bagsResponseDTO.getStatusBag())
                    .clientEmail(bagsResponseDTO.getEmail())
                    .partner(bagsResponseDTO.getPartner())
                    .photographyStatus(bagsResponseDTO.getPhotographyStatus())
                    .items(photoQtyText + "/" + totalQtytext)
                    .build();

            listSearch.add(build);
        });
        listSearch.sort(Comparator.comparing(BagsPhotoDTO::getReceiveDate).reversed());

        return new PhotoBagsResponseDTO(totalrecords, listSearch);

    }

    private LocalDateTime dateConverter(String dateString) {
        if (Objects.isNull(dateString))
            return null;

        String pattern = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            Date date = sdf.parse(dateString);
            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (ParseException exception) {
            return null;
        }

    }

    public List<ProductPhotographyDTO> findProductsByBagId(int page, int size, String bagId) throws RepassaException, JsonProcessingException {
        String tokenAuth = headers.getHeaderString(AUTHORIZATION);
        Response returnProducts = productRestClient.findBagsForProduct(page, size, bagId, tokenAuth);
        List<ProductPhotographyDTO> photographyDTOS = returnProducts.readEntity(new GenericType<List<ProductPhotographyDTO>>() {});
        return photoManagerRepository.findByIds(photographyDTOS);

    }


}
