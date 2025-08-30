package com.project.controller;

import com.project.model.Room;
import com.project.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class RoomController {
    private final RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<Room> createRoom(
            @RequestParam String algorithm,
            @RequestParam String mode,
            @RequestParam String padding
    ) {
        Room room = roomService.createRoom(algorithm, mode, padding);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/list")
    public ResponseEntity<List<Room>> listRooms() {
        return ResponseEntity.ok(roomService.listRooms());
    }

    @PostMapping("/close/{id}")
    public ResponseEntity<String> closeRoom(@PathVariable String id) {
        boolean closed = roomService.closeRoom(id);
        if (closed) {
            return ResponseEntity.ok("Комната закрыта: " + id);
        } else {
            return ResponseEntity.badRequest().body("Комната не найдена или уже закрыта");
        }
    }

    @PostMapping("/{roomId}/submitKey")
    public ResponseEntity<String> submitKey(
            @PathVariable String roomId,
            @RequestParam String userId,
            @RequestParam String publicKey
    ) {
        try {
            BigInteger Y = new BigInteger(publicKey);
            boolean ok = roomService.submitPublicKey(roomId, userId, Y);
            return ok ? ResponseEntity.ok("Ключ принят")
                    : ResponseEntity.badRequest().body("Комната закрыта");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка формата ключа");
        }
    }

    @GetMapping("/{roomId}/keys")
    public ResponseEntity<Map<String, BigInteger>> getKeys(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.getPublicKeys(roomId));
    }

}
