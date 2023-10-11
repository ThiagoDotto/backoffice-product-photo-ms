package br.com.repassa.client;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoClient {

	private static DynamoDbClient clientInstance;
	
	public static synchronized DynamoDbClient openDynamoDBConnection() {
		 if (clientInstance == null) {
			 System.out.println("CRIOU CONEXAOO >>>>" );
	        try {
	            var accessKey = "AKIA3GWR6GFBSXT2QB52";
	            var secretKey = "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";
	            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
	
	            clientInstance = DynamoDbClient.builder()
	                    .region(Region.US_EAST_1)
	                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
	                    .build();
	
	        } catch (Exception e) {
	            System.out.println(e.getMessage());
	            return null;
	        }
		 } else {
			 System.out.println("NAO CRIOU CONEXAOO >>>>" );
		 }
		 
		return clientInstance;
    }

    public void closeDynamoDbConnection(DynamoDbClient dynamoDB) {
    	dynamoDB.close();
    }

}