package com.project.kafka;

import com.project.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/msg")
@RequiredArgsConstructor
public class MessageController {

    private final KafkaTemplate<String, ChatMessage> chatMessageKafkaTemplate;

    @PostMapping("/send/{roomId}")
    public ResponseEntity<String> sendMessage(
            @PathVariable String roomId,
            @RequestParam String from,
            @RequestParam String ciphertext
    ) {
        ChatMessage msg = new ChatMessage(roomId, from, ciphertext, System.currentTimeMillis());
        chatMessageKafkaTemplate.send("room-" + roomId, msg);
        return ResponseEntity.ok("Сообщение отправлено");
    }
}
