package com.covolt.backend.core.exception;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum ErrorCode {

    // --- Genel Sistem Hataları (SYS_xxx) ---
    SYS_001("Bilinmeyen bir sunucu hatası oluştu. Lütfen daha sonra tekrar deneyin veya sistem yöneticisine başvurun."),
    SYS_002("İstek formatı geçersiz veya beklenmeyen bir yapıya sahip."),
    SYS_003("Yapılandırma hatası: Gerekli bir sistem parametresi eksik veya yanlış."),
    SYS_004("Dış servislerle iletişimde bir sorun oluştu: %s."),

    // --- Kaynak Yönetimi Hataları (RES_xxx) ---
    RES_001("Aranan kaynak bulunamadı: %s (ID: %s)."),
    RES_002("Kaynak zaten mevcut: %s bilgisiyle daha önce kayıt oluşturulmuş."),
    RES_003("Kaynak oluşturulurken bir hata oluştu: %s."),
    RES_004("Kaynak güncellenirken bir hata oluştu: %s."),
    RES_005("Kaynak silinirken bir hata oluştu: %s."),
    RES_006("Kaynak kullanımda olduğu için bu işlem gerçekleştirilemiyor: %s."),

    // --- Validasyon Hataları (VAL_xxx) ---
    VAL_001("Geçersiz istek: Lütfen girdiğiniz verileri kontrol edin."),
    VAL_002("Zorunlu alan eksik: '%s' alanı boş bırakılamaz."),
    VAL_003("Geçersiz format: '%s' alanı için beklenen formatta değil."),
    VAL_004("Değer aralığı dışında: '%s' alanı %s ile %s arasında olmalıdır."),
    VAL_005("Minimum uzunluk sağlanamadı: '%s' alanı en az %d karakter olmalıdır."),
    VAL_006("Maksimum uzunluk aşıldı: '%s' alanı en fazla %d karakter olabilir."),

    // --- Kimlik Doğrulama Hataları (AUTH_xxx) ---
    AUTH_001("Geçersiz e-posta veya şifre."),
    AUTH_002("JWT token süresi dolmuş. Lütfen tekrar giriş yapın."),
    AUTH_003("Geçersiz veya hatalı biçimlendirilmiş JWT token."),
    AUTH_004("Hesabınız askıya alınmış veya kilitlenmiş. Lütfen yönetici ile iletişime geçin."),
    AUTH_005("Yenileme tokeni bulunamadı veya geçersiz."),
    AUTH_006("Yenileme tokeninin süresi dolmuş. Lütfen tekrar giriş yapın."),
    AUTH_007("E-posta adresi henüz doğrulanmamış."),
    AUTH_008("Maksimum hatalı giriş denemesi aşıldı, hesabınız geçici olarak kilitlendi."),

    // --- Yetkilendirme Hataları (AUTHZ_xxx) ---
    AUTHZ_001("Bu kaynağa veya işleme erişim yetkiniz bulunmamaktadır."),
    AUTHZ_002("İşlem reddedildi: Yetersiz yetki seviyesi."),

    // --- Abonelik Hataları (SUB_xxx) ---
    SUB_001("Firmanız için aktif bir abonelik planı bulunamadı."),
    SUB_002("Abonelik planınız bu özelliği (%s) desteklemiyor."),
    SUB_003("Abonelik süreniz dolmuş veya aboneliğiniz pasif durumda."),
    SUB_004("Deneme aboneliği başlatılırken bir sorun oluştu."),
    SUB_005("Abonelik planı bulunamadı: %s."),
    SUB_006("Abonelik planı limiti aşıldı: %s (Limit: %d, Mevcut: %d)."),

    // --- Firma Yönetimi Hataları (COMP_xxx) ---
    COMP_001("Firma adı zaten kullanımda."),
    COMP_002("Firma silinemez, aktif kullanıcılara veya kaynaklara sahip."),

    // --- Kullanıcı Hataları (USER_xxx) ---
    USER_001("E-posta adresi zaten kullanımda."),
    USER_002("Kullanıcı adı zaten kullanımda."),
    USER_003("Kullanıcı bulunamadı: %s."),
    USER_004("Mevcut şifre yanlış."),

    // --- Rol ve İzin Hataları (RPM_xxx) ---
    RPM_001("Rol adı zaten kullanımda: %s."),
    RPM_002("İzin adı zaten kullanımda: %s."),
    RPM_003("Rol silinemez, aktif olarak kullanıcılara atanmış."),
    RPM_004("İzin silinemez, aktif olarak rollere atanmış."),
    RPM_005("Varsayılan sistem rolü/izni silinemez veya değiştirilemez."),

    // --- Veritabanı Hataları (DB_xxx) ---
    DB_001("Veritabanı işlemi sırasında bir tutarlılık sorunu oluştu. Yinelenen bir kayıt oluşturmaya çalışıyor olabilirsiniz."),
    DB_002("Veritabanı işlemi sırasında beklenmedik bir hata oluştu."),
    DB_003("İlişkili bir kayıt bulunamadığı için işlem gerçekleştirilemedi (foreign key ihlali).");

    private final String defaultMessageTemplate;

    ErrorCode(String defaultMessageTemplate) {
        this.defaultMessageTemplate = defaultMessageTemplate;
    }

    public String formatMessage(Object... args) {
        if (args == null || args.length == 0) {
            return defaultMessageTemplate;
        }
        try {
            return String.format(defaultMessageTemplate, args);
        } catch (Exception e) {
            return defaultMessageTemplate + " [Formatlama argümanları: " + Arrays.toString(args) + "]";
        }
    }

    public String getCode() {
        return this.name();
    }

    public ErrorCategory getCategory() {
        if (name().startsWith("SYS")) return ErrorCategory.SYSTEM;
        if (name().startsWith("RES")) return ErrorCategory.RESOURCE;
        if (name().startsWith("VAL")) return ErrorCategory.VALIDATION;
        if (name().startsWith("AUTHZ")) return ErrorCategory.AUTHORIZATION;
        if (name().startsWith("AUTH")) return ErrorCategory.AUTHENTICATION;
        if (name().startsWith("SUB")) return ErrorCategory.SUBSCRIPTION;
        if (name().startsWith("COMP")) return ErrorCategory.COMPANY;
        if (name().startsWith("USER")) return ErrorCategory.USER;
        if (name().startsWith("RPM")) return ErrorCategory.ROLE_PERMISSION;
        if (name().startsWith("DB")) return ErrorCategory.DATABASE;
        return ErrorCategory.UNKNOWN;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", this.getCode(), this.getDefaultMessageTemplate());
    }

    public enum ErrorCategory {
        SYSTEM,
        RESOURCE,
        VALIDATION,
        AUTHENTICATION,
        AUTHORIZATION,
        SUBSCRIPTION,
        COMPANY,
        USER,
        ROLE_PERMISSION,
        DATABASE,
        UNKNOWN
    }
}
