package com.covolt.backend.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice // Bu anotasyon, bu sınıfın tüm @Controller'lar için global bir exception handler olacağını belirtir.
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler { // ResponseEntityExceptionHandler bazı standart Spring MVC hatalarını handle etmek için iyi bir başlangıç noktasıdır.

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Kendi Tanımladığımız Özel Exception'lar İçin Handler'lar

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
        logger.warn("DuplicateResourceException yakalandı: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT)
                .message(ex.getMessage()) // Exception kendi mesajını (ErrorCode'dan formatlanmış) kullanır.
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // Statü: 409
    }

    // YENİ HANDLER: DuplicateRegistrationException için spesifik handler
    @ExceptionHandler(DuplicateRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRegistrationException(DuplicateRegistrationException ex, WebRequest request) {
        logger.warn("DuplicateRegistrationException yakalandı: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value()) // Exception constructor'ındaki statüyü kullan
                .error(HttpStatus.CONFLICT)         // Veya doğrudan ex.getHttpStatus() kullanılabilir.
                .message(ex.getMessage())           // Exception'ın taşıdığı özel mesajı kullan
                .path(extractPath(request))
                .build();
        // Not: DuplicateRegistrationException zaten Conflict statüsü ile tanımlı.
        // Bu handler'ı eklemek, onun generic Exception handler'a düşmesini engeller.
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // Statü: 409
    }

    // ... (MethodArgumentNotValidException handler dahil diğer mevcut handler'lar)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("ResourceNotFoundException yakalandı: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND)
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceCreationException.class) // Bu exception'ı daha önce tanımlamıştık sanırım
    public ResponseEntity<ErrorResponse> handleResourceCreationException(ResourceCreationException ex, WebRequest request) {
        logger.error("ResourceCreationException yakalandı: {}", ex.getMessage(), ex); // Log cause as well
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value()) // Genellikle kaynak oluşturma hatası client girdisiyle ilgilidir
                .error(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Auth ile ilgili exception'lar (Bunlar zaten core.exception altında olmalı)
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex, WebRequest request) {
        logger.warn("TokenRefreshException yakalandı: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value()) // veya FORBIDDEN, duruma göre
                .error(HttpStatus.UNAUTHORIZED)
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(LockedException ex, WebRequest request) {
        logger.warn("LockedException yakalandı: Hesap kilitli. {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED)
                .message("Hesabınız kilitlenmiştir.") // Daha kullanıcı dostu bir mesaj
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    // Opsiyonel: Subscription ile ilgili exception'lar (bunları da core.exception altına almıştık)
    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFoundException(SubscriptionNotFoundException ex, WebRequest request) {
        logger.warn("SubscriptionNotFoundException yakalandı: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND)
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SubscriptionInactiveException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionInactiveException(SubscriptionInactiveException ex, WebRequest request) {
        logger.warn("SubscriptionInactiveException yakalandı: {}", ex.getMessage());
        // Bu, login sırasında 401, diğer durumlarda 403 olabilir. Şimdilik genel bir 403 (Forbidden) verelim.
        // Veya AuthServiceImpl'de fırlatırken context'e göre farklı bir exception fırlatılabilir.
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN)
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }


    // 2. Spring Security Kaynaklı Exception'lar

    @ExceptionHandler(BadCredentialsException.class) // Login hatası
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        logger.warn("BadCredentialsException yakalandı: Login denemesi başarısız. {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED)
                .message("E-posta veya şifre yanlış.") // Güvenlik için genel mesaj
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class) // Yetkilendirme hatası (@PreAuthorize vb.)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("AccessDeniedException yakalandı: Yetkisiz erişim denemesi. {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN)
                .message("Bu kaynağa erişim yetkiniz bulunmamaktadır.")
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // 3. Jakarta Bean Validation (@Valid) Hataları (ResponseEntityExceptionHandler'dan override)
    // Bu metod, DTO'lardaki @NotBlank, @Size gibi validasyonlar patladığında çalışır.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logger.warn("MethodArgumentNotValidException yakalandı: Validasyon hatası. {}", ex.getMessage());
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        List<String> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST)
                .message("Validasyon hatası oluştu.")
                .path(extractPath(request))
                .fieldErrors(fieldErrors.isEmpty() ? null : fieldErrors)
                .globalErrors(globalErrors.isEmpty() ? null : globalErrors)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 4. Genel (Beklenmedik) Hatalar İçin Bir Fallback Handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("Beklenmedik bir hata oluştu: {}", ex.getMessage(), ex); // Stack trace'i de logla
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Sunucuda beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.")
                .path(extractPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Yardımcı metot
    private String extractPath(WebRequest request) {
        try {
            // "uri=" prefix'ini kaldır
            return request.getDescription(false).substring(4);
        } catch (Exception e) {
            return "Bilinmeyen yol";
        }
    }
}