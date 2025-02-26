package com.techie.microservices.order.service;

import com.techie.microservices.order.client.InventoryClient;
import com.techie.microservices.order.dto.OrderRequest;
import com.techie.microservices.order.event.OrderPlacedEvent;
import com.techie.microservices.order.event.OrderReceivedEvent;
import com.techie.microservices.order.model.Order;
import com.techie.microservices.order.repository.OrderRepository;
import com.techie.microservices.order.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {

        var isProductInStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());
        if (isProductInStock) {
            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setPrice(orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())));
            order.setSkuCode(orderRequest.skuCode());
            order.setQuantity(orderRequest.quantity());
            orderRepository.save(order);

            // Send the message to Kafka Topic
            OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent();
            orderPlacedEvent.setOrderNumber(order.getOrderNumber());
            orderPlacedEvent.setEmail(orderRequest.userDetails().email());
            orderPlacedEvent.setFirstName(orderRequest.userDetails().firstName());
            orderPlacedEvent.setLastName(orderRequest.userDetails().lastName());
            log.info("Start - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);
            kafkaTemplate.send(Constants.ORDER_PLACED_NOTIFICATION_TOPIC_NAME, orderPlacedEvent);
            log.info("End - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);
        } else {
            throw new RuntimeException("Product with SkuCode " + orderRequest.skuCode() + " is not in stock");
        }
    }

    public OrderReceivedEvent confirm(OrderReceivedEvent orderPayment, OrderReceivedEvent inventory) {
        OrderReceivedEvent o = new OrderReceivedEvent();
        o.setId(orderPayment.getId());
        o.setProductId(orderPayment.getProductId());
        o.setQuantity(orderPayment.getQuantity());
        o.setPrice(orderPayment.getPrice());

        if (orderPayment.getStatus().equals(Constants.ACCEPT_STATUS) &&
                inventory.getStatus().equals(Constants.ACCEPT_STATUS)) {
            o.setStatus(Constants.ACCEPT_STATUS);
        } else if (orderPayment.getStatus().equals(Constants.REJECT_STATUS) &&
                        inventory.getStatus().equals(Constants.REJECT_STATUS)) {
            o.setStatus(Constants.REJECT_STATUS);
        } else if (orderPayment.getStatus().equals(Constants.REJECT_STATUS) ||
                        inventory.getStatus().equals(Constants.REJECT_STATUS)) {
            String source = orderPayment.getStatus().equals(Constants.REJECT_STATUS)
                    ? Constants.PAYMENT_STATUS : Constants.STOCK_STATUS;
            o.setStatus(Constants.ROLLBACK_STATUS);
            o.setSource(source);
        }

        return o;
    }
}
