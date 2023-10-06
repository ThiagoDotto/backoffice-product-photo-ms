package br.com.repassa.client;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

public class DynamoDbClient {

    public DynamoDB openDynamoDBConnection() {
        try {
            var accessKey = "AKIA3GWR6GFBSXT2QB52";
            var secretKey = "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";
            var awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

            var client = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .build();

            return new DynamoDB(client);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void closeDynamoDbConnection(DynamoDB dynamoDB) {
        dynamoDB.shutdown();
    }
}