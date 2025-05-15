package com.covolt.backend.core.security.common;

public enum SecurityAccess {
    PERMIT_ALL,      // Herkes erişebilir
    AUTHENTICATED,   // Sadece kimliği doğrulanmış kullanıcılar
    HAS_ROLE,        // Belirli bir role sahip kullanıcılar
    HAS_AUTHORITY,   // Belirli bir izne sahip kullanıcılar
    DENY_ALL         // Hiç kimse erişemez (varsayılan olarak kullanılabilir)
    // İleride eklenebilir: HAS_ANY_ROLE, HAS_ANY_AUTHORITY, CUSTOM_SPEL
}