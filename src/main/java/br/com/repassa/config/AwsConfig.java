package br.com.repassa.config;

import lombok.Getter;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AwsConfig {

//    @ConfigProperty(name = "s3.aws.bucket-name")
    String bucketName = ConfigProvider.getConfig().getValue("s3.aws.bucket-name", String.class);

//    @ConfigProperty(name = "s3.aws.access-key")
    String accessKey = ConfigProvider.getConfig().getValue("s3.aws.access-key", String.class);

//    @ConfigProperty(name = "s3.aws.secret-key")
    String secretKey = ConfigProvider.getConfig().getValue("s3.aws.secret-key", String.class);

//    @ConfigProperty(name = "s3.aws.front-url")
    String cloudFrontURL = ConfigProvider.getConfig().getValue("s3.aws.front-url", String.class);

//    @ConfigProperty(name = "s3.aws.url-base")
    String urlBase = ConfigProvider.getConfig().getValue("s3.aws.url-base", String.class);

//    @ConfigProperty(name = "s3.aws.error-image")
    String errorImage = ConfigProvider.getConfig().getValue("s3.aws.error-image", String.class);


    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getCloudFrontURL() {
        return cloudFrontURL;
    }

    public void setCloudFrontURL(String cloudFrontURL) {
        this.cloudFrontURL = cloudFrontURL;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public String getErrorImage() {
        return errorImage;
    }

    public void setErrorImage(String errorImage) {
        this.errorImage = errorImage;
    }
}
