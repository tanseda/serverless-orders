package com.stan.orders.handler.orders;


import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.stan.orders.handler.repository.EntityMapper;

import java.util.Map;

/**
 * Created by stan on 08/05/2021.
 */
public class ItemsEntityMapper implements EntityMapper<ItemResponse> {

    @Override
    public ItemResponse map(Map<String, AttributeValue> attributeValueMap) {
        if(attributeValueMap == null)
            return null;

        String productId = attributeValueMap.get("sort").getS();
        String productName = attributeValueMap.get("productName").getS();

        Double price = null;
        if (attributeValueMap.containsKey("price")) {
            price = Double.valueOf(attributeValueMap.get("price").getN());
        }

        return new ItemResponse(productId, productName, price);
    }

}
