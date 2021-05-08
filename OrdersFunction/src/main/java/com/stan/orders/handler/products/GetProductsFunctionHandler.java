package com.stan.orders.handler.products;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stan.orders.handler.repository.DynamoDBAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for requests to Lambda function.
 */
public class GetProductsFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final DynamoDBAdapter dynamoDbService = DynamoDBAdapter.getInstance();
    private static final ProductEntityMapper mapper = new ProductEntityMapper();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LambdaLogger logger = context.getLogger();

        if (input == null || input.getPathParameters().isEmpty()) {
            return createResponse("{ \"error\": \"missing path variable\" }", 400);
        }

        final String param = input.getPathParameters().get("productId");

        Optional<ProductResponse> product = getProductInternal(param, logger);

        if (product.isPresent()) {
            return createResponse(gson.toJson(product.get()), 200);
        }

        return createResponse("not found", 404);
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

    private Optional<ProductResponse> getProductInternal(String productId, LambdaLogger logger) {
        String hashKey = "PRODUCT-" + productId;

        logger.log("hashKey: " + gson.toJson(hashKey));

        return dynamoDbService.getItem(hashKey, mapper, logger)
                .stream()
                .findFirst();
    }
}
