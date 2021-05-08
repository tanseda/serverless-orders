package com.stan.orders.handler.orders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stan.orders.handler.repository.DynamoDBAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stan on 08/05/2021.
 */
public class GetAllItemsForOrderFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static DynamoDBAdapter dynamoDbService = DynamoDBAdapter.getInstance();
    private static ItemsEntityMapper mapper = new ItemsEntityMapper();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LambdaLogger logger = context.getLogger();
        mapper = new ItemsEntityMapper();

        if (input == null || input.getPathParameters().isEmpty()) {
            return createResponse("{ \"error\": \"missing path variable\" }", 400);
        }

        final String param = input.getPathParameters().get("orderRef");
        List<ItemResponse> orders = getAllItemsByOrder(param, logger);

        if (orders.isEmpty()) {
            return createResponse("not found", 404);
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

    private List<ItemResponse> getAllItemsByOrder(String orderRef, LambdaLogger logger) {
        String hashKey = "ORDER-" + orderRef;

        logger.log("hashKey: " + gson.toJson(hashKey));
        return dynamoDbService.getItem(hashKey, mapper, logger);
    }
}
