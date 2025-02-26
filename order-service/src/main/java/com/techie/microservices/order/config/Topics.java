package com.techie.microservices.order.config;

import com.techie.microservices.order.event.OrderReceivedEvent;
import com.techie.microservices.order.service.OrderService;
import com.techie.microservices.order.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Configuration
@Slf4j
public class Topics {

    private final OrderService orderService;

    public Topics(OrderService orderService) {
        this.orderService = orderService;
    }

    @Bean
    public NewTopic orders() {
        return TopicBuilder.name(Constants.ORDERS_TOPIC_NAME)
                .partitions(3)
                .compact()
                .build();
    }

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name(Constants.PAYMENT_ORDERS_TOPIC_NAME)
                .partitions(3)
                .compact()
                .build();
    }

    @Bean
    public NewTopic stockTopic() {
        return TopicBuilder.name(Constants.STOCK_ORDERS_TOPIC_NAME)
                .partitions(3)
                .compact()
                .build();
    }

    @Bean
    public KStream<Long, OrderReceivedEvent> stream(StreamsBuilder builder) {
        JsonSerde<OrderReceivedEvent> orderSerde = new JsonSerde<>(OrderReceivedEvent.class);
        KStream<Long, OrderReceivedEvent> stream = builder
                .stream(Constants.PAYMENT_ORDERS_TOPIC_NAME, Consumed.with(Serdes.Long(), orderSerde));

        stream.join(
                builder.stream(Constants.STOCK_ORDERS_TOPIC_NAME),
                orderService::confirm,
                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(10)),
                StreamJoined.with(Serdes.Long(), orderSerde, orderSerde)
        ).peek((k, o) -> log.info("Output: {}", o))
                .to(Constants.ORDERS_TOPIC_NAME);

        return stream;
    }

}
