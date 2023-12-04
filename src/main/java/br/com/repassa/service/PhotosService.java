package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.dto.UserPrincipalDTO;
import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.AwsConfig;
import br.com.repassa.dto.*;
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
import br.com.repassa.utils.PhotoUtils;
import br.com.repassa.utils.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class PhotosService {
    private static final Logger LOG = LoggerFactory.getLogger(PhotosService.class);

    @Inject
    AwsConfig awsConfig;

    @Inject
    ProductService productService;

    @Inject
    PhotoManagerRepository photoManagerRepository;

    @Inject
    PhotoProcessingService photoProcessingService;

    @Inject
    HistoryService historyService;

    @Inject
    AwsS3Client awsS3Client;

    @Inject
    AwsS3RenovaClient awsS3RenovaClient;

    @RestClient
    ProductRestClient productRestClient;

    @Inject
    RekognitionService rekognitionService;

    @Inject
    PhotoRemoveService photoRemoveService;

    public void filterAndPersist(final PhotoFilterDTO filter, final String name) throws RepassaException {

        LOG.info("Filter {}", filter.getDate());
        String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(name));

        LOG.info("Fetered by Name: {}", username);
        List<PhotoFilterResponseDTO> photoFilterResponseDTOS = this.photoProcessingService
                .listItensByDateAndUser(filter.getDate(), username);

        persistPhotoManager(photoFilterResponseDTOS);
    }

    public PhotosManager processBarCode(ProcessBarCodeRequestDTO processBarCodeRequestDTO, String user,
                                        String tokenAuth) throws RepassaException {
        List<ProcessBarCodeRequestDTO.GroupPhoto> groupPhotos = processBarCodeRequestDTO.getGroupPhotos();

        List<IdentificatorsDTO> validateIds = rekognitionService.PhotosRecognition(groupPhotos);

        if (validateIds.isEmpty()) {
            throw new RepassaException(AwsPhotoError.REKOGNITION_PHOTO_EMPTY);
        }

//        if (groupPhotos.size() == validateIds.size()) {
//            AtomicInteger count = new AtomicInteger(0);
//
//            validateIds.forEach(productId -> {
//                if (!productId.getValid()) {
//                    count.incrementAndGet();
//                }
//            });
//
//            if (count.get() == validateIds.size()) {
//                if (validateIds.size() > 1) {
//                    throw new RepassaException(AwsPhotoError.REKOGNITION_PRODUCT_ID_NOT_FOUND_N);
//                } else {
//                    throw new RepassaException(AwsPhotoError.REKOGNITION_PRODUCT_ID_NOT_FOUND);
//                }
//            }
//        }

        try {
            validateIdentificators(validateIds, tokenAuth, true);
        } catch (Exception e) {
            throw new RepassaException(AwsPhotoError.REKOGNITION_ERROR);
        }

        return searchPhotos(processBarCodeRequestDTO.getDate(), user);
    }

    public PhotosManager searchPhotos(String date, String name) {
        try {
            String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(name));
            LOG.info("Fetered by Name: {}", username);
            return photoManagerRepository.getByEditorUploadDateAndInProgressStatus(date, username);
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

                    productService.verifyProduct(identificator.getProductId(), tokenAuth);

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
                        PhotosManager photoManagerGroup = photoManagerRepository.findByGroupId(identificator.getGroupId());
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
            PhotoBase64DTO photo = imageDTO.getPhotoBase64().get(i);

            try {
                String[] nameAux = photo.getName().split("\\."); // [0] - Apenas o nome do arquivo
                String[] typeAux = photo.getType().split("\\/"); // [1] - Apenas a extensão do arquivo
                String newNameFile = nameAux[0] + "." + typeAux[1]; // Nome do arquivo com base na extensão que está sendo passada
                String newNameThumbnailFile = nameAux[0] + "_thumbnail" + "." + typeAux[1]; // Nome do arquivo com base na extensão que está sendo passada

                photo.setName(newNameFile);
                photo.setUrlThumbNail(newNameThumbnailFile);
                String objKey = objectKey.concat(newNameFile);
                String objThumbnailKey = objectKey.concat(newNameThumbnailFile);
                urlImage.set(awsConfig.getUrlBase() + objKey);
                PhotoInsertValidateDTO photoInsertValidate = photosValidate.validatePhoto(photo);
                String objThumbnailBase64 = PhotoUtils.thumbnail(photo.getBase64());

                if (photoInsertValidate.isValid()) {
                    awsS3Client.uploadBase64FileToS3(awsConfig.getBucketName(), objKey, photo.getBase64());
                    awsS3Client.uploadBase64FileToS3(awsConfig.getBucketName(), objThumbnailKey, objThumbnailBase64);
                    PhotoProcessed photoProcessed = this.savePhotoProcessingDynamo(photo, username, urlImage);
                    photosManager.set(savePhotoManager(imageDTO, urlImage.get(), photoProcessed));
                } else {
                    imageDTO.getPhotoBase64().set(i, photoInsertValidate.getPhoto());
                    photosManager.set(savePhotoManager(imageDTO, urlImage.get(), null));
                }

            } catch (RepassaException e) {
                LOG.debug("Erro ao tentar salvar as imagens para o GroupId {} ", imageDTO.getGroupId());
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
            PhotosManager photoManager = photoManagerRepository.findByImageAndGroupId(data.getPhotoId(), data.getGroupId());
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

        for (int pos = 0; pos <= resultList.size() - 1; pos++) {
            PhotoFilterResponseDTO photosFilter = resultList.get(pos);
            String[] imageName = photosFilter.getImageName().split("\\.");

            var photo = Photo.builder().namePhoto(photosFilter.getImageName())
                    .sizePhoto(photosFilter.getSizePhoto())
                    .id(photosFilter.getImageId())
                    .typePhoto(TypePhoto.getPosition(count.get()))
                    .urlPhoto(photosFilter.getOriginalImageUrl())
                    .urlThumbnail(photosFilter.getUrlThumbnail()).build();

            if (Boolean.FALSE.equals(PhotosValidate.extensionTypeValidation(imageName[1]))) {
                photo.setNote("Formato de arquivo inválido. São aceitos somente JPG ou JPEG");
            }

            if (PhotosValidate.isGreatThanMaxSize(photo.getSizePhoto())) {
                photo.setNote("Tamanho do arquivo inválido. São aceitos arquivos de até 15Mb");
            }

            // Seta photoManager
            photoManager.setEditor(photosFilter.getEditedBy());
            photoManager.setDate(photosFilter.getUploadDate());
            photoManager.setId(UUID.randomUUID().toString());

            photos.add(photo);
            count.set(count.get() + 1);

            if (Boolean.FALSE.equals(Boolean.valueOf(photosFilter.getIsValid()))) {
                isPhotoValid.set(Boolean.FALSE);
            }

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

    @Transactional
    public void finishManagerPhotos(String id, UserPrincipalDTO userPrincipalDTO, HttpHeaders headers) throws Exception {

        String tokenAuth = headers.getHeaderString("Authorization");

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
             * Antes de salvar e finalizar as informações do Grupo, é necessário realizar a movimentação das fotos
             * entre os buckets
             */
            photosManager = moveBucket(photosManager, userPrincipalDTO);
            photoManagerRepository.savePhotosManager(photosManager);
            historyService.save(photosManager, userPrincipalDTO, headers);
            photosManager.getGroupPhotos().stream()
                    .map(groupPhotos -> Long.parseLong(groupPhotos.getProductId()))
                    .forEach(productId -> productRestClient.updatePhotographyStatus(productId, tokenAuth));
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_SALVAR_NO_DYNAMO);
        }
    }

    public PhotosManager moveBucket(PhotosManager photosManager, UserPrincipalDTO userPrincipalDTO) throws RepassaException {
        if(photosManager.getStatusManagerPhotos() == StatusManagerPhotos.FINISHED) {
            photosManager.getGroupPhotos().forEach(group -> {
                if(!group.getPhotos().isEmpty()) {
                    group.getPhotos().forEach(photo -> {
                        if(!photo.getUrlPhoto().isEmpty()) {
                            //Chamar metodo para movimentar adicionar a imagem no bucket do Renova
                            String urlPhoto = movePhotoBucketRenova(photo.getUrlPhoto(), group.getProductId(), photo.getNamePhoto());
                            /**
                             * Se ao mover a Foto, retornar SUCESSO, então poderá ser removida do bucket Seler Center
                             */
                            if(urlPhoto != null) {
                                photoRemoveService.remove(photo);
                                photo.setUrlPhoto(urlPhoto);
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
            String base64String = Base64.getEncoder().encodeToString(imageBytes);

            var photosValidate = new PhotosValidate();

            String photoBase64 = "data:image/jpg;base64," + base64String;

            String objectKey = photosValidate.validatePathBucketRenova(productId, "original", photoName);
            String urlImage = awsConfig.getUrlBase() + objectKey;

            awsS3RenovaClient.uploadBase64FileToS3(awsConfig.getBucketNameRenova(), objectKey, photoBase64);

            return urlImage;
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
                                .urlPhoto(StringUtils.formatToCloudFrontURL(p.getUrlPhoto(), awsConfig.getCloudFrontURL()))
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
//        } catch (RepassaException e) {
//            throw new RepassaException(e.getRepassaUtilError());
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

    private static boolean isEditorEquals(UserPrincipalDTO userPrincipalDTO, PhotosManager photosManager) {
        String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(userPrincipalDTO.getFirtName()));
        return photosManager.getEditor().equals(username);
    }

    private static boolean isPhotoEqual(String idPhoto, Photo photo) {
        return Objects.nonNull(photo.getId()) && (photo.getId().equals(idPhoto));
    }
}
