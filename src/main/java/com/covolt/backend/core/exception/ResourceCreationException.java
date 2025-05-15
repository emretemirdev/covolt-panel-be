package com.covolt.backend.core.exception;

import org.springframework.http.HttpStatus;

public class ResourceCreationException extends BaseApplicationException {
    public ResourceCreationException(String details) {
        super(ErrorCode.RES_003, HttpStatus.BAD_REQUEST, details);
    }

    public ResourceCreationException(String details, Throwable cause) {
        super(cause, ErrorCode.RES_003, HttpStatus.BAD_REQUEST, details);
    }
}