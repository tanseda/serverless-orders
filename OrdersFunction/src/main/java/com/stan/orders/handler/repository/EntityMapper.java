package com.stan.orders.handler.repository;


import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

/**
 * Created by stan on 08/05/2021.
 */
public interface EntityMapper<T> {
    T map(Map<String, AttributeValue> attributeValueMap);
}
