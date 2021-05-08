package com.stan.orders.handler.orders;

/**
 * Created by stan on 08/05/2021.
 */
public class ItemResponse {
    String productId;
    String productName;
    Double price;

    public ItemResponse(String productId, String productName, Double price) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Double getPrice() {
        return price;
    }
}