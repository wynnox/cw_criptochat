package com.project.model;

import lombok.Getter;

@Getter
public class ChatFileMessage {
    private final byte[] data;
    private final String fileName;
    private final String mimeType;
    private final String type;

    public ChatFileMessage(byte[] data, String fileName, String mimeType, String type) {
        this.data = data;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.type = type;
    }

}
