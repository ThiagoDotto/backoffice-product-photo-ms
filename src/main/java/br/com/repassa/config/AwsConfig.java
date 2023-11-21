package br.com.repassa.config;

import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@Getter
@ApplicationScoped
public class AwsConfig {

    @ConfigProperty(name = "s3.aws.bucket-name")
    String bucketName;

    @ConfigProperty(name = "s3.aws.access-key")
    String accessKey;

    @ConfigProperty(name = "s3.aws.secret-key")
    String secretKey;

    @ConfigProperty(name = "s3.aws.front-url")
    String cloudFrontURL;

    @ConfigProperty(name = "s3.aws.url-base")
    String urlBase;

    @ConfigProperty(name = "s3.aws.error-image")
    String errorImage;
}
