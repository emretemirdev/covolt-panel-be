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
    
    /**
     * Constructs an EmailPriority enum constant with the specified priority level and Turkish description.
     *
     * @param level the integer value representing the priority level
     * @param description the Turkish description of the priority
     */
    EmailPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }
    
    /**
     * Returns the integer value representing the priority level of this email priority.
     *
     * @return the priority level as an integer
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Returns the Turkish description of the email priority level.
     *
     * @return the description of the priority in Turkish
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines whether this email priority is higher than the specified priority.
     *
     * @param other the email priority to compare against
     * @return true if this priority's level is greater than the other; false otherwise
     */
    public boolean isHigherThan(EmailPriority other) {
        return this.level > other.level;
    }
    
    /**
     * Determines whether this email priority is lower than the specified priority.
     *
     * @param other the priority to compare against
     * @return true if this priority's level is less than the other; false otherwise
     */
    public boolean isLowerThan(EmailPriority other) {
        return this.level < other.level;
    }
}
