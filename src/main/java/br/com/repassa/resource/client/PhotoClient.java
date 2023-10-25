package br.com.repassa.resource.client;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.DynamoClient;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.enums.StatusManagerPhotos;
import br.com.repassa.exception.PhotoError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

@Slf4j
@ApplicationScoped
public class PhotoClient {
    private static final Logger LOG = LoggerFactory.getLogger(PhotoClient.class);
    private static final String TABLE_NAME = "PhotoProcessingTable_QA";
    private static final String TABLE_NAME_PHOTOS = "PhotosManager_QA";

    public void savePhotosManager(PhotosManager manager) {
        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();
        PhotosManagerRepositoryImpl impl = new PhotosManagerRepositoryImpl(dynamoDB);
        impl.save(manager);
    }

    public List<PhotoFilterResponseDTO> listItem(Map<String, AttributeValue> expressionAttributeValues)
            throws RepassaException {

        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        List<PhotoFilterResponseDTO> photoFilterResponseDTOS = new ArrayList<PhotoFilterResponseDTO>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        do {

            ScanRequest.Builder scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .filterExpression("contains(upload_date, :upload_date) AND edited_by = :edited_by")
                    .expressionAttributeValues(expressionAttributeValues);

            if (lastEvaluatedKey != null) {
                scanRequest.exclusiveStartKey(lastEvaluatedKey);
            }

            ScanResponse items = dynamoDB.scan(scanRequest.build());
            photoFilterResponseDTOS.addAll(mapPhotoFilter(items));
            lastEvaluatedKey = items.lastEvaluatedKey();

        } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        if (photoFilterResponseDTOS.size() == 0) {
            log.error("Não há itens encontrados para a data informada. Selecione uma nova data ou tente novamente");
            throw new RepassaException(PhotoError.FOTOS_NAO_ENCONTRADA);
        } else {
            Collections.sort(photoFilterResponseDTOS,
                    Comparator.nullsFirst(Comparator.comparing(PhotoFilterResponseDTO::getImageName)));
        }
        return photoFilterResponseDTOS;
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

    private List<PhotoFilterResponseDTO> mapPhotoFilter(ScanResponse scanResponse) {

        List<PhotoFilterResponseDTO> resultList = new ArrayList<>();

        scanResponse.items().forEach(item -> {
            String imageName = item.get("imagem_name").s();

            if (imageName == null) {
                imageName = "undefined";
            }

            PhotoFilterResponseDTO responseDTO = new PhotoFilterResponseDTO();
            responseDTO.setBagId(item.get("bag_id").s());
            responseDTO.setEditedBy(item.get("edited_by").s());
            responseDTO.setImageName(imageName);
            responseDTO.setId(item.get("id").s());
            responseDTO.setImageId(item.get("image_id").s());
            responseDTO.setIsValid(item.get("is_valid").s());
            responseDTO.setOriginalImageUrl(item.get("original_image_url").s());
            responseDTO.setSizePhoto(item.get("size_photo").s());
            responseDTO.setThumbnailBase64(item.get("thumbnail_base64").s());
            responseDTO.setUploadDate(item.get("upload_date").s());

            resultList.add(responseDTO);
        });

        return resultList;
    }
}
