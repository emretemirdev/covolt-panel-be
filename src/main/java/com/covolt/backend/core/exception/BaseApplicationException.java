package com.covolt.backend.core.exception; // Paket yolunun doğru olduğundan emin ol

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter // Lombok ile getter'ları otomatik oluşturalım
public abstract class BaseApplicationException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode; // ErrorCode enum'umuzu kullanıyoruz
    private final transient Object[] messageArgs; // Mesaj formatlama için argümanlar (transient: JSON'a serialize edilmesin)

    /**
     * En yaygın kullanılacak constructor.
     * ErrorCode içindeki mesaj template'ini ve verilen argümanları kullanarak mesaj oluşturur.
     *
     * @param errorCode Uygulamaya özgü hata kodu enum'u.
     * @param httpStatus Bu hata durumunda döndürülecek HTTP statüsü.
     * @param messageArgs ErrorCode'daki mesaj template'ini formatlamak için kullanılacak argümanlar.
     */
    protected BaseApplicationException(ErrorCode errorCode, HttpStatus httpStatus, Object... messageArgs) {
        super(errorCode.formatMessage(messageArgs)); // ErrorCode'dan formatlanmış mesajı al ve ata
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.messageArgs = messageArgs; // Bunları saklayabiliriz, belki loglama veya detay için lazım olur.
    }

    /**
     * Bir önceki hatayı (cause) da zincire eklemek için.
     *
     * @param cause Bu hataya neden olan bir önceki exception.
     * @param errorCode Uygulamaya özgü hata kodu enum'u.
     * @param httpStatus Bu hata durumunda döndürülecek HTTP statüsü.
     * @param messageArgs ErrorCode'daki mesaj template'ini formatlamak için kullanılacak argümanlar.
     */
    protected BaseApplicationException(Throwable cause, ErrorCode errorCode, HttpStatus httpStatus, Object... messageArgs) {
        super(errorCode.formatMessage(messageArgs), cause); // cause'u da ata
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Nadiren, ErrorCode'daki varsayılan mesajı tamamen override etmek istediğimizde
     * ve yine de bir ErrorCode ile ilişkilendirmek istediğimizde kullanılabilir.
     *
     * @param customMessage Bu exception için kullanılacak özel mesaj.
     * @param cause Bu hataya neden olan bir önceki exception (opsiyonel, null olabilir).
     * @param errorCode Uygulamaya özgü hata kodu enum'u.
     * @param httpStatus Bu hata durumunda döndürülecek HTTP statüsü.
     * @param messageArgs Bu constructor'da doğrudan kullanılmaz ama loglama veya detay için saklanabilir.
     */
    protected BaseApplicationException(String customMessage, Throwable cause, ErrorCode errorCode, HttpStatus httpStatus, Object... messageArgs) {
        super(customMessage, cause); // Verilen özel mesajı kullan
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    protected BaseApplicationException(String customMessage, ErrorCode errorCode, HttpStatus httpStatus, Object... messageArgs) {
        super(customMessage);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
    // Getter'lar Lombok tarafından sağlanıyor.
    // getMessage() metodu zaten RuntimeException'dan geliyor ve bizim constructor'da set ettiğimiz mesajı döndürecek.
}