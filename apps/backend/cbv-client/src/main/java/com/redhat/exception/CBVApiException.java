package com.redhat.exception;

public class CBVApiException extends RuntimeException {

    private final int status;
    private final String downstreamMessage;
    private final String downstreamPath;

    public CBVApiException(int status, String downstreamMessage, String downstreamPath) {
        super(downstreamMessage);
        this.status = status;
        this.downstreamMessage = downstreamMessage;
        this.downstreamPath = downstreamPath;
    }

    public int getStatus() {
        return status;
    }

    public String getDownstreamMessage() {
        return downstreamMessage;
    }

    public String getDownstreamPath() {
        return downstreamPath;
    }
}
