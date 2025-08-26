package com.project.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RoomConsumer {

    @KafkaListener(topicPattern = "room-.*", groupId = "server-log")
    public void listen(ConsumerRecord<String, String> record) {
        System.out.println("[" + record.topic() + "] " + record.value());
    }
}

