package com.techie.microservices.order.util;

public final class Constants {
    private Constants() {}

    public static final String ORDER_PLACED_NOTIFICATION_TOPIC_NAME = "order-placed-notification";
    public static final String ORDERS_TOPIC_NAME = "orders";
    public static final String PAYMENT_ORDERS_TOPIC_NAME = "payment-orders";
    public static final String STOCK_ORDERS_TOPIC_NAME = "stock-orders";
    public static final String ACCEPT_STATUS = "ACCEPT";
    public static final String REJECT_STATUS = "REJECT";
    public static final String ROLLBACK_STATUS = "ROLLBACK";
    public static final String PAYMENT_STATUS = "PAYMENT";
    public static final String STOCK_STATUS = "STOCK";
}
