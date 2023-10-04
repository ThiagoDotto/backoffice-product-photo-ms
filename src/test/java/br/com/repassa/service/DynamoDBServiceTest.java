package br.com.repassa.service;

import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

public class DynamoDBServiceTest {

    public Table table;

    public ItemCollection<ScanOutcome> listItem(String fieldFiltered, Map<String, Object> expressionAttributeValues) {
        return table.scan();
    }

}
