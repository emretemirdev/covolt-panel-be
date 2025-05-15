package com.covolt.backend.core.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseApplicationException {

    // Bu constructor, ErrorCode.RES_002'nin mesajını formatlamak için kullanılır.
    // Örnek Kullanım: throw new DuplicateResourceException(ErrorCode.RES_002, "Rol Adı: " + roleName);
    // VEYA ErrorCode.RES_002'nin mesaj template'i zaten "%s zaten mevcut" ise:
    // throw new DuplicateResourceException(ErrorCode.RES_002, roleName);
    public DuplicateResourceException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, HttpStatus.CONFLICT, messageArgs);
    }

    // Eğer illa ki String mesajla çağırmak istediğimiz bir yer varsa (eski kullanımlar gibi)
    // ve bunu ErrorCode.RES_002 ile ilişkilendirmek istiyorsak,
    // BaseApplicationException'daki customMessage alan constructor'ı kullanabiliriz.
    // Ama bu durumda yukarıdaki ile çakışmaması için parametre sayısı/tipi farklı olmalı.
    // Şimdilik bunu kaldıralım ve servislerde ErrorCode ile exception fırlatmaya odaklanalım.
    /*
    public DuplicateResourceException(String customMessage) {
        super(customMessage, ErrorCode.RES_002, HttpStatus.CONFLICT);
    }
    */
}