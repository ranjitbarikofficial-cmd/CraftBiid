package com.craftbid.exception;

public class AlreadyJoinedException extends RuntimeException {

    private final String code = "ALREADY_JOINED";

    public AlreadyJoinedException(String message) {
        super(message);
    }

    public String getCode() {
        return code;
    }
}
