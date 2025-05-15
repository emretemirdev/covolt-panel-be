package com.covolt.backend.core.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionInactiveException extends BaseApplicationException {
    public SubscriptionInactiveException() {
        super(ErrorCode.SUB_003, HttpStatus.FORBIDDEN); // Varsayılan olarak 403
    }

    public SubscriptionInactiveException(HttpStatus specificHttpStatus) { // Duruma göre farklı HTTP status ile
        super(ErrorCode.SUB_003, specificHttpStatus);
    }

    public SubscriptionInactiveException(String customMessage) {
        super(customMessage, ErrorCode.SUB_003, HttpStatus.FORBIDDEN);
    }
}