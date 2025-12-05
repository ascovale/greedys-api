package com.application.common.persistence.model.social;

/**
 * ⭐ REACTION TYPE ENUM
 * 
 * Tipo di reazione su un post/commento.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum ReactionType {
    
    LIKE("👍"),
    LOVE("❤️"),
    LAUGH("😂"),
    WOW("😮"),
    YUM("😋"),
    CLAP("👏"),
    FIRE("🔥"),
    SAD("😢");
    
    private final String emoji;
    
    ReactionType(String emoji) {
        this.emoji = emoji;
    }
    
    public String getEmoji() {
        return emoji;
    }
}
