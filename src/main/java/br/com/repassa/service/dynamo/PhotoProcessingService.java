package br.com.repassa.service.dynamo;

import br.com.repassa.config.DynamoClient;
import br.com.repassa.entity.dynamo.PhotoProcessed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class PhotoProcessingService extends PhotoAbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoProcessingService.class);
    private static final String TABLE_NAME = "PhotoProcessingTable";
    private static final String SUCCESSFUL_STATS = "successful";


    public void save(PhotoProcessed photoProcessed) {
        DynamoDbClient dynamoDB = DynamoClient.openDynamoDBConnection();
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("id", AttributeValue.builder().s(photoProcessed.getId()).build());
        item.put("bag_id", AttributeValue.builder().s(photoProcessed.getBagId()).build());
        //TODO: Verificar a data de upload
//        item.put("created_at", AttributeValue.builder().s(photoProcessed.get()).build());
        item.put("edited_by", AttributeValue.builder().s(photoProcessed.getEditedBy()).build());
        item.put("image_id", AttributeValue.builder().s(photoProcessed.getImageId().toString()).build());
        item.put("imagem_name", AttributeValue.builder().s(photoProcessed.getImageName()).build());
        item.put("is_valid", AttributeValue.builder().s(photoProcessed.getIsValid()).build());
        item.put("notes", AttributeValue.builder().s(SUCCESSFUL_STATS).build());
        item.put("original_image_url", AttributeValue.builder().s(photoProcessed.getOriginalImageUrl()).build());
        item.put("size_photo", AttributeValue.builder().s(photoProcessed.getSizePhoto()).build());
        item.put("thumbnail_base64", AttributeValue.builder().s(photoProcessed.getThumbnailBase64()).build());
        item.put("upload_date", AttributeValue.builder().s(photoProcessed.getUploadDate()).build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        try {
            dynamoDB.putItem(putItemRequest);
            LOGGER.debug("Item saved successfully!");
        } catch (DynamoDbException e) {
            LOGGER.error("Unable to save item. {}", photoProcessed.getId());
        }
    }
}
