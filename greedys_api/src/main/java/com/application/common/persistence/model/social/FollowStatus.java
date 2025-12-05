package com.application.common.persistence.model.social;

/**
 * ⭐ FOLLOW STATUS ENUM
 * 
 * Stato della relazione di follow.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum FollowStatus {
    
    /**
     * Follow attivo
     */
    ACTIVE,
    
    /**
     * In attesa di approvazione (per profili privati)
     */
    PENDING,
    
    /**
     * Bloccato
     */
    BLOCKED,
    
    /**
     * Rimosso/Unfollowed
     */
    REMOVED
}
