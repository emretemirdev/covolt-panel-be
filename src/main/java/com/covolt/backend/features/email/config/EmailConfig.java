package com.covolt.backend.features.email.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for email feature
 */
@Configuration
@ConfigurationProperties(prefix = "app.email")
@Data
public class EmailConfig {
    
    // Basic email settings
    private boolean enabled = true;
    private String defaultFromEmail = "noreply@covolt.com";
    private String defaultFromName = "Covolt";
    
    // Rate limiting
    private RateLimiting rateLimiting = new RateLimiting();
    
    // Retry settings
    private Retry retry = new Retry();
    
    // Template settings
    private Template template = new Template();
    
    // Tracking settings
    private Tracking tracking = new Tracking();
    
    // Queue settings
    private Queue queue = new Queue();
    
    // Provider settings
    private Provider provider = new Provider();
    
    @Data
    public static class RateLimiting {
        private boolean enabled = true;
        private int maxEmailsPerMinute = 60;
        private int maxEmailsPerHour = 1000;
        private int maxEmailsPerDay = 10000;
        private int bulkEmailBatchSize = 50;
        private long bulkEmailBatchDelayMs = 1000;
    }
    
    @Data
    public static class Retry {
        private boolean enabled = true;
        private int maxRetries = 3;
        private long initialDelayMs = 1000;
        private double backoffMultiplier = 2.0;
        private long maxDelayMs = 300000; // 5 minutes
    }
    
    @Data
    public static class Template {
        private String basePath = "templates/email";
        private String defaultLanguage = "tr";
        private boolean cacheEnabled = true;
        private long cacheExpirationMinutes = 60;
    }
    
    @Data
    public static class Tracking {
        private boolean enabled = true;
        private boolean trackOpens = true;
        private boolean trackClicks = true;
        private String trackingDomain = "track.covolt.com";
        private long trackingDataRetentionDays = 90;
    }
    
    @Data
    public static class Queue {
        private boolean enabled = true;
        private String queueName = "email-queue";
        private int maxQueueSize = 10000;
        private int workerThreads = 5;
        private long processingTimeoutMs = 30000;
    }
    
    @Data
    public static class Provider {
        private String primary = "smtp";
        private String fallback = "smtp";
        private Map<String, Object> smtpSettings;
        private Map<String, Object> sendgridSettings;
        private Map<String, Object> awsSesSettings;
    }
}
