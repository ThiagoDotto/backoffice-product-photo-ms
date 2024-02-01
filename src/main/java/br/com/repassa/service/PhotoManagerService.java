package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.AwsConfig;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.enums.TypePhoto;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.repository.aws.PhotoManagerRepository;
import br.com.repassa.repository.aws.PhotoProcessingRepository;
import br.com.repassa.utils.CommonsUtil;
import br.com.repassa.utils.PhotoUtils;
import br.com.repassa.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class PhotoManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoManagerService.class);

    PhotoProcessingRepository photoProcessingRepository;

    PhotoManagerRepository photoManagerRepository;
    AwsConfig awsConfig;

    @Inject
    public PhotoManagerService(PhotoProcessingRepository photoProcessingRepository) {
        this.photoProcessingRepository = photoProcessingRepository;
    }

    public PhotosManager photoManagerBuilder(List<PhotoFilterResponseDTO> resultList) throws RepassaException {
        return this.createPhotoManagerGroup(resultList);


    }


    private PhotosManager createPhotoManagerGroup(List<PhotoFilterResponseDTO> resultList) throws RepassaException {
        LOG.info("Iniciando processo de persistencia");

        var photoManager = new PhotosManager();
        List<GroupPhotos> groupPhotos = new ArrayList<>();
        List<Photo> photos = new ArrayList<>(4);
        var managerGroupPhotos = new ManagerGroupPhotos(groupPhotos);

        AtomicInteger count = new AtomicInteger();
        AtomicBoolean isPhotoValid = new AtomicBoolean(Boolean.TRUE);
//TODO: Regra para criar o grupo de 4 fotos.
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
        //TODO: Salva o grupo fotos no dynamo.
        // Retornar já esse objeto para o front
        return photoManager;
    }

    public void filterAndPersist(final String date, final String username) throws RepassaException {

        LOG.info("Filter {}", filter.getDate());
        String username = StringUtils.replaceCaracterSpecial(StringUtils.normalizerNFD(name));

        LOG.info("Fetered by Name: {}", username);
        List<PhotoFilterResponseDTO> photoFilterResponseDTOS = this.photoProcessingRepository
                .listItensByDateAndUser(filter.getDate(), username);

//        persistPhotoManager(photoFilterResponseDTOS);
    }

    private void persistPhotoManager(List<PhotoFilterResponseDTO> resultList) throws RepassaException {
        LOG.info("Iniciando processo de persistencia");

        var photoManager = new PhotosManager();
        List<GroupPhotos> groupPhotos = new ArrayList<>();
        List<Photo> photos = new ArrayList<>(4);
        var managerGroupPhotos = new ManagerGroupPhotos(groupPhotos);

        AtomicInteger count = new AtomicInteger();
        AtomicBoolean isPhotoValid = new AtomicBoolean(Boolean.TRUE);
//TODO: Regra para criar o grupo de 4 fotos.
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
        //TODO: Salva o grupo fotos no dynamo.
        // Retornar já esse objeto para o front
        persistPhotoManagerDynamoDB(photoManager);
    }

    public void persistPhotoManagerDynamoDB(PhotosManager photosManager) throws RepassaException {
        try {
            photoManagerRepository.savePhotosManager(photosManager);
        } catch (Exception e) {
            throw new RepassaException(PhotoError.ERRO_AO_PERSISTIR);
        }
    }
}
