-- src/main/resources/data.sql

-- Bu script, User ve Role tablolarınıza başlangıç verisi ekler.
-- Tablo ve kolon isimleri ile PK (Primary Key) tipinizin (Long vs UUID) doğruluğundan emin olun.
-- Şifreler mutlaka hashlenmiş OLMALIDIR! Güvenlik için BURAYA AÇIK ŞİFRE YAZMAYIN!

-- NOT: Varsayılan 'ROLE_USER' ve 'ROLE_ADMIN' InitialDataLoader tarafından eklenmiş olmalı.

-- Hashlenmiş şifre: 'password' şifresinin BCrypt hash'i. Kendiniz generate edip kullanabilirsiniz.
-- Örneğin: Java kodunda `new BCryptPasswordEncoder().encode("testpassword")` ile generate edilebilir.
-- Burada "password" için örnek BCrypt hash'i kullanıyorum:
-- $2a$10$Cm6q3Gv1O0w3zL1M2xKq3O.K5R0n0UvY4pYpZpQ0R8Qz0R0Xy5Kq - (bu farklı bir hash'e dönebilir siz generate ettiğinizde)
-- SİZİN PasswordEncoder'ınız ile "password" için üreteceğiniz hash'i KULLANIN!

-- **Örnek User Kaydı 1 (Normal Kullanıcı):**
-- ID alanının nasıl yönetildiğini (serial/sequence vs UUID) kontrol edin ve ona göre ID'yi verin/boş bırakın.
-- Varsayım: PK Long ve Sequence/IDENTITY tarafından yönetiliyor
-- ID kendiliğinden oluşuyorsa sadece diğer kolonları belirtin ve ID kolonunu atlayın:
INSERT INTO users (email, username, password, full_name, phone_number, enabled, locked, failed_login_attempts, created_at, updated_at)
VALUES (
    'user@example.com',
    'standarduser',
    '$2a$10$10r78H6.Yl1X3kP9pB9xKu1uT7nQ/sZ/tV.j.d8v5V.zY4g0b.xY.', -- 'password' kelimesinin BCrypt hash'i (SİZİN ÜRETTİĞİNİZ HASH İLE DEĞİŞTİRİN!)
    'Standard Kullanici',
    '5551112233',
    true, -- enabled
    false, -- locked
    0, -- failed_login_attempts
    NOW(), -- created_at (PostgreSQL NOW() fonksiyonu)
    NOW()  -- updated_at (PostgreSQL NOW() fonksiyonu)
);

-- **Örnek User Kaydı 2 (Admin Kullanıcı):**
INSERT INTO users (email, username, password, full_name, phone_number, enabled, locked, failed_login_attempts, created_at, updated_at)
VALUES (
    'admin@example.com',
    'adminuser',
    '$2a$10$10r78H6.Yl1X3kP9pB9xKu1uT7nQ/sZ/tV.j.d8v5V.zY4g0b.xY.', -- 'password' kelimesinin BCrypt hash'i (SİZİN ÜRETTİĞİNİZ HASH İLE DEĞİŞTİRİN!)
    'Admin Kullanici',
    '5554445566',
    true,
    false,
    0,
    NOW(),
    NOW()
);

-- Şimdi Roll-User ilişkisini kur.
-- 'user_roles' ara tablosu ve kolon isimleri (user_id, role_id) şemanıza uygun olmalı.
-- user_id'ler için SELECT ile ilgili kullanıcının ID'sini bulacağız.
-- role_id'ler için SELECT ile ilgili rolün ID'sini bulacağız (InitialDataLoader eklemiş olmalı).

-- 'standarduser' (user@example.com) kullanıcısına 'ROLE_USER' rolü atama:
INSERT INTO user_roles (user_id, role_id)
VALUES (
    (SELECT id FROM users WHERE email = 'user@example.com'),
    (SELECT id FROM roles WHERE name = 'ROLE_USER')
);

-- 'adminuser' (admin@example.com) kullanıcısına 'ROLE_ADMIN' ve 'ROLE_USER' rolleri atama:
INSERT INTO user_roles (user_id, role_id)
VALUES (
    (SELECT id FROM users WHERE email = 'admin@example.com'),
    (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')
);

INSERT INTO user_roles (user_id, role_id)
VALUES (
    (SELECT id FROM users WHERE email = 'admin@example.com'),
    (SELECT id FROM roles WHERE name = 'ROLE_USER')
);

-- Permissions ilişkisi (eğer kullanıcılara doğrudan permission atayacaksanız)
-- Bu örneği Permissions Entity/Tablosu olduğunu varsayarak ekliyorum.
-- user_permissions ara tablosu ve kolon isimleri (user_id, permission_id) şemanıza uygun olmalı.
-- Varsayım: READ_PRIVILEGE gibi permission isimleri veritabanınızda var.
-- Örn: 'adminuser' kullanıcısına 'READ_PRIVILEGE' iznini doğrudan ata
/*
INSERT INTO user_permissions (user_id, permission_id)
VALUES (
    (SELECT id FROM users WHERE email = 'admin@example.com'),
    (SELECT id FROM permissions WHERE name = 'READ_PRIVILEGE') -- permission tablonuz ve alan adınız neyse
);
*/