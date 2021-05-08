package com.stan.orders.handler.products;


import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.stan.orders.handler.repository.EntityMapper;

import java.util.Map;

/**
 * Created by stan on 08/05/2021.
 */
public class ProductEntityMapper implements EntityMapper<ProductResponse> {
    String KEY_PREFIX = "ORDER-" ;

    //ORDER-12345	notebook_hardcover_red	56789	Red Notebook	19.99
    @Override
    public ProductResponse map(Map<String, AttributeValue> attributeValueMap) {
        if(attributeValueMap == null)
            return null;

        String productId = attributeValueMap.get("sort").getS();
        String productName = attributeValueMap.get("productName").getS();

        Double price = null;
        if (attributeValueMap.containsKey("price")) {
            price = Double.valueOf(attributeValueMap.get("price").getN());
        }

        return new ProductResponse(productId, productName, price);
    }

}
