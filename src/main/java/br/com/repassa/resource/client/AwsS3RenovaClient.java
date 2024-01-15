package br.com.repassa.resource.client;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.AwsConfig;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.utils.PhotoUtils;
import br.com.repassa.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Singleton
@Slf4j
public class AwsS3RenovaClient {

    S3Client s3Client;

    @Inject
    AwsConfig awsConfig;

    @Inject
    AwsS3Client awsS3Client;

    AwsCredentialsProvider credentialsProvider;

    @PostConstruct
    private void init() {
        credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsConfig.getAccessKey(), awsConfig.getSecretKey()));
        s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.SA_EAST_1)
                .build();
    }

    public String uploadBase64FileToS3(String bucketName, String objectKey, String base64Data, String mimeType) throws RepassaException {
        log.info("Iniciando o upload da imagem no S3");
        try {
            byte[] data = java.util.Base64.getDecoder().decode(base64Data);

            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(mimeType)
                    .build(), RequestBody.fromBytes(data));
        } catch (IllegalArgumentException ignored) {
            throw new RepassaException(PhotoError.BASE64_INVALIDO);
        }
        log.info("Retornando endereco da imagem");

        return "https://" + bucketName + ".s3."+ Region.SA_EAST_1 +".amazonaws.com" + objectKey;
    }
}
