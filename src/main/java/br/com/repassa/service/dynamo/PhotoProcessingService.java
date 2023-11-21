package br.com.repassa.service.dynamo;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.DynamoClient;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.entity.dynamo.PhotoProcessed;
import br.com.repassa.exception.AwsPhotoError;
import br.com.repassa.exception.PhotoError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
public class PhotoProcessingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoProcessingService.class);
    private static final String TABLE_NAME = "PhotoProcessingTable";
    private static final String SUCCESSFUL_STATS = "successful";

    public void save(PhotoProcessed photoProcessed) throws RepassaException {
        try {
            DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();
            Map<String, AttributeValue> item = new HashMap<>();

            item.put("id", AttributeValue.builder().s(photoProcessed.getId()).build());
            item.put("created_at", AttributeValue.builder().s(LocalDateTime.now().toString()).build());
            item.put("edited_by", AttributeValue.builder().s(photoProcessed.getEditedBy()).build());
            item.put("image_id", AttributeValue.builder().s(photoProcessed.getImageId()).build());
            item.put("imagem_name", AttributeValue.builder().s(photoProcessed.getImageName()).build());
            item.put("is_valid", AttributeValue.builder().s(photoProcessed.getIsValid()).build());
            item.put("notes", AttributeValue.builder().s(SUCCESSFUL_STATS).build());
            item.put("original_image_url", AttributeValue.builder().s(photoProcessed.getOriginalImageUrl()).build());
            item.put("size_photo", AttributeValue.builder().s(photoProcessed.getSizePhoto()).build());
            item.put("thumbnail", AttributeValue.builder().s(photoProcessed.getUrlThumbnail()).build());
            item.put("upload_date", AttributeValue.builder().s(photoProcessed.getUploadDate()).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();

            dynamoDB.putItem(putItemRequest);
            LOGGER.debug("Item saved on DynamoDB successfully!");
        } catch (DynamoDbException | RepassaException e) {
            LOGGER.error("Unable to save item. {} on DynamoDB", photoProcessed.getId());
            throw new RepassaException(AwsPhotoError.DYNAMO_CONNECTION);
        }
    }

    public void removeItemByPhotoId(String photoId) throws RepassaException {
        try {
            DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

            Map<String, AttributeValue> itemFilterMap = new HashMap<>();
            itemFilterMap.put(":image_id", AttributeValue.builder().s(photoId).build());

            ScanRequest.Builder scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .filterExpression("image_id = :image_id")
                    .expressionAttributeValues(itemFilterMap);

            ScanResponse scanResponse = dynamoDB.scan(scanRequest.build());

            scanResponse.items().forEach(item -> {
                Map<String, AttributeValue> itemMap = new HashMap<>();

                String idItem = item.get("id").s();
                itemMap.put("id", AttributeValue.builder().s(idItem).build());

                // Criando solicitação para excluir o item
                DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .key(itemMap)
                        .build();

                // Excluindo item da tabela
                dynamoDB.deleteItem(deleteRequest);
                LOGGER.info("item {} excluído", photoId);
            });
        } catch (DynamoDbException e) {
            LOGGER.error("Error remove item photoId. {}", photoId);
            throw new RepassaException(AwsPhotoError.DYNAMO_CONNECTION);
        }
    }

    public void removeItemByOriginalImageUrl(String originalImageUrl) throws RepassaException {
        try {
            DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

            Map<String, AttributeValue> itemFilterMap = new HashMap<>();
            itemFilterMap.put(":original_image_url", AttributeValue.builder().s(originalImageUrl).build());

            ScanRequest.Builder scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .filterExpression("original_image_url = :original_image_url")
                    .expressionAttributeValues(itemFilterMap);

            ScanResponse scanResponse = dynamoDB.scan(scanRequest.build());

            scanResponse.items().forEach(item -> {
                Map<String, AttributeValue> itemMap = new HashMap<>();

                String idItem = item.get("id").s();
                itemMap.put("id", AttributeValue.builder().s(idItem).build());

                // Criando solicitação para excluir o item
                DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .key(itemMap)
                        .build();

                // Excluindo item da tabela
                dynamoDB.deleteItem(deleteRequest);
                LOGGER.info("item {} excluído", originalImageUrl);
            });
        } catch (DynamoDbException e) {
            LOGGER.error("Error remove item original_image_url. {}", originalImageUrl);
            throw new RepassaException(AwsPhotoError.DYNAMO_CONNECTION);
        }
    }

    public List<PhotoFilterResponseDTO> listItensByDateAndUser(String date, String username) throws RepassaException {

        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":upload_date", AttributeValue.builder().s(date).build());
        expressionAttributeValues.put(":edited_by", AttributeValue.builder().s(username).build());

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

        if (photoFilterResponseDTOS.isEmpty()) {
            LOGGER.error("Não há itens encontrados para a data informada. Selecione uma nova data ou tente novamente");
            throw new RepassaException(PhotoError.FOTOS_NAO_ENCONTRADA);
        } else {
            Collections.sort(photoFilterResponseDTOS,
                    Comparator.nullsFirst(Comparator.comparing(PhotoFilterResponseDTO::getImageName)));
        }
        return photoFilterResponseDTOS;
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
            responseDTO.setUrlThumbnail(item.get("thumbnail").s());
            responseDTO.setUploadDate(item.get("upload_date").s());

            resultList.add(responseDTO);
        });

        return resultList;
    }

    public PhotoFilterResponseDTO findPhoto(String idPhoto) {

        DynamoDbClient dynamoDB;
        try {
            dynamoDB = DynamoClient.openDynamoDBConnection();
            Map<String, AttributeValue> itemFilterMap = new HashMap<>();
            itemFilterMap.put(":image_id", AttributeValue.builder().s(idPhoto).build());

            ScanRequest.Builder scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .filterExpression("image_id = :image_id")
                    .expressionAttributeValues(itemFilterMap);

            ScanResponse scanResponse = dynamoDB.scan(scanRequest.build());
            List<PhotoFilterResponseDTO> photoFilterResponseDTOS = mapPhotoFilter(scanResponse);
            Optional<PhotoFilterResponseDTO> first = photoFilterResponseDTOS.stream().findFirst();
            return first.orElse(new PhotoFilterResponseDTO());
        } catch (RepassaException e) {
            LOGGER.error(e.getMessage());
        }
        return new PhotoFilterResponseDTO();
    }
}
