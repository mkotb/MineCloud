package io.minecloud.models.server;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ServerMetadata {
    private String key;
    private String value;

    public ServerMetadata() {
    }

    public ServerMetadata(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
