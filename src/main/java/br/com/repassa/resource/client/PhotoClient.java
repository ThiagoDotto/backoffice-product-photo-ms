package br.com.repassa.resource.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.DynamoClient;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.exception.PhotoError;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class PhotoClient {
    private static final Logger LOG = LoggerFactory.getLogger(PhotoClient.class);
    private static final String TABLE_NAME_PHOTOS = "PhotosManager";

    public void savePhotosManager(PhotosManager manager) throws RepassaException {
        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();
        PhotosManagerRepositoryImpl impl = new PhotosManagerRepositoryImpl(dynamoDB);
        impl.save(manager);
    }

    public PhotosManager findByProductId(String productId) throws Exception {
        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":groupPhotos",
                AttributeValue.builder().s("\"productId\":\"" + productId + "\"").build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE_NAME_PHOTOS)
                .filterExpression("contains(groupPhotos, :groupPhotos)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager findByImageId(String imageId) throws Exception {
        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":groupPhotos", AttributeValue.builder().s("\"id\":\"" + imageId + "\"").build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE_NAME_PHOTOS)
                .filterExpression("contains(groupPhotos, :groupPhotos)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager findByImageAndGroupId(String imageId, String groupId) throws Exception {
        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":groupPhotos", AttributeValue.builder().s("\"id\":\"" + imageId + "\"").build());
        expressionAttributeValues.put(":groupPhotos", AttributeValue.builder().s("\"id\":\"" + groupId + "\"").build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE_NAME_PHOTOS)
                .filterExpression("contains(groupPhotos, :groupPhotos)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager findByGroupId(String groupId) throws Exception {

        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":groupPhotos", AttributeValue.builder().s("\"id\":\"" + groupId + "\"").build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE_NAME_PHOTOS)
                .filterExpression("contains(groupPhotos, :groupPhotos)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager findById(String id) throws Exception {

        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":id", AttributeValue.builder().s(id).build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE_NAME_PHOTOS)
                .filterExpression("id = :id")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager getPhotos(Map<String, AttributeValue> expressionAttributeValues)
            throws RepassaException {
        PhotosManager responseDTO = null;

        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        try {

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME_PHOTOS)
                    .filterExpression(
                            "statusManagerPhotos = :statusManagerPhotos and contains(upload_date, :upload_date) and editor = :editor")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse items = dynamoDB.scan(scanRequest);

            for (Map<String, AttributeValue> item : items.items()) {

                responseDTO = new PhotosManager();
                responseDTO.setId(item.get("id").s());
                responseDTO.setDate(item.get("upload_date").s());
                responseDTO.setEditor(item.get("editor").s());
                responseDTO.setStatusManagerPhotos(StatusManagerPhotos.valueOf(item.get("statusManagerPhotos").s()));
                String json = item.get("groupPhotos").s();
                ObjectMapper objectMapper = new ObjectMapper();
                List<GroupPhotos> readValue = null;
                try {
                    readValue = objectMapper.readValue(json, new TypeReference<List<GroupPhotos>>() {
                    });
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                responseDTO.setGroupPhotos(readValue);
                break;
            }

            return responseDTO;
        } catch (Exception e) {
            log.error("Erro nao esperado ao buscar as Fotoso no DynamoDB");
            e.printStackTrace();
            throw new RepassaException(PhotoError.ERRO_AO_BUSCAR_IMAGENS);
        }
    }

    private PhotosManager parseJsonToObject(ScanResponse items) throws RepassaException, JsonProcessingException {
        PhotosManager responseDTO = null;

        if (items.count() == 0) {
            return null;
        }

        for (Map<String, AttributeValue> item : items.items()) {
            responseDTO = new PhotosManager();
            responseDTO.setId(item.get("id").s());
            responseDTO.setDate(item.get("upload_date").s());
            responseDTO.setEditor(item.get("editor").s());
            responseDTO.setStatusManagerPhotos(StatusManagerPhotos.valueOf(item.get("statusManagerPhotos").s()));
            String json = item.get("groupPhotos").s();
            ObjectMapper objectMapper = new ObjectMapper();
            List<GroupPhotos> readValue = objectMapper.readValue(json, new TypeReference<List<GroupPhotos>>() {
            });
            responseDTO.setGroupPhotos(readValue);
        }

        return responseDTO;
    }

}
