package br.com.repassa.repository.aws;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.DynamoConfig;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.exception.AwsPhotoError;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.resource.client.PhotosManagerRepositoryImpl;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class PhotoManagerRepository {

    @Inject
    DynamoConfig dynamoConfig;

    public void savePhotosManager(PhotosManager manager) throws RepassaException {
        DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();
        PhotosManagerRepositoryImpl impl = new PhotosManagerRepositoryImpl(dynamoDB, dynamoConfig);
        impl.save(manager);
    }

    public PhotosManager findByProductId(String productId) throws Exception {
        DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":groupPhotos",
                AttributeValue.builder().s("\"productId\":\"" + productId + "\"").build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(dynamoConfig.getPhotosManager())
                .filterExpression("contains(groupPhotos, :groupPhotos)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager findByImageId(String imageId) throws RepassaException {
        try {
            DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

            expressionAttributeValues.put(":groupPhotos",
                    AttributeValue.builder().s("\"id\":\"" + imageId + "\"").build());

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(dynamoConfig.getPhotosManager())
                    .filterExpression("contains(groupPhotos, :groupPhotos)")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse items = dynamoDB.scan(scanRequest);

            return parseJsonToObject(items);
        } catch (DynamoDbException | JsonProcessingException e) {
            throw new RepassaException(AwsPhotoError.DYNAMO_CONNECTION);
        }
    }

    public PhotosManager findByImageIdGroupId(String imageId, String groupId) throws Exception {
        DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":groupPhotos", AttributeValue.builder().s("\"id\":\"" + imageId + "\"").build());

        if(groupId != null) {
            expressionAttributeValues.put(":groupPhotos", AttributeValue.builder().s("\"id\":\"" + groupId + "\"").build());
        }

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(dynamoConfig.getPhotosManager())
                .filterExpression("contains(groupPhotos, :groupPhotos)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager findByGroupId(String groupId) throws Exception {

        DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":groupPhotos", AttributeValue.builder().s("\"id\":\"" + groupId + "\"").build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(dynamoConfig.getPhotosManager())
                .filterExpression("contains(groupPhotos, :groupPhotos)")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public PhotosManager findById(String id) throws Exception {

        DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":id", AttributeValue.builder().s(id).build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(dynamoConfig.getPhotosManager())
                .filterExpression("id = :id")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        ScanResponse items = dynamoDB.scan(scanRequest);

        return parseJsonToObject(items);
    }

    public void deletePhotoManager(String photoManagerId) throws RepassaException {
        try {
            DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();

            HashMap<String, AttributeValue> keyToGet = new HashMap<>();
            keyToGet.put("id", AttributeValue.builder().s(photoManagerId).build());

            DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                    .tableName(dynamoConfig.getPhotosManager())
                    .key(keyToGet)
                    .build();

            dynamoDB.deleteItem(deleteReq);
        } catch (RepassaException | DynamoDbException e) {
            throw new RepassaException(PhotoError.ERROR_FAILED_CONNECT_DYNAMODB);
        }
    }

    public PhotosManager getByEditorUploadDateAndStatus(String date, String userName, String lastEvaluatedKey)
            throws RepassaException {

        try {
            PhotosManager responseDTO = null;
            DynamoDbClient dynamoDB = DynamoConfig.openDynamoDBConnection();

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":editor", AttributeValue.builder().s(userName).build());
            expressionAttributeValues.put(":upload_date", AttributeValue.builder().s(date).build());

            Map<String, AttributeValue> lastEvaluatedKey = lastEvaluatedKey != null ? lastEvaluatedKey : null;

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(dynamoConfig.getPhotosManager())
                    .filterExpression("contains(upload_date, :upload_date) and editor = :editor")
                    .expressionAttributeValues(expressionAttributeValues)
                    .limit(10)
                    .exclusiveStartKey()
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
            log.error("Erro nao esperado ao buscar as Fotos no DynamoDB");
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
