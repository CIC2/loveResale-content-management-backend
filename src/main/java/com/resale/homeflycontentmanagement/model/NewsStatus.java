package com.resale.homeflycontentmanagement.model;

public enum NewsStatus {
    ACTIVE(0),
    INACTIVE(1);

    private final int code;

    NewsStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static NewsStatus fromCode(int code) {
        for (NewsStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid status code: " + code);
    }
}


