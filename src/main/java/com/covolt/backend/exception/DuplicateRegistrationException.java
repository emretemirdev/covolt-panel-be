package com.covolt.backend.exception;

// Standart RuntimeException'dan miras alır.
// @ResponseStatus ANOTASYONU BU SINIFTA DEĞİLDİR. GLOBAL HANDLER'DA BELİRLENİR.
public class DuplicateRegistrationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateRegistrationException(String message) {
        super(message);
    }
}