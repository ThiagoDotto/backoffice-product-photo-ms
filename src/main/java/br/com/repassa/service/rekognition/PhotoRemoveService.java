package br.com.repassa.service.rekognition;


import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.AwsConfig;
import br.com.repassa.entity.Photo;
import br.com.repassa.repository.aws.PhotoProcessingRepository;
import br.com.repassa.resource.client.AwsS3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PhotoRemoveService {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoRemoveService.class);

    AwsConfig awsConfig;
    PhotoProcessingRepository photoProcessingRepository;
    AwsS3Client awsS3Client;

    @Inject
    public PhotoRemoveService(AwsConfig awsConfig, PhotoProcessingRepository photoProcessingRepository, AwsS3Client awsS3Client) {
        this.awsConfig = awsConfig;
        this.photoProcessingRepository = photoProcessingRepository;
        this.awsS3Client = awsS3Client;
    }

    public void remove(Photo photo) {
        String bucketName = awsConfig.getBucketName();
        try {
            if (!photo.getUrlThumbnail().isEmpty()) {
                LOG.info("Removendo Photo {} no S3", photo.getUrlThumbnail());
                awsS3Client.removeImageByUrl(bucketName, photo.getUrlThumbnail().replace("+", " "));
                LOG.info("Removendo Photo {} na tabela ProcessingTable", photo.getUrlThumbnail());
                photoProcessingRepository.removeItemByOriginalImageUrl(photo.getUrlThumbnail());
            }

            LOG.info("Removendo Photo {} no S3", photo.getId());
            awsS3Client.removeImageByUrl(bucketName, photo.getUrlPhoto().replace("+", " "));
            LOG.info("Removendo Photo {} na tabela ProcessingTable", photo.getId());
            photoProcessingRepository.removeItemByPhotoId(photo.getId());
        } catch (RepassaException e) {
            LOG.error("Erro ao remover a PhotoID {} na tabela ProcessingTable, erro: {} ", photo.getId(), e.getMessage());
        }
    }
}

