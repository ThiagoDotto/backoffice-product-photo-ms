package br.com.repassa.service.dynamo;

import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

public abstract class PhotoAbstractService {

    private static final String TABLE_NAME = "PhotoProcessingTable";


    protected ScanRequest scanRequest() {
        return ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build();
    }
//
//    protected PutItemRequest putRequest(Fruit fruit) {
//        Map<String, AttributeValue> item = new HashMap<>();
//        item.put(FRUIT_NAME_COL, AttributeValue.builder().s(fruit.getName()).build());
//        item.put(FRUIT_DESC_COL, AttributeValue.builder().s(fruit.getDescription()).build());
//
//        return PutItemRequest.builder()
//                .tableName(FRUIT_TABLE_NAME)
//                .item(item)
//                .build();
//    }
//
//    protected GetItemRequest getRequest(String name) {
//        Map<String, AttributeValue> key = new HashMap<>();
//        key.put(FRUIT_NAME_COL, AttributeValue.builder().s(name).build());
//
//        return GetItemRequest.builder()
//                .tableName(FRUIT_TABLE_NAME)
//                .key(key)
//                .attributesToGet(FRUIT_NAME_COL, FRUIT_DESC_COL)
//                .build();
//    }
}
