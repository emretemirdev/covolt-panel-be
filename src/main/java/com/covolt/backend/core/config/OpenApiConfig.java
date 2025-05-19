// com.covolt.backend.core.config.OpenApiConfig.java (veya core.security altına)

package com.covolt.backend.core.config; // Paket yolunu uygun şekilde ayarlayın

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // JWT Güvenlik Şeması Adı
    private static final String SECURITY_SCHEME_NAME = "bearerAuth"; // Swagger UI'da görünecek isim


    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Covolt Panel Backend API Dokümantasyonu")
                        .version("1.0")
                        .description("Covolt Panel Backend Uygulaması API'leri. Authentication, Kullanıcı, Şirket, Rol ve İzin Yönetimi endpointlerini içerir."))
                // JWT Yetkilendirme şemasını tanımlama
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer") // Protokol şeması (HTTP Authorization header için)
                                        .bearerFormat("JWT") // Taşıyıcı formatı
                                        .description("JWT Kimlik Doğrulama için 'Bearer {token}' formatında token girin.") // Swagger UI'da görünecek açıklama
                        ))
                // API'lere global olarak bu güvenlik şemasının uygulanacağını belirtme
                // (Belirli endpointleri `@Operation(security = {})` ile override edebilirsiniz)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
        // Eğer bazı endpointler anonim ise ve global securityrequirement istemiyorsanız, bu satırı kaldırabilirsiniz
        // ve her güvenli endpoint'e `@Operation(security = @SecurityRequirement(name = "bearerAuth"))` ekleyebilirsiniz.
        // Genellikle admin/Authenticated endpoint'ler JWT istediği için global tanım uygun olur.
    }

    // Diğer özelleştirmeler buraya eklenebilir (örneğin: groupNames, pathsToMatch vb.)
    // Daha fazla bilgi için springdoc-openapi dokümantasyonuna bakabilirsiniz.
}