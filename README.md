# Covolt Projesi: Teknik Spesifikasyon ve Yol Haritası
![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/emretemirdev/covolt-panel-be?utm_source=oss&utm_medium=github&utm_campaign=emretemirdev%2Fcovolt-panel-be&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)
## Versiyon 1.0: Gelişmiş Kullanıcı Yönetimi, Firma (Tenant) ve Abonelik Altyapısı

**Proje Vizyonu:** Covolt, enerji sektöründeki firmalar için kapsamlı bir veri toplama, analiz, raporlama ve yapay zeka destekli optimizasyon platformu olacaktır. Sistem, çok kiracılı (multi-tenant) mimaride çalışarak her firmaya izole ve özelleştirilmiş bir deneyim sunacak, farklı abonelik katmanları ile esnek bir gelir modeli sağlayacaktır.

---

## Bölüm 1: Temel Mimari ve Teknolojiler

### 1.1. Ana Teknoloji Yığını

- **Backend:** Spring Boot 3.x (Java 17+), Spring Security 6.x, Spring Data JPA, Spring WebFlux (ileride reaktif gereksinimler için düşünülebilir, başlangıç MVC).
- **Veritabanı:** Azure PostgreSQL (veya eşdeğeri PostgreSQL).
- **Build Aracı:** Maven.
- **Kütüphaneler:** Lombok, MapStruct (DTO-Entity dönüşümleri için), JJWT (JWT işlemleri için).
- **Test:** JUnit 5, Mockito, Testcontainers (entegrasyon testleri için).

### 1.2. Genel Mimari Prensipleri

- **Modülerlik:** Her özellik veya ana işlev kendi modülü içinde (paketler veya ileride mikroservisler) geliştirilecektir.
- **Katmanlı Mimari:** Sunum (Controller), Servis, Veri Erişim (Repository), Alan (Domain/Model) katmanları net bir şekilde ayrılacaktır.
- **API First Yaklaşımı:** Tüm işlevler öncelikle iyi tanımlanmış RESTful API'ler aracılığıyla sunulacaktır. OpenAPI (Swagger) dokümantasyonu standart olacaktır.
- **Güvenlik Odaklı Tasarım (Security by Design):** Güvenlik, tasarımın her aşamasında öncelikli olacaktır.
- **Ölçeklenebilirlik:** Azure üzerindeki kaynakları verimli kullanarak yatay ve dikey ölçeklenebilirlik hedeflenecektir.
- **Asenkron İşlemler (Gelecek Planı):** Yüksek hacimli veri işleme ve rapor oluşturma gibi uzun süren işlemler için asenkron mimariler (örn: Kafka, RabbitMQ ile Mesaj Kuyrukları, Spring @Async) planlanmalıdır.

---

## Bölüm 2: Firma (Tenant) Yönetimi Altyapısı

### 2.1. Firma Konsepti

- Her "Firma" (Company/Organization/Tenant), Covolt platformunda izole bir çalışma alanına sahip olacaktır.
- Bir Firma, birden fazla kullanıcıya, kendi veri kaynaklarına (IoT, SCADA bağlantıları), analizlerine ve abonelik planına sahip olacaktır.

### 2.2. Veri Modeli: `Company` Entity

- **Tablo Adı:** `companies`
- **Alanlar:**
  - `id` (UUID, PK): Firma için benzersiz tanımlayıcı.
  - `name` (String, NotNull, Unique Index): Firma adı.
  - `registration_identifier` (String, Nullable, Unique Index): Ticari sicil no, vergi no gibi resmi tanımlayıcı.
  - `status` (`CompanyStatus` Enum: `ACTIVE`, `SUSPENDED`, `PENDING_VERIFICATION`, `DELETED`): Firmanın platformdaki durumu.
  - `industry_type` (String, Nullable): Firmanın sektörü (Enerji Üretimi, Dağıtım, Tüketici vb.).
  - `address` (Embedded `Address` objesi veya ayrı tablo): Firma adresi.
  - `contact_email` (String, NotNull): Firma iletişim e-postası.
  - `contact_phone` (String, Nullable): Firma iletişim telefonu.
  - `settings` (JSONB veya ayrı `company_settings` tablosu): Firmaya özel ayarlar (tema, dil, bildirim tercihleri vb.).
  - `owner_user_id` (UUID, Nullable, FK -> users.id): Firmayı ilk oluşturan veya birincil yönetici kullanıcının ID'si.
  - `created_at`, `updated_at`, `version` (BaseEntity'den).
- **İlişkiler:**
  - `users` (OneToMany to `User`): Bu firmaya ait kullanıcılar.
  - `companySubscriptions` (OneToMany to `CompanySubscription`): Bu firmanın sahip olduğu (veya geçmişteki) abonelik kayıtları.
  - (Gelecekte) `dataSources` (OneToMany to `DataSource`), `reports` (OneToMany to `Report`), `devices` (OneToMany to `Device`) vb. firmanın sahip olduğu diğer varlıklar.

### 2.3. Firma Durumları: `CompanyStatus` Enum

- Değerler: `ACTIVE`, `SUSPENDED` (örn: ödeme sorunu), `PENDING_VERIFICATION` (yeni kayıt, admin onayı bekliyor), `DELETED` (kalıcı silinmiş).

### 2.4. Firma Yönetimi API'leri (Platform Adminleri İçin)

- `POST /api/admin/companies`: Yeni firma oluşturma (admin tarafından).
- `GET /api/admin/companies`: Tüm firmaları listeleme (filtreleme, sayfalama ile).
- `GET /api/admin/companies/{companyId}`: Belirli bir firmayı getirme.
- `PUT /api/admin/companies/{companyId}`: Firma bilgilerini güncelleme.
- `PATCH /api/admin/companies/{companyId}/status`: Firma durumunu değiştirme (ACTIVE, SUSPENDED vb.).

---

## Bölüm 3: Gelişmiş Kullanıcı Yönetimi

### 3.1. Veri Modeli: `User` Entity (Güncellenmiş)

- **Tablo Adı:** `users`
- **Alanlar:**
  - `id` (UUID, PK).
  - `email` (String, NotNull, Unique Index): Birincil giriş ve iletişim.
  - `username` (String, NotNull, Unique Index): Kullanıcı adı.
  - `password` (String, NotNull): BCrypt ile hashlenmiş şifre.
  - `first_name` (String, Nullable).
  - `last_name` (String, Nullable).
  - `phone_number` (String, Nullable, Unique Index opsiyonel).
  - `profile_picture_url` (String, Nullable).
  - `status` (`UserStatus` Enum: `ACTIVE`, `PENDING_EMAIL_VERIFICATION`, `DISABLED_BY_ADMIN`, `LOCKED_FAILED_LOGIN`): Kullanıcının platformdaki durumu.
  - `last_login_at` (Instant, Nullable): Son başarılı giriş zamanı.
  - `failed_login_attempts` (int, default 0): Başarısız giriş deneme sayısı.
  - `lockout_end_time` (Instant, Nullable): Hesap kilidinin ne zaman açılacağı.
  - `two_factor_enabled` (boolean, default false): İki faktörlü kimlik doğrulama aktif mi?
  - `two_factor_secret` (String, Nullable): 2FA için secret (şifrelenmiş).
  - `preferred_language` (String, default "tr-TR").
  - `timezone` (String, default "Europe/Istanbul").
  - `email_verified_at` (Instant, Nullable): E-posta doğrulama zamanı.
  - `password_changed_at` (Instant, Nullable): Son şifre değişim zamanı.
  - `created_at`, `updated_at`, `version` (BaseEntity'den).
- **İlişkiler:**
  - `company` (ManyToOne to `Company`, NotNull, FK `company_id`): Kullanıcının ait olduğu firma.
  - `userRoles` (ManyToMany to `Role` via `user_roles` join table): Kullanıcının global veya firma bazlı rolleri.

### 3.2. Kullanıcı Durumları: `UserStatus` Enum

- Değerler: `ACTIVE`, `PENDING_EMAIL_VERIFICATION`, `DISABLED_BY_ADMIN`, `INVITED` (eğer davet sistemi olacaksa), `LOCKED_FAILED_LOGIN`.

### 3.3. Rol ve İzin Yönetimi (`Role`, `Permission` Entity'leri)

- **`Role` Entity:**
  - `id` (UUID, PK).
  - `name` (String, NotNull, Unique Index - örn: `PLATFORM_ADMIN`, `COMPANY_ADMIN_for_{companyId}`, `USER_for_{companyId}`). **Rol isimlerinin firma bazında (tenant-aware) veya global olacağı stratejisi belirlenmeli.** Eğer firma bazlı ise `company_id` (FK, Nullable) alanı eklenmeli.
  - `description` (String).
  - `permissions` (ManyToMany to `Permission` via `role_permissions` join table): Bu role ait izinler.
- **`Permission` Entity:**
  - `id` (UUID, PK).
  - `name` (String, NotNull, Unique Index - örn: `USER_READ`, `USER_CREATE`, `REPORT_GENERATE`, `DEVICE_CONTROL`).
  - `description` (String).
  - `resource_group` (String, Nullable - örn: "USER_MANAGEMENT", "DEVICE_OPERATIONS"): İzinleri gruplamak için.

### 3.4. `CustomUserDetails` (Geliştirilmiş)

- Spring Security `UserDetails` arayüzünü implement eden özel sınıf.
- Temel kullanıcı bilgilerine ek olarak:
  - `companyId` ve `companyName`.
  - Aktif `CompanySubscription` bilgileri (plan adı, abonelik durumu - `UserSubscriptionStatus`).
  - Gerekirse kullanıcının **hesaplanan efektif izinlerini** (hem rollerden hem de belki doğrudan atananlardan - şimdilik doğrudan yoktu).
- Bu bilgiler, `@PreAuthorize` ile detaylı yetkilendirme yapmak için kullanılacak.

---

## Bölüm 4: Abonelik ve Faturalandırma Sistemi

### 4.1. Veri Modeli: `SubscriptionPlan` Entity (Güncellenmiş)

- **Tablo Adı:** `subscription_plans`
- **Alanlar:**
  - `id` (UUID, PK).
  - `name` (String, NotNull, Unique Index - örn: "FREE_TIER", "BASIC_MONTHLY", "PREMIUM_ANNUAL").
  - `display_name` (String, NotNull): Kullanıcıya gösterilecek plan adı.
  - `description` (String).
  - `price` (BigDecimal, NotNull): Planın birim fiyatı.
  - `billing_interval` (`BillingInterval` Enum: `MONTHLY`, `ANNUALLY`, `ONE_TIME`).
  - `trial_days` (Integer, Nullable): Bu plana ilk geçişte verilecek ücretsiz deneme gün sayısı (sadece belirli planlar için anlamlı).
  - `features` (JSONB veya `plan_features` join table to `Feature` entity): Planın sunduğu özellikler/limitler (örn: "Max Kullanıcı: 10", "Veri Saklama: 30 gün", "API Erişimi: Var").
  - `is_public` (boolean, default true): Plan kullanıcılar tarafından seçilebilir mi? (Bazı planlar özel olabilir).
  - `stripe_price_id` (String, Nullable): Ödeme sistemi (Stripe) entegrasyonu için planın oradaki ID'si.
  - `status` (`PlanStatus` Enum: `ACTIVE`, `ARCHIVED`).
  - `created_at`, `updated_at`, `version`.

### 4.2. Faturalandırma Aralığı: `BillingInterval` Enum

- Değerler: `MONTHLY`, `ANNUALLY`, `ONE_TIME`, `CUSTOM`.

### 4.3. Plan Durumu: `PlanStatus` Enum

- Değerler: `ACTIVE` (Seçilebilir), `ARCHIVED` (Artık yeni abonelik için seçilemez ama mevcut aboneler devam edebilir).

### 4.4. Veri Modeli: `CompanySubscription` Entity (Güncellenmiş)

- **Tablo Adı:** `company_subscriptions`
- **Alanlar:**
  - `id` (UUID, PK).
  - `company` (ManyToOne to `Company`, NotNull, FK `company_id`). **`company_id` ve `status=ACTIVE/TRIAL` üzerinde UNIQUE kısıtlama olmalı.** (Bir firma sadece bir aktif/deneme aboneliğe sahip olabilir).
  - `plan` (ManyToOne to `SubscriptionPlan`, NotNull, FK `plan_id`).
  - `status` (`UserSubscriptionStatus` Enum, NotNull).
  - `current_period_start` (Instant, NotNull): Mevcut faturalama döneminin başlangıcı.
  - `current_period_end` (Instant, Nullable): Mevcut faturalandırma döneminin bitişi (deneme için `trial_end_date` kullanılabilir, süresiz planlar için null olabilir).
  - `trial_ends_at` (Instant, Nullable): Sadece `TRIAL` durumundaki abonelikler için.
  - `cancelled_at` (Instant, Nullable): İptal edilme tarihi.
  - `ends_at` (Instant, Nullable): Aboneliğin tamamen sona ereceği tarih (iptal sonrası dönem sonu).
  - `auto_renew` (boolean, default true): Otomatik yenileme açık mı?
  - `stripe_subscription_id` (String, Nullable): Ödeme sistemi (Stripe) entegrasyonu için aboneliğin oradaki ID'si.
  - `created_at`, `updated_at`, `version`.
- **Not:** `UserSubscriptionStatus` Enum adı daha genele hitap edecek `SubscriptionStatus` olarak değiştirilebilir.

### 4.5. `CompanySubscriptionService` (Geliştirilmiş)

- `startTrial(Company company, SubscriptionPlan trialPlan)`: Deneme planı ile abonelik başlatır.
- `subscribeToPlan(Company company, SubscriptionPlan plan, String paymentMethodToken)`: Firmayı seçilen plana abone yapar (Ödeme entegrasyonu gerekir).
- `changePlan(CompanySubscription currentSubscription, SubscriptionPlan newPlan)`: Plan değişikliği (upgrade/downgrade). Faturalandırma (proration vb.) düşünülmeli.
- `cancelSubscription(CompanySubscription subscription, boolean atPeriodEnd)`: Aboneliği iptal eder.
- `reactivateSubscription(CompanySubscription subscription)`: İptal edilmiş/süresi dolmuş aboneliği yeniden aktif eder.
- `getCurrentActiveSubscription(Company company)`: Firmanın geçerli (ACTIVE veya TRIAL ve süresi dolmamış) aboneliğini getirir.
- **Scheduled Tasks:**
  - Süresi dolan denemeleri `EXPIRED` veya otomatik olarak ücretsiz bir plana (`FREE_TIER`) geçiren görev.
  - Yenileme tarihi gelen abonelikler için ödeme almaya çalışan ve duruma göre (`PAST_DUE`, `EXPIRED`) güncelleyen görev.

---

## Bölüm 5: Kimlik Doğrulama (Authentication) ve Oturum Yönetimi (Detaylar)

### 5.1. Kayıt Akışı (`AuthServiceImpl.register`)

- **Input:** `RegisterRequest` (email, username, password, **companyName**, firstName, lastName).
- **İş Mantığı:**
  1.  Email ve username benzersizliği kontrolü.
  2.  `companyName` ile Firma kontrolü:
      - Eğer "aynı isimle firma olamaz" kuralı varsa: Hata dön.
      - Eğer firma yoksa: Yeni `Company` oluştur (`status = PENDING_VERIFICATION` veya `ACTIVE`), `owner_user_id`'yi yeni oluşacak kullanıcıya ata. Kaydet.
      - Eğer firma varsa ve "mevcut firmaya katılma" senaryosu yoksa (başlangıç için yok): Bu kısmı netleştir. Genellikle ilk kayıt yeni firma oluşturur.
  3.  Yeni `User` oluştur, bilgilerini ve **`company` ilişkisini** set et. `status = PENDING_EMAIL_VERIFICATION` yap. Kaydet.
  4.  **E-posta Doğrulama:** Kullanıcıya doğrulama linki içeren bir e-posta gönder (Asenkron işlem - örn: RabbitMQ veya @Async).
  5.  Oluşturulan Firma için `CompanySubscriptionService.startTrial(company, defaultTrialPlan)` çağır (14 günlük deneme için).
  6.  Kullanıcıya `ROLE_USER` ata (belki de firmadaki varsayılan rol).
  7.  (Kullanıcı henüz `PENDING_EMAIL_VERIFICATION` olduğu için) JWT token DÖNÜLMEMELİ. Kullanıcıya "E-postanızı doğrulayın" mesajı dönülmeli. VEYA token dönülür ama e-posta doğrulanana kadar korunan endpointlere erişimi engellenir (UserDetails'deki `enabled` flag'i `false` olur).

### 5.2. E-posta Doğrulama Akışı

- `POST /api/auth/verify-email?token={verificationToken}`: Kullanıcı linke tıkladığında.
- Token validasyonu -> `User.status`'u `ACTIVE` yap, `email_verified_at`'ı set et. Başarılı yanıt dön (belki otomatik login ve JWT tokenları).

### 5.3. Login Akışı (`AuthServiceImpl.login`)

- **Input:** `LoginRequest` (email, password).
- **İş Mantığı:**
  1.  Kullanıcı kimlik doğrulama (AuthenticationManager).
  2.  Başarılıysa:
      - Kullanıcıyı çek. `User.status` kontrolü (ACTIVE mi? PENDING_EMAIL_VERIFICATION ise uyarı ver/engelle. LOCKED ise engelle).
      - Firmanın `CompanySubscriptionService.getCurrentActiveSubscription(user.getCompany())` ile aktif aboneliğini çek.
      - Eğer abonelik yok veya süresi dolmuş/pasif ise `SubscriptionInactiveException` fırlat (GlobalExceptionHandler yakalar, 401/403 döner).
      - JWT Access ve Refresh Token üret.
      - `last_login_at` güncelle. `failed_login_attempts` sıfırla.
      - `AuthResponse` dön (içinde tokenlara ek olarak belki temel firma ve abonelik bilgisi).
  3.  Başarısızsa:
      - `failed_login_attempts` artır. Belirli bir eşikten sonra `User.status`'u `LOCKED_FAILED_LOGIN` yap ve `lockout_end_time` set et.
      - `BadCredentialsException` fırlat.

### 5.4. Şifre Yönetimi

- **Şifre Sıfırlama İsteği:** `POST /api/auth/forgot-password` (email alır, reset token üretir, e-posta ile gönderir).
- **Şifre Sıfırlama:** `POST /api/auth/reset-password` (resetToken, newPassword alır, tokenı doğrular, şifreyi günceller).
- **Şifre Değiştirme (Login Olmuşken):** `POST /api/users/me/change-password` (oldPassword, newPassword alır).

### 5.5. Token Yönetimi

- **Access Token:** Kısa ömürlü (örn: 15-60 dk). Rol/izin ve temel kullanıcı ID'si içerir. Hassas veri içermez.
- **Refresh Token:** Uzun ömürlü (örn: 7-30 gün). Opak, rastgele string. DB'de saklanır ve User ile ilişkilidir. Tek kullanımlık (Single-Use). Kaydırılabilir pencere (Sliding window) ile ömrü uzatılabilir (başarılı refresh sonrası yeni refresh token'ın da ömrü full ayarlanır).
- **Logout (`/api/auth/logout`):** İlgili Refresh Token'ı DB'den siler.

---

## Bölüm 6: Yetkilendirme (Authorization)

### 6.1. Mekanizma

- Spring Security `@PreAuthorize`, `@PostAuthorize`, `@Secured` anotasyonları kullanılacak (`@EnableMethodSecurity` aktif edilecek).
- SpEL (Spring Expression Language) ile `CovoltUserDetails` principal objesindeki alanlara (roller, izinler, firma bilgisi, abonelik planı/durumu) göre dinamik yetkilendirme kuralları yazılacak.

### 6.2. Örnek Kurallar

- **Platform Admini Yetkileri:** `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` (Firma oluşturma, genel ayarlar vb.).
- **Firma Admini Yetkileri:** `@PreAuthorize("hasRole('COMPANY_ADMIN') and principal.companyId == #companyIdFromPathVariableOrRequestBody")` (Kendi firmasının kullanıcılarını, ayarlarını yönetme).
- **Abonelik Seviyesine Göre Özellik Erişimi:** `@PreAuthorize("isAuthenticated() and principal.activeSubscription.plan.name == 'PREMIUM_PLAN' and principal.activeSubscription.status == 'ACTIVE'")` (Sadece aktif Premium abonelerin erişebileceği bir özellik).
- **Veri Erişimi (Multi-Tenant):** Servis katmanında veya Repository'de, sorgular her zaman o anki kullanıcının `companyId`'sine göre filtrelenmelidir. Bu, Spring Data JPA Specifications, Querydsl veya Hibernate Filters ile otomatikleştirilebilir veya servislerde manuel olarak `SecurityContextHolder`'dan `CovoltUserDetails` alınıp `companyId` kullanılır.

---

## Bölüm 7: Diğer Önemli Hususlar

### 7.1. API Versiyonlama

- Başlangıç için `/api/v1/...` gibi URL bazlı versiyonlama düşünülebilir.

### 7.2. Veri Validasyonu

- DTO'larda Jakarta Bean Validation standart olarak kullanılacak.
- Servis katmanında ek iş kuralları validasyonları.

### 7.3. Loglama

- SLF4J ve Logback (Spring Boot default) kullanılacak.
- İstek/yanıt loglaması (opsiyonel, hassas veri maskelenerek).
- Hata loglaması (GlobalExceptionHandler ve kritik noktalarda).
- Audit Logları: Kim, ne zaman, ne yaptı gibi kritik işlemler için ayrı bir audit log mekanizması düşünülebilir (`@EntityListeners` veya AOP ile).

### 7.4. E-posta Servisi

- Kullanıcı kaydı, şifre sıfırlama, bildirimler için asenkron bir e-posta gönderme servisi (örn: Spring Mail + Amazon SES / SendGrid veya local SMTP for dev).

### 7.5. Asenkron İşlemler

- Uzun süren işlemler (rapor oluşturma, toplu veri işleme) için `@Async` veya mesaj kuyrukları (RabbitMQ/Kafka) kullanılmalı.

### 7.6. Test Stratejisi

- Birim Testleri (JUnit, Mockito): Servis ve yardımcı sınıflar için.
- Entegrasyon Testleri (Testcontainers, @SpringBootTest): Veritabanı ve diğer bileşenlerle entegrasyonu test etmek için. API endpoint testleri de burada yapılabilir.

### 7.7. Dokümantasyon

- OpenAPI (Swagger) ile API dokümantasyonu otomatik olarak generate edilecek (`springdoc-openapi`).

---

## İlk Adımlar (Öncelikli İnşa Planı):

1.  **Temel Entity'leri Kur:** `Company`, `SubscriptionPlan`, `CompanySubscription`, `UserSubscriptionStatus`, ve `User` (güncellenmiş) entity'lerini ve repolarını oluştur.
2.  **`InitialDataLoader` Güncellemesi:** Varsayılan Rolleri ve Abonelik Planlarını (örn: FREE_TRIAL_PLAN, BASIC_MONTHLY) ve bir Platform Admin kullanıcısını (ve onun firmasını/aboneliğini) oluştur.
3.  **`CompanySubscriptionService` Temel Metotları:** `startTrial`, `getCurrentActiveSubscription` implement et.
4.  **`AuthServiceImpl.register` Yeniden Yazımı:** Firma oluşturma, kullanıcıyı firmaya bağlama, deneme aboneliği başlatma (E-posta onayı şimdilik atlanabilir, `User.status=ACTIVE` ile başlar). Token dönmeli (yeni kullanıcı login olabilmeli).
5.  **`CovoltUserDetails` ve `CustomUserDetailsService` Güncellemesi:** Abonelik ve firma bilgilerini UserDetails'e ekle.
6.  **`AuthServiceImpl.login` Yeniden Yazımı:** Firma ve abonelik durumunu kontrol et, `SubscriptionInactiveException` fırlat. Token dön.
7.  **GlobalExceptionHandler Güncellemesi:** Yeni exception'ları yakala.
8.  **Temel API Testleri (Postman):** Register (firma ile), Login (abonelik kontrolü), Korunan bir endpoint'e erişim (sadece `isAuthenticated()`).
9.  **İlk Yetkilendirme Kuralı:** Basit bir `@PreAuthorize("isAuthenticated() and principal.activeSubscription?.plan?.name == 'PREMIUM_PLAN'")` gibi bir kuralı bir test endpoint'ine ekleyip, farklı aboneliklerle (data.sql veya InitialDataLoader ile set edilmiş) erişimi test et.

Bu yol haritası, Covolt projesini sağlam temeller üzerine, modüler, güvenli ve gelecekteki genişlemelere açık bir şekilde inşa etmek için kapsamlı bir başlangıç noktası sunar. Bu sadece bir teknik spesifikasyon taslağıdır; her adımda daha da detaylanabilir ve gerçek implementasyon sırasında yeni gereksinimler ortaya çıkabilir.

Kralım, şimdi bu daha detaylı ve profesyonel spesifikasyon üzerinden ilerleyebiliriz. Her bir maddeyi ve adımı konuşarak, kodlayarak hayata geçirebiliriz. Bu, gerçekten "dünya çapında bir yazılım" için doğru bir temel oluşturacaktır. Ne düşünüyorsun? Hangi bölümden veya ilk adımdan başlamak istersin?
