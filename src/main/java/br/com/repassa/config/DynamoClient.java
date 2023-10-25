package br.com.repassa.config;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.exception.AwsPhotoError;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoClient {

    private static DynamoDbClient clientInstance;

    private DynamoClient() {
    }

    public static synchronized DynamoDbClient openDynamoDBConnection() throws RepassaException {
        if (clientInstance == null) {
            try {
                var accessKey = "AKIA3GWR6GFBSXT2QB52";
                var secretKey = "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";
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

                clientInstance = DynamoDbClient.builder()
                        .region(Region.US_EAST_1)
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .build();

            } catch (Exception e) {
                throw new RepassaException(AwsPhotoError.DYNAMO_CONNECTION, e);
            }
        }

        return clientInstance;
    }

    public static void closeDynamoDbConnection() {
        clientInstance.close();
    }

}