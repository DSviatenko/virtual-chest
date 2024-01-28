package com.sviatenterprise.exception;

public class UploadFileException extends RuntimeException {
    public UploadFileException(Throwable cause) {
        super(cause);
    }

    public UploadFileException(String message) {
        super(message);
    }

    public UploadFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
