package br.com.repassa.client;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

public class RekognitionBarClient {

    public RekognitionClient openConnection() {
    	 String accessKeyId = "AKIA3GWR6GFBSXT2QB52";
         String secretAccessKey = "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";

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        Region region = Region.US_EAST_1; 
        RekognitionClient rekognitionClient = RekognitionClient.builder()
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .build();

        return rekognitionClient;
    }
    
}
