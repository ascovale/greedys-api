package com.application.common.persistence.model.social;

/**
 * ⭐ POST VISIBILITY ENUM
 * 
 * Livello di visibilità di un post.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum PostVisibility {
    
    /**
     * Visibile a tutti
     */
    PUBLIC,
    
    /**
     * Visibile solo ai follower
     */
    FOLLOWERS_ONLY,
    
    /**
     * Visibile solo all'autore (draft/bozza)
     */
    PRIVATE
}
