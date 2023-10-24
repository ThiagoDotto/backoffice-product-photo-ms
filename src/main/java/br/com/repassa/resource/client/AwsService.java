package br.com.repassa.resource.client;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.exception.PhotoError;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Slf4j
public class AwsService {

    S3Client s3Client;

    private final String accessKey = "AKIAJWITEVM4HXREFLYA";
    private final String secretKey = "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";
    private final String cloudFrontURL = "https://assets-dev-curadoria.repassa.com.br";

    AwsCredentialsProvider credentialsProvider;

//    @Inject
//    public AwsService(@ConfigProperty(name = "s3.aws.access-key-id") String accessKey,
//                      @ConfigProperty(name = "s3.aws.secret-access-key") String secretKey,
//                      @ConfigProperty(name = "cloudfront.url") String cloudFrontURL,
//                      S3Client s3Client) {
//        this.accessKey = accessKey;
//        this.secretKey = secretKey;
//        this.s3Client = s3Client;
//        this.cloudFrontURL = cloudFrontURL;
//    }

//    @PostConstruct
//    private void init() {
//        credentialsProvider = StaticCredentialsProvider.create(
//                AwsBasicCredentials.create(accessKey, secretKey)
//        );
//        s3Client = S3Client.builder()
//                .credentialsProvider(credentialsProvider)
//                .region(Region.US_EAST_1)
//                .build();
//    }

    @PostConstruct
    private void init() {
        credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
        s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();
    }


    public String uploadBase64FileToS3(String bucketName, String objectKey, String base64Data) throws RepassaException {
        log.info("Iniciando o upload da imagem no S3");
        try {
            String[] parts = base64Data.split(",");
            if (parts.length == 2) {
                String contentType = parts[0].split(":")[1].split(";")[0];
                String base64 = parts[1];
                byte[] data = java.util.Base64.getDecoder().decode(base64);
                s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(contentType)
                        .build(), RequestBody.fromBytes(data));
            } else {
                throw new RepassaException(PhotoError.BASE64_INVALIDO);
            }
        } catch (IllegalArgumentException ignored) {
            throw new RepassaException(PhotoError.BASE64_INVALIDO);
        }
        log.info("Retornando endereco da imagem");

        return cloudFrontURL + "/" + objectKey;
    }

    public void removeImageByUrl(String bucketName, String url) {
        Pattern pattern = Pattern.compile("triage/.*");
        Matcher matcher = pattern.matcher(url);

        if(matcher.find()) {
            String objectKey = matcher.group();

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            log.info("Iniciando a remocao da imagem {} no s3", objectKey);

            try {
                s3Client.deleteObject(deleteObjectRequest);
            } catch (Exception e) {
                log.error("Falha na remocao da imagem {} no s3", objectKey);
            }

            log.info("Imagem {} removida do s3 com sucesso", objectKey);
        } else {
            log.info("Nao foi possivel extrair a key da URL: {}", url);
        }
    }
}
