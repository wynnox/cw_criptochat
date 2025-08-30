package com.project.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomConsumer {

    @KafkaListener(topicPattern = "room-.*", groupId = "chat-clients")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String topic = record.topic();
            String value = record.value();
            log.info("[{}] {}", topic, value);
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения: {}", e.getMessage(), e);
        }
    }
}

