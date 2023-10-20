package br.com.repassa.resource.client;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.repassa.entity.PhotosManager;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class PhotosManagerRepositoryImpl  {
	
    private final DynamoDbClient dynamoDB;
    private final String TABLE_NAME = "PhotosManager";

    public PhotosManagerRepositoryImpl(DynamoDbClient dynamoDBClient) {
        this.dynamoDB = dynamoDBClient;
    }

    public <S extends PhotosManager> S save(S entity) {
	
        ObjectMapper map = new ObjectMapper();
        String writeValueAsString = null;
		try {
			writeValueAsString = map.writeValueAsString(entity.getGroupPhotos());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		 Map<String, AttributeValue> item = new HashMap<>();
		    item.put("id", AttributeValue.builder().s(entity.getId()).build());
		    item.put("editor", AttributeValue.builder().s( entity.getEditor()).build());
		    item.put("upload_date", AttributeValue.builder().s(entity.getDate()).build());
		    item.put("statusManagerPhotos", AttributeValue.builder().s(entity.getStatusManagerPhotos().toString()).build());
		    item.put("groupPhotos", AttributeValue.builder().s(writeValueAsString).build());
		    
		    PutItemRequest putItemRequest = PutItemRequest.builder()
		        .tableName(TABLE_NAME)
		        .item(item)
		        .build();
		    
		    try {
		    	dynamoDB.putItem(putItemRequest);
		        System.out.println("Item saved successfully!");
		    } catch (Exception e) {
		        System.err.println("Unable to save item.");
		        e.printStackTrace();
		    }
        
        return entity;
    }

}
