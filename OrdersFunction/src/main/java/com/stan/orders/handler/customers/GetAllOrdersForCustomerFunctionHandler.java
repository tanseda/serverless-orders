package com.stan.orders.handler.customers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stan.orders.handler.products.ProductEntityMapper;
import com.stan.orders.handler.products.ProductResponse;
import com.stan.orders.handler.repository.DynamoDBAdapter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Handler for requests to Lambda function.
 */
public class GetAllOrdersForCustomerFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final DynamoDBAdapter dynamoDbService = DynamoDBAdapter.getInstance();
    private static final CustomerEntityMapper mapper = new CustomerEntityMapper();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LambdaLogger logger = context.getLogger();

        if (input == null || input.getPathParameters().isEmpty()) {
            return createResponse("{ \"error\": \"missing path variable\" }", 400);
        }

        final String param = input.getPathParameters().get("customerId");

        List<OrderResponse> orders = getOrdersByCustomer(param, logger);

        if (orders.isEmpty()) {
            return createResponse("{ \"error\": \"no records found\" }", 404);
        }

        try {
            return createResponse(gson.toJson(orders),200);
        } catch (Exception e) {
            return createResponse(e.getMessage(), 500);
        }
    }

    private APIGatewayProxyResponseEvent createResponse(String body, int statusCode) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        return response
                .withStatusCode(statusCode)
                .withBody(body);
    }

    private List<OrderResponse> getOrdersByCustomer(String customerId, LambdaLogger logger) {
        String hashKey = "CUSTOMER-" + customerId;

        logger.log("hashKey: " + gson.toJson(hashKey));

        return dynamoDbService.getItem(hashKey, mapper, logger);
    }

}
