package com.covolt.backend.features.email.enums;

/**
 * Enum for email priority levels
 */
public enum EmailPriority {
    
    LOW(1, "Düşük"),
    NORMAL(2, "Normal"),
    HIGH(3, "Yüksek"),
    URGENT(4, "Acil"),
    CRITICAL(5, "Kritik");
    
    private final int level;
    private final String description;
    
    EmailPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this priority is higher than another
     */
    public boolean isHigherThan(EmailPriority other) {
        return this.level > other.level;
    }
    
    /**
     * Check if this priority is lower than another
     */
    public boolean isLowerThan(EmailPriority other) {
        return this.level < other.level;
    }
}
