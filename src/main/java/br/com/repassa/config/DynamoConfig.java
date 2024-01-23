package br.com.repassa.config;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.exception.AwsPhotoError;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DynamoConfig {

    //    @ConfigProperty(name = "dynamo.table.photoProcessing")
    String photoProcessingTable = ConfigProvider.getConfig().getValue("dynamo.table.photoProcessing", String.class);

    //    @ConfigProperty(name = "dynamo.table.photosManager")
    String photosManager = ConfigProvider.getConfig().getValue("dynamo.table.photosManager", String.class);


    static String ACEESSKEY = ConfigProvider.getConfig().getValue("s3.aws.access-key", String.class);
    static String SECRETKEY = ConfigProvider.getConfig().getValue("s3.aws.secret-key", String.class);

    private static DynamoDbClient clientInstance;

    private DynamoConfig() {
    }

    public static synchronized DynamoDbClient openDynamoDBConnection() throws RepassaException {
        if (clientInstance == null) {
            try {
                long inicio = System.currentTimeMillis();
                System.out.println("inicio da Conexao ");
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(ACEESSKEY, SECRETKEY);
                clientInstance = DynamoDbClient.builder()
                        .region(Region.US_EAST_1)
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .build();
                long fim = System.currentTimeMillis();
                System.out.printf("Fim Conexão %.3f ms%n", (fim - inicio) / 1000d);
            } catch (Exception e) {
                throw new RepassaException(AwsPhotoError.DYNAMO_CONNECTION, e);
            }
        }

        return clientInstance;
    }

    public static void closeDynamoDbConnection() {
        clientInstance.close();
    }


    public String getPhotoProcessingTable() {
        return photoProcessingTable;
    }

    public String getPhotosManager() {
        return photosManager;
    }
}