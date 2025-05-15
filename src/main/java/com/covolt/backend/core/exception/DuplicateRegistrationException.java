package com.covolt.backend.core.exception; // Veya modules.authentication.exception

import org.springframework.http.HttpStatus;

public class DuplicateRegistrationException extends BaseApplicationException {
    // Hangi alanın duplicate olduğunu belirtmek için
    public DuplicateRegistrationException(String fieldName, String fieldValue) {
        super(ErrorCode.RES_002, HttpStatus.CONFLICT, fieldName + ": " + fieldValue);
    }

    // Daha genel bir mesaj için
    public DuplicateRegistrationException(String message) {
        super(message, ErrorCode.RES_002, HttpStatus.CONFLICT);
    }
}