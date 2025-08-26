package com.project.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/msg")
@RequiredArgsConstructor
public class MessageController {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/send/{roomId}")
    public ResponseEntity<String> sendMessage(
            @PathVariable String roomId,
            @RequestParam String from,
            @RequestParam String ciphertext
    ) {
        String topic = "room-" + roomId;
        kafkaTemplate.send(topic, from + ":" + ciphertext);
        return ResponseEntity.ok("Сообщение отправлено");
    }
}
