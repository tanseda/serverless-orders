package com.stan.orders.handler.customers;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stan.orders.handler.products.ProductResponse;
import com.stan.orders.handler.repository.EntityMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by stan on 08/05/2021.
 */
public class CustomerEntityMapper implements EntityMapper<OrderResponse> {
    String KEY_PREFIX = "CUSTOMER-" ;

    @Override
    public OrderResponse map(Map<String, AttributeValue> attributeValueMap) {
        if(attributeValueMap == null)
            return null;

        String customerId = attributeValueMap.get("par").getS();
        customerId = customerId.replace(KEY_PREFIX,"");

        String orderRef = attributeValueMap.get("sort").getS();

        List<ProductResponse> products = new ArrayList<>();
        if (attributeValueMap.containsKey("products")) {
            try {
                Gson gson = new Gson();
                products = gson.fromJson(attributeValueMap.get("products").getS(), new TypeToken<List<ProductResponse>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new OrderResponse.OrdersResponseBuilder()
                .withOrderRef(orderRef)
                .withCustomerId(customerId)
                .withProducts(products)
                .build();
    }
}
