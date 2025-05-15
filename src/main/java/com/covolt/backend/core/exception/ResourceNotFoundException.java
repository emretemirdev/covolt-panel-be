package com.covolt.backend.core.exception; // Paket yolunu kontrol et

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseApplicationException {

    // En çok kullanılacak constructor: Kaynak adı ve ID'si ile spesifik mesaj için
    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(ErrorCode.RES_001, HttpStatus.NOT_FOUND, resourceName, resourceId != null ? resourceId.toString() : "null");
    }

    // Sadece genel bir mesaj ile (eski kodlarla uyumluluk veya özel durumlar için)
    // Bu durumda mesajı ErrorCode.RES_001'in mesajıyla birleştirmek yerine direkt özel mesajı kullanabiliriz
    // veya daha genel bir ErrorCode (belki ErrorCode.GEN_001 altında bir alt kod) tanımlayabiliriz.
    // Şimdilik, eğer sadece mesaj veriliyorsa, bunu özel mesaj olarak kabul edelim ama yine de RES_001 ile ilişkilendirelim.
    public ResourceNotFoundException(String customMessage) {
        super(customMessage, ErrorCode.RES_001, HttpStatus.NOT_FOUND);
    }
}