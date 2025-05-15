package com.covolt.backend.core.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends BaseApplicationException {
    public SubscriptionNotFoundException(String planIdentifier) { // Plan adı veya ID'si
        super(ErrorCode.SUB_005, HttpStatus.NOT_FOUND, planIdentifier);
    }

    public SubscriptionNotFoundException() { // Genel "aktif abonelik yok" durumu
        super(ErrorCode.SUB_001, HttpStatus.NOT_FOUND);
    }
}