package com.application.common.persistence.model.notification;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@MappedSuperclass
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public abstract class ANotification {
    
    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "body", nullable = false)
    String body;

    @ElementCollection
    @CollectionTable(name = "notification_properties", joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value")
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();

    @Column(name = "is_read")
    @Builder.Default
    private Boolean read = false;

    /**
     * ⭐ SHARED READ PATTERN (First-Act Logic)
     * 
     * Se true: quando UN utente legge, TUTTI gli altri con stesso eventId
     *          vengono marcati come letti automaticamente.
     * 
     * Use Cases:
     * - Chat messages: Manager risponde → Altri staff vedono "GESTITO"
     * - System alerts: Admin risolve → Altri admin vedono "RISOLTO"
     * - Reservations: INDIVIDUAL (ogni staff deve leggere)
     * 
     * Default: false (individual read)
     */
    @Column(name = "shared_read")
    @Builder.Default
    private Boolean sharedRead = false;

    /**
     * User ID che ha letto per primo (se sharedRead=true)
     * Usato per mostrare "Letto da [Nome]" agli altri utenti
     */
    @Column(name = "read_by_user_id")
    private Long readByUserId;

    /**
     * Timestamp quando la notifica è stata letta
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Timestamp di creazione della notifica (gestito automaticamente da Hibernate)
     */
    @Column(name = "creation_time", updatable = false)
    @CreationTimestamp
    private Instant creationTime;

}
