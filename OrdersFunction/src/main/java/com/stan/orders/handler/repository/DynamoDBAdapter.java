package com.stan.orders.handler.repository;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by stan on 08/05/2021.
 */
public class DynamoDBAdapter {
    private static DynamoDBAdapter instance;
    private final AmazonDynamoDB client;

    private DynamoDBAdapter() {
        this.client = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(Regions.EU_WEST_2)
                .build();
    }

    public static DynamoDBAdapter getInstance() {
        if (instance == null) {
            instance = new DynamoDBAdapter();
        }

        return instance;
    }

    public <T> List<T> getItem(String id, EntityMapper<T> mapper, LambdaLogger logger) {
        Map<String, AttributeValue> attrValues =
                new HashMap<>();
        attrValues.put(":v_id", new AttributeValue(id));

        QueryRequest queryReq = new QueryRequest(System.getenv("TABLE_NAME"));
        queryReq.setKeyConditionExpression("par = :v_id");
        queryReq.setExpressionAttributeValues(attrValues);

        logger.log(queryReq.toString());
        List<Map<String, AttributeValue>> result = client.query(queryReq).getItems();

        return result.stream()
                .map(mapper::map)
                .collect(Collectors.toList());
    }

}
