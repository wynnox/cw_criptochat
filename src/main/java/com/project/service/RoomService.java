package com.project.service;

import com.project.crypto.keyx.DhParams;
import com.project.model.Room;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom(String algorithm, String mode, String padding) {
        String id = UUID.randomUUID().toString();
        DhParams params = DhParams.generate(512, 40, new SecureRandom());
        Room room = new Room(id, algorithm, mode, padding, params.p, params.q, params.g);
        rooms.put(id, room);
        return room;
    }

    public List<Room> listRooms() {
        return new ArrayList<>(rooms.values());
    }

    public boolean closeRoom(String id) {
        Room room = rooms.get(id);
        if (room != null && room.isActive()) {
            room.close();
            return true;
        }
        return false;
    }

    public boolean submitPublicKey(String roomId, String userId, BigInteger Y) {
        Room room = rooms.get(roomId);
        if (room != null && room.isActive()) {
            room.addPublicKey(userId, Y);
            return true;
        }
        return false;
    }

    public Map<String, BigInteger> getPublicKeys(String roomId) {
        Room room = rooms.get(roomId);
        return (room != null) ? room.getPublicKeys() : Map.of();
    }
}
