package com.application.common.service.group;

import com.application.common.persistence.dao.group.*;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.common.persistence.model.group.*;
import com.application.common.persistence.model.group.enums.BookerType;
import com.application.common.persistence.model.group.enums.DietaryRequirement;
import com.application.common.persistence.model.group.enums.GroupBookingStatus;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Service;
import com.application.common.persistence.model.reservation.SpecialBookingType;
import com.application.common.persistence.model.user.AbstractUser;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service per la gestione delle Prenotazioni di Gruppo.
 */
@Slf4j
@org.springframework.stereotype.Service
@Transactional
@RequiredArgsConstructor
public class GroupBookingService {

    private final GroupBookingDAO groupBookingDAO;
    private final GroupBookingDietaryNeedsDAO dietaryNeedsDAO;
    private final FixedPriceMenuDAO fixedPriceMenuDAO;
    private final RestaurantAgencyDAO restaurantAgencyDAO;
    private final AgencyProposalDAO agencyProposalDAO;
    private final RestaurantDAO restaurantDAO;
    private final ReservationDAO reservationDAO;
    private final ServiceDAO serviceDAO;

    // ==================== CREATE ====================

    /**
     * Crea una nuova prenotazione di gruppo
     */
    public GroupBooking createBooking(Long restaurantId, GroupBooking booking) {
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
        
        booking.setRestaurant(restaurant);
        booking.setStatus(GroupBookingStatus.INQUIRY);
        
        // Calcola totale se menu selezionato
        if (booking.getFixedPriceMenu() != null) {
            calculateAndSetPricing(booking);
        }
        
        GroupBooking saved = groupBookingDAO.save(booking);
        log.info("Created group booking {} for restaurant {}", saved.getConfirmationCode(), restaurantId);
        
        return saved;
    }

    /**
     * Crea prenotazione come Agency
     */
    public GroupBooking createAgencyBooking(Long restaurantId, Long agencyId, GroupBooking booking) {
        // Verifica relazione B2B attiva
        RestaurantAgency relation = restaurantAgencyDAO.findActiveRelationship(restaurantId, agencyId)
            .orElseThrow(() -> new IllegalStateException("No active relationship between restaurant and agency"));
        
        booking.setBookerType(BookerType.AGENCY);
        booking.setAgency(relation);
        
        // Applica sconto agency se presente
        if (booking.getFixedPriceMenu() != null && booking.getFixedPriceMenu().getAgencyPrice() != null) {
            booking.setPricePerPerson(booking.getFixedPriceMenu().getAgencyPrice());
        }
        
        return createBooking(restaurantId, booking);
    }

    /**
     * Crea prenotazione come Customer
     */
    public GroupBooking createCustomerBooking(Long restaurantId, AbstractUser customer, GroupBooking booking) {
        booking.setBookerType(BookerType.CUSTOMER);
        booking.setBooker(customer);
        
        return createBooking(restaurantId, booking);
    }

    // ==================== READ ====================

    public GroupBooking getById(Long bookingId) {
        return groupBookingDAO.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    public Optional<GroupBooking> getByConfirmationCode(String code) {
        return groupBookingDAO.findByConfirmationCode(code);
    }

    public Page<GroupBooking> getByRestaurant(Long restaurantId, Pageable pageable) {
        return groupBookingDAO.findByRestaurantId(restaurantId, pageable);
    }

    public List<GroupBooking> getActiveByRestaurant(Long restaurantId) {
        return groupBookingDAO.findActiveByRestaurant(restaurantId);
    }

    public Page<GroupBooking> getByAgency(Long agencyId, Pageable pageable) {
        return groupBookingDAO.findByAgencyId(agencyId, pageable);
    }

    public List<GroupBooking> getActiveByAgency(Long agencyId) {
        return groupBookingDAO.findActiveByAgency(agencyId);
    }

    public List<GroupBooking> getByDate(Long restaurantId, LocalDate date) {
        return groupBookingDAO.findByRestaurantAndDate(restaurantId, date);
    }

    public List<GroupBooking> getByDateRange(Long restaurantId, LocalDate start, LocalDate end) {
        return groupBookingDAO.findByRestaurantAndDateRange(restaurantId, start, end);
    }

    public List<GroupBooking> getUpcoming(Long restaurantId) {
        return groupBookingDAO.findUpcomingByRestaurant(restaurantId, LocalDate.now());
    }

    public Page<GroupBooking> searchBookings(Long restaurantId, String search, Pageable pageable) {
        return groupBookingDAO.searchByRestaurant(restaurantId, search, pageable);
    }

    // ==================== UPDATE ====================

    public GroupBooking updateBooking(Long bookingId, GroupBooking updates) {
        GroupBooking booking = getById(bookingId);
        
        if (!booking.getStatus().isModifiable()) {
            throw new IllegalStateException("Booking cannot be modified in status: " + booking.getStatus());
        }
        
        if (updates.getEventName() != null) booking.setEventName(updates.getEventName());
        if (updates.getEventDescription() != null) booking.setEventDescription(updates.getEventDescription());
        if (updates.getEventDate() != null) booking.setEventDate(updates.getEventDate());
        if (updates.getEventTime() != null) booking.setEventTime(updates.getEventTime());
        if (updates.getTotalPax() != null) booking.setTotalPax(updates.getTotalPax());
        if (updates.getAdultsCount() != null) booking.setAdultsCount(updates.getAdultsCount());
        if (updates.getChildrenCount() != null) booking.setChildrenCount(updates.getChildrenCount());
        if (updates.getContactName() != null) booking.setContactName(updates.getContactName());
        if (updates.getContactEmail() != null) booking.setContactEmail(updates.getContactEmail());
        if (updates.getContactPhone() != null) booking.setContactPhone(updates.getContactPhone());
        if (updates.getMenuNotes() != null) booking.setMenuNotes(updates.getMenuNotes());
        if (updates.getInternalNotes() != null) booking.setInternalNotes(updates.getInternalNotes());
        
        // Ricalcola totale se cambiato PAX o menu
        if (updates.getTotalPax() != null || updates.getFixedPriceMenu() != null) {
            if (updates.getFixedPriceMenu() != null) {
                booking.setFixedPriceMenu(updates.getFixedPriceMenu());
            }
            calculateAndSetPricing(booking);
        }
        
        log.info("Updated booking {}", bookingId);
        return groupBookingDAO.save(booking);
    }

    // ==================== STATUS MANAGEMENT ====================

    /**
     * Conferma una prenotazione di gruppo.
     * Crea automaticamente una Reservation collegata per l'integrazione nel calendario.
     * 
     * Logica Service:
     * - Prova a trovare un Service che copra l'orario richiesto
     * - Se trovato: lo aggancia alla Reservation
     * - Se NON trovato: crea Reservation senza service, marcata come OFF_HOURS
     */
    public GroupBooking confirm(Long bookingId, Long confirmedBy) {
        GroupBooking booking = getById(bookingId);
        
        if (!booking.canConfirm()) {
            throw new IllegalStateException("Booking cannot be confirmed in status: " + booking.getStatus());
        }
        
        // Prova a trovare un servizio, ma se non c'è va bene lo stesso
        Service service = findServiceForBookingOrNull(booking);
        
        // Crea la Reservation collegata (con o senza service)
        Reservation reservation = createLinkedReservation(booking, service);
        booking.setReservation(reservation);
        
        booking.setStatus(GroupBookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking.setConfirmedBy(confirmedBy);
        
        String serviceInfo = service != null ? service.getName() : "FUORI ORARIO";
        log.info("Confirmed group booking {} with linked reservation {} (service: {})", 
                booking.getConfirmationCode(), reservation.getId(), serviceInfo);
        return groupBookingDAO.save(booking);
    }

    /**
     * Trova il servizio più appropriato per l'orario della prenotazione di gruppo.
     * Ritorna NULL se nessun servizio copre l'orario richiesto.
     * 
     * @return Service se trovato, null se l'orario è fuori dai servizi configurati
     */
    private Service findServiceForBookingOrNull(GroupBooking booking) {
        Collection<Service> services = serviceDAO.findActiveEnabledServices(
            booking.getRestaurant().getId(), 
            booking.getEventDate()
        );
        
        if (services.isEmpty()) {
            log.info("No active services for restaurant {} on date {} - booking will be OFF_HOURS",
                booking.getRestaurant().getId(), booking.getEventDate());
            return null;
        }
        
        // TODO: In futuro, implementare logica per matchare l'orario
        // con gli orari di apertura/chiusura dei servizi.
        // Per ora restituiamo il primo servizio disponibile.
        
        // Se c'è un solo servizio, usalo
        if (services.size() == 1) {
            return services.iterator().next();
        }
        
        // Con più servizi, per ora prendi il primo
        // Miglioramento futuro: scegliere in base a eventTime vs service opening hours
        return services.iterator().next();
    }

    /**
     * Crea una Reservation standard collegata al GroupBooking.
     * Questo permette di vedere la prenotazione di gruppo nel calendario giornaliero.
     * 
     * @param booking Il GroupBooking da collegare
     * @param service Il Service (può essere null per prenotazioni fuori orario)
     */
    private Reservation createLinkedReservation(GroupBooking booking, Service service) {
        LocalDateTime reservationDateTime = LocalDateTime.of(
            booking.getEventDate(), 
            booking.getEventTime()
        );
        
        // Costruisci nome utente dalle info contatto
        String userName = booking.getContactName();
        if (booking.isAgencyBooking() && booking.getAgency() != null && booking.getAgency().getAgency() != null) {
            userName = "[GRUPPO] " + booking.getAgency().getAgency().getName() + " - " + userName;
        } else {
            userName = "[GRUPPO] " + userName;
        }
        
        // Costruisci note con dettagli gruppo
        StringBuilder notes = new StringBuilder();
        notes.append("Prenotazione Gruppo: ").append(booking.getConfirmationCode());
        if (booking.getEventName() != null) {
            notes.append("\nEvento: ").append(booking.getEventName());
        }
        if (booking.getFixedPriceMenu() != null) {
            notes.append("\nMenu: ").append(booking.getFixedPriceMenu().getName());
        }
        if (booking.getMenuNotes() != null) {
            notes.append("\nNote menu: ").append(booking.getMenuNotes());
        }
        if (booking.getTotalAmount() != null) {
            notes.append("\nTotale: €").append(booking.getTotalAmount());
        }
        
        // Determina il tipo speciale e il nome servizio
        SpecialBookingType specialType = SpecialBookingType.GROUP_BOOKING;
        String bookedServiceName;
        
        if (service != null) {
            bookedServiceName = service.getName();
        } else {
            // Fuori orario - marca come OFF_HOURS invece di GROUP_BOOKING
            specialType = SpecialBookingType.OFF_HOURS;
            bookedServiceName = "Prenotazione Gruppo (Fuori Orario)";
        }
        
        Reservation reservation = Reservation.builder()
            .restaurant(booking.getRestaurant())
            .userName(userName)
            .date(booking.getEventDate())
            .service(service) // Può essere null per fuori orario
            .specialBookingType(specialType)
            .reservationDateTime(reservationDateTime)
            .bookedServiceName(bookedServiceName)
            .pax(booking.getTotalPax())
            .kids(booking.getChildrenCount() != null ? booking.getChildrenCount() : 0)
            .notes(notes.toString())
            .status(Reservation.Status.ACCEPTED) // Auto-accettata
            .createdAt(LocalDateTime.now())
            .build();
        
        return reservationDAO.save(reservation);
    }

    public GroupBooking cancel(Long bookingId, String reason, Long cancelledBy) {
        GroupBooking booking = getById(bookingId);
        
        if (!booking.canCancel()) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }
        
        booking.setStatus(GroupBookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(cancelledBy);
        
        log.info("Cancelled booking {} - reason: {}", booking.getConfirmationCode(), reason);
        return groupBookingDAO.save(booking);
    }

    public GroupBooking markDepositPaid(Long bookingId, BigDecimal amount) {
        GroupBooking booking = getById(bookingId);
        
        booking.setDepositPaid(amount);
        booking.setDepositPaidAt(LocalDateTime.now());
        
        if (booking.isDepositComplete()) {
            booking.setStatus(GroupBookingStatus.DEPOSIT_PAID);
        }
        
        log.info("Deposit {} paid for booking {}", amount, booking.getConfirmationCode());
        return groupBookingDAO.save(booking);
    }

    public GroupBooking markFullyPaid(Long bookingId) {
        GroupBooking booking = getById(bookingId);
        
        booking.setIsFullyPaid(true);
        booking.setStatus(GroupBookingStatus.FULLY_PAID);
        
        log.info("Booking {} fully paid", booking.getConfirmationCode());
        return groupBookingDAO.save(booking);
    }

    public GroupBooking complete(Long bookingId) {
        GroupBooking booking = getById(bookingId);
        
        booking.setStatus(GroupBookingStatus.COMPLETED);
        
        // Aggiorna statistiche agency se presente
        if (booking.isAgencyBooking() && booking.getAgency() != null) {
            booking.getAgency().incrementBookings(booking.getTotalAmount());
        }
        
        log.info("Completed booking {}", booking.getConfirmationCode());
        return groupBookingDAO.save(booking);
    }

    public GroupBooking markNoShow(Long bookingId) {
        GroupBooking booking = getById(bookingId);
        
        booking.setStatus(GroupBookingStatus.NO_SHOW);
        
        log.info("No show for booking {}", booking.getConfirmationCode());
        return groupBookingDAO.save(booking);
    }

    // ==================== DIETARY NEEDS ====================

    /**
     * Aggiunge esigenze alimentari alla prenotazione
     */
    public GroupBooking addDietaryNeeds(Long bookingId, Map<DietaryRequirement, Integer> needs) {
        GroupBooking booking = getById(bookingId);
        
        // Rimuovi esistenti
        dietaryNeedsDAO.deleteByGroupBookingId(bookingId);
        
        // Aggiungi nuovi
        for (Map.Entry<DietaryRequirement, Integer> entry : needs.entrySet()) {
            if (entry.getValue() > 0) {
                GroupBookingDietaryNeeds need = GroupBookingDietaryNeeds.builder()
                    .groupBooking(booking)
                    .dietaryRequirement(entry.getKey())
                    .paxCount(entry.getValue())
                    .build();
                dietaryNeedsDAO.save(need);
            }
        }
        
        log.info("Updated dietary needs for booking {}", bookingId);
        return getById(bookingId);
    }

    /**
     * Aggiunge singola esigenza alimentare
     */
    public GroupBookingDietaryNeeds addDietaryNeed(Long bookingId, DietaryRequirement requirement, 
                                                    Integer paxCount, String notes, Boolean isCritical) {
        GroupBooking booking = getById(bookingId);
        
        GroupBookingDietaryNeeds need = GroupBookingDietaryNeeds.builder()
            .groupBooking(booking)
            .dietaryRequirement(requirement)
            .paxCount(paxCount)
            .notes(notes)
            .isCritical(isCritical != null ? isCritical : false)
            .build();
        
        return dietaryNeedsDAO.save(need);
    }

    public List<GroupBookingDietaryNeeds> getDietaryNeeds(Long bookingId) {
        return dietaryNeedsDAO.findByGroupBookingId(bookingId);
    }

    public List<GroupBookingDietaryNeeds> getCriticalDietaryNeeds(Long bookingId) {
        return dietaryNeedsDAO.findCriticalByBooking(bookingId);
    }

    // ==================== MENU SELECTION ====================

    /**
     * Seleziona menu per la prenotazione
     */
    public GroupBooking selectMenu(Long bookingId, Long menuId) {
        GroupBooking booking = getById(bookingId);
        FixedPriceMenu menu = fixedPriceMenuDAO.findById(menuId)
            .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + menuId));
        
        // Verifica PAX
        if (!menu.isPaxAllowed(booking.getTotalPax())) {
            throw new IllegalArgumentException("Menu not available for " + booking.getTotalPax() + " guests");
        }
        
        booking.setFixedPriceMenu(menu);
        calculateAndSetPricing(booking);
        
        return groupBookingDAO.save(booking);
    }

    /**
     * Applica proposta personalizzata
     */
    public GroupBooking applyProposal(Long bookingId, Long proposalId) {
        GroupBooking booking = getById(bookingId);
        AgencyProposal proposal = agencyProposalDAO.findById(proposalId)
            .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        
        if (!proposal.isValid()) {
            throw new IllegalStateException("Proposal is not valid");
        }
        
        booking.setProposal(proposal);
        booking.setPricePerPerson(proposal.getProposedPrice());
        booking.setChildrenPrice(proposal.getChildrenPrice());
        
        if (proposal.getBaseMenu() != null) {
            booking.setFixedPriceMenu(proposal.getBaseMenu());
        }
        
        booking.setTotalAmount(booking.calculateTotal());
        
        return groupBookingDAO.save(booking);
    }

    // ==================== STATISTICS ====================

    public Long countByStatus(Long restaurantId, GroupBookingStatus status) {
        return groupBookingDAO.countByRestaurantAndStatus(restaurantId, status);
    }

    public Integer getTotalPaxForDate(Long restaurantId, LocalDate date) {
        Integer total = groupBookingDAO.sumPaxByRestaurantAndDate(restaurantId, date);
        return total != null ? total : 0;
    }

    // ==================== PRIVATE ====================

    private void calculateAndSetPricing(GroupBooking booking) {
        FixedPriceMenu menu = booking.getFixedPriceMenu();
        if (menu == null) return;
        
        BigDecimal pricePerPerson;
        if (booking.isAgencyBooking() && menu.getAgencyPrice() != null) {
            pricePerPerson = menu.getAgencyPrice();
        } else {
            pricePerPerson = menu.getBasePrice();
        }
        
        booking.setPricePerPerson(pricePerPerson);
        
        // Children price: 50% default
        if (booking.getChildrenPrice() == null && booking.getChildrenCount() != null && booking.getChildrenCount() > 0) {
            booking.setChildrenPrice(pricePerPerson.multiply(BigDecimal.valueOf(0.5)));
        }
        
        booking.setTotalAmount(booking.calculateTotal());
    }
}
