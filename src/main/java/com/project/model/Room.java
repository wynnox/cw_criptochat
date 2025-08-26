package com.project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private String id;
    private String algorithm;
    private String mode;
    private String padding;
    private boolean active;

    private BigInteger p;
    private BigInteger q;
    private BigInteger g;

    private Map<String, BigInteger> publicKeys = new ConcurrentHashMap<>();

    public Room(String id, String algorithm, String mode, String padding,
                BigInteger p, BigInteger q, BigInteger g) {
        this.id = id;
        this.algorithm = algorithm;
        this.mode = mode;
        this.padding = padding;
        this.active = true;
        this.p = p;
        this.q = q;
        this.g = g;
    }

    public void close() { this.active = false; }

    public void addPublicKey(String userId, BigInteger Y) {
        publicKeys.put(userId, Y);
    }
}
