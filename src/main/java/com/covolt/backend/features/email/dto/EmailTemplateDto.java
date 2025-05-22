package com.covolt.backend.features.email.dto;

import com.covolt.backend.features.email.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for email template information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDto {
    
    private String templateId;
    private EmailType emailType;
    private String name;
    private String description;
    
    // Template content
    private String subject;
    private String htmlContent;
    private String textContent; // Plain text version
    
    // Template path in resources
    private String templatePath;
    
    // Template variables and their descriptions
    private List<TemplateVariable> variables;
    
    // Template metadata
    private String version;
    private boolean isActive;
    private boolean isDefault;
    
    // Localization
    private String language;
    private String locale;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Usage statistics
    private long usageCount;
    private LocalDateTime lastUsedAt;
    
    // Template settings
    private Map<String, Object> settings;
    
    /**
     * Template variable definition
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateVariable {
        private String name;
        private String type; // STRING, NUMBER, DATE, BOOLEAN, OBJECT
        private String description;
        private boolean required;
        private Object defaultValue;
        private String example;
    }
}
