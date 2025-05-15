package com.covolt.backend.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL) // null alanları JSON çıktısına dahil etme
@Data
@Builder
public class ErrorResponse {

    private Instant timestamp; // Hatanın meydana geldiği zaman
    private int status; // HTTP durum kodu (sayısal)
    private HttpStatus error; // HTTP durum kodu (enum adı)
    private String message; // Hatanın genel açıklaması
    private String path; // Hatanın tetiklendiği request path'i (opsiyonel)

    // Validasyon hataları için ek detaylar
    private Map<String, String> fieldErrors; // Alan adlarına göre hata mesajları
    private List<String> globalErrors; // Genel hatalar

    // ... Ek bilgi alanları buraya eklenebilir (örn. custom error codes)
}