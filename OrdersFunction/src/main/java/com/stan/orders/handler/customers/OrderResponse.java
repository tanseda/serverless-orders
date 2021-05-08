package com.stan.orders.handler.customers;


import com.stan.orders.handler.products.ProductResponse;

import java.util.List;

/**
 * Created by stan on 08/05/2021.
 */
public class OrderResponse {
    String orderRef;
    String customerId;
    List<ProductResponse> products;

    public OrderResponse(String orderRef, String customerId, List<ProductResponse> products) {
        this.orderRef = orderRef;
        this.customerId = customerId;
        this.products = products;
    }

    public static class OrdersResponseBuilder{
        String orderRef;
        String customerId;
        List<ProductResponse> products;

        public OrdersResponseBuilder withOrderRef(String orderRef) {
            this.orderRef = orderRef;
            return this;
        }

        public OrdersResponseBuilder withCustomerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public OrdersResponseBuilder withProducts(List<ProductResponse> products) {
            this.products = products;
            return this;
        }

        public OrderResponse build() {
            return new OrderResponse(this.orderRef, this.customerId, this.products);
        }
    }

}
