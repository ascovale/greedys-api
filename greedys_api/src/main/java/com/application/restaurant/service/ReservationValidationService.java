package com.application.restaurant.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.ServiceVersion;
import com.application.restaurant.persistence.dao.ServiceVersionDAO;
import com.application.restaurant.web.dto.schedule.TimeSlotDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationValidationService {

    private final ServiceVersionScheduleService scheduleService;
    private final ServiceVersionDAO serviceVersionDAO;

    public ValidationResult validateReservationDateTime(
            Long serviceVersionId,
            LocalDate reservationDate,
            LocalTime reservationTime,
            Integer partySize) {

        try {
            log.info("Validating reservation - serviceVersion={}, date={}, time={}, partySize={}",
                    serviceVersionId, reservationDate, reservationTime, partySize);

            if (partySize == null || partySize <= 0) {
                return ValidationResult.invalid("Party size must be greater than 0");
            }

            if (serviceVersionId == null) {
                return ValidationResult.invalid("Service version ID is required");
            }

            if (reservationDate == null || reservationTime == null) {
                return ValidationResult.invalid("Reservation date and time are required");
            }

            Optional<ServiceVersion> serviceVersionOpt = serviceVersionDAO.findById(serviceVersionId);
            if (!serviceVersionOpt.isPresent()) {
                return ValidationResult.invalid("Service version not found");
            }

            ServiceVersion serviceVersion = serviceVersionOpt.get();

            if (!serviceVersion.getState().toString().equals("ACTIVE")) {
                return ValidationResult.invalid("Service version is not active for reservations");
            }

            if (reservationDate.isBefore(LocalDate.now())) {
                return ValidationResult.invalid("Cannot reserve for past dates");
            }

            List<TimeSlotDto> availableSlots = checkAvailableSlots(serviceVersionId, reservationDate, partySize);

            if (availableSlots.isEmpty()) {
                return ValidationResult.invalid("No available time slots for the requested date and party size");
            }

            boolean timeAvailable = availableSlots.stream()
                    .anyMatch(slot -> !slot.getSlotStart().toLocalTime().isAfter(reservationTime) &&
                            !slot.getSlotEnd().toLocalTime().isBefore(reservationTime));

            if (!timeAvailable) {
                return ValidationResult.invalid("Requested time is not available. Available times: " +
                        availableSlots.stream()
                            .map(s -> s.getSlotStart().toLocalTime().toString())
                            .collect(Collectors.joining(", ")));
            }

            log.info("Reservation validation successful for serviceVersion={}, date={}, time={}",
                    serviceVersionId, reservationDate, reservationTime);

            return ValidationResult.valid();

        } catch (Exception e) {
            log.error("Error validating reservation datetime", e);
            return ValidationResult.invalid("Error validating reservation: " + e.getMessage());
        }
    }

    public List<TimeSlotDto> checkAvailableSlots(Long serviceVersionId, LocalDate reservationDate, Integer partySize) {
        try {
            log.debug("Checking available slots - serviceVersion={}, date={}, partySize={}",
                    serviceVersionId, reservationDate, partySize);

            if (partySize == null || partySize <= 0) {
                log.warn("Invalid party size: {}", partySize);
                return new ArrayList<>();
            }

            ServiceVersion serviceVersion = serviceVersionDAO.findById(serviceVersionId)
                    .orElseThrow(() -> new IllegalArgumentException("Service version not found"));

            Long restaurantId = serviceVersion.getService().getRestaurant().getId();
            // Use restaurant ID as user ID for internal validation (ownership check bypassed)
            Long ownerId = restaurantId;

            List<TimeSlotDto> allSlots = scheduleService.getActiveTimeSlotsForDate(
                    serviceVersionId, reservationDate, restaurantId, ownerId);

            List<TimeSlotDto> availableSlots = allSlots.stream()
                    .filter(TimeSlotDto::isAvailable)
                    .filter(slot -> slot.getAvailableCapacity() != null && slot.getAvailableCapacity() >= partySize)
                    .collect(Collectors.toList());

            log.debug("Found {} available slots out of {} total", availableSlots.size(), allSlots.size());

            return availableSlots;

        } catch (Exception e) {
            log.error("Error checking available slots", e);
            return new ArrayList<>();
        }
    }

    public CapacityCheckResult checkCapacity(
            Long serviceVersionId,
            LocalDate reservationDate,
            LocalTime slotStartTime,
            LocalTime slotEndTime,
            Integer requestedPartySize) {

        try {
            log.debug("Checking capacity - serviceVersion={}, date={}, time={}-{}, partySize={}",
                    serviceVersionId, reservationDate, slotStartTime, slotEndTime, requestedPartySize);

            LocalDateTime slotStart = LocalDateTime.of(reservationDate, slotStartTime);
            LocalDateTime slotEnd = LocalDateTime.of(reservationDate, slotEndTime);

            ServiceVersion serviceVersion = serviceVersionDAO.findById(serviceVersionId)
                    .orElseThrow(() -> new IllegalArgumentException("Service version not found"));

            Long restaurantId = serviceVersion.getService().getRestaurant().getId();
            // Use restaurant ID as user ID for internal validation (ownership check bypassed)
            Long ownerId = restaurantId;

            List<TimeSlotDto> slots = scheduleService.getActiveTimeSlotsForDate(
                    serviceVersionId, reservationDate, restaurantId, ownerId);

            Optional<TimeSlotDto> matchingSlot = slots.stream()
                    .filter(slot -> !slot.getSlotStart().isBefore(slotStart) &&
                            !slot.getSlotEnd().isAfter(slotEnd))
                    .findFirst();

            if (!matchingSlot.isPresent()) {
                log.warn("Slot not found: serviceVersion={}, time={}-{}", 
                        serviceVersionId, slotStartTime, slotEndTime);
                return CapacityCheckResult.notFound();
            }

            TimeSlotDto slot = matchingSlot.get();
            Integer totalCapacity = slot.getTotalCapacity();
            Integer availableCapacity = slot.getAvailableCapacity();
            Integer bookingCount = slot.getBookingCount();

            boolean hasCapacity = availableCapacity >= requestedPartySize;
            int occupancyPercent = totalCapacity > 0 ? (bookingCount * 100) / totalCapacity : 0;

            return new CapacityCheckResult(
                    slot.getSlotStart().toString(),
                    totalCapacity,
                    bookingCount,
                    availableCapacity,
                    requestedPartySize,
                    hasCapacity,
                    occupancyPercent,
                    hasCapacity ? null : "Insufficient capacity for party size"
            );

        } catch (Exception e) {
            log.error("Error checking capacity", e);
            return CapacityCheckResult.error("Error checking capacity: " + e.getMessage());
        }
    }

    public List<AvailableTimeRange> returnAvailableTimeRanges(
            Long serviceVersionId,
            LocalDate preferredDate,
            LocalTime preferredTime,
            Integer partySize,
            Integer daysAhead) {

        try {
            log.info("Finding alternative time ranges - serviceVersion={}, preferred={}, daysAhead={}",
                    serviceVersionId, preferredDate, daysAhead);

            List<AvailableTimeRange> alternatives = new ArrayList<>();
            int maxDays = daysAhead != null && daysAhead > 0 ? Math.min(daysAhead, 90) : 7;
            LocalDate searchDate = preferredDate != null ? preferredDate : LocalDate.now();

            for (int i = 0; i < maxDays && alternatives.size() < 10; i++) {
                LocalDate checkDate = searchDate.plusDays(i);
                List<TimeSlotDto> slotsForDate = checkAvailableSlots(serviceVersionId, checkDate, partySize);

                if (!slotsForDate.isEmpty()) {
                    AvailableTimeRange timeRange = new AvailableTimeRange(
                            checkDate,
                            slotsForDate,
                            slotsForDate.size(),
                            slotsForDate.get(0).getSlotStart().toLocalTime(),
                            slotsForDate.get(slotsForDate.size() - 1).getSlotEnd().toLocalTime(),
                            i
                    );
                    alternatives.add(timeRange);
                }
            }

            log.info("Found {} alternative date ranges", alternatives.size());
            return alternatives;

        } catch (Exception e) {
            log.error("Error finding alternative time ranges", e);
            return new ArrayList<>();
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, "Reservation is valid");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message != null ? message : "Reservation is invalid");
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class CapacityCheckResult {
        private final String slotId;
        private final Integer maxCapacity;
        private final Integer currentBookings;
        private final Integer availableCapacity;
        private final Integer requestedPartySize;
        private final boolean hasCapacity;
        private final Integer percentageOccupancy;
        private final String errorMessage;

        public CapacityCheckResult(String slotId, Integer maxCapacity, Integer currentBookings,
                Integer availableCapacity, Integer requestedPartySize, boolean hasCapacity,
                Integer percentageOccupancy, String errorMessage) {
            this.slotId = slotId;
            this.maxCapacity = maxCapacity;
            this.currentBookings = currentBookings;
            this.availableCapacity = availableCapacity;
            this.requestedPartySize = requestedPartySize;
            this.hasCapacity = hasCapacity;
            this.percentageOccupancy = percentageOccupancy;
            this.errorMessage = errorMessage;
        }

        public static CapacityCheckResult notFound() {
            return new CapacityCheckResult(null, null, null, null, null, false, null, "Slot not found");
        }

        public static CapacityCheckResult error(String message) {
            return new CapacityCheckResult(null, null, null, null, null, false, null, message);
        }

        public String getSlotId() { return slotId; }
        public Integer getMaxCapacity() { return maxCapacity; }
        public Integer getCurrentBookings() { return currentBookings; }
        public Integer getAvailableCapacity() { return availableCapacity; }
        public Integer getRequestedPartySize() { return requestedPartySize; }
        public boolean isHasCapacity() { return hasCapacity; }
        public Integer getPercentageOccupancy() { return percentageOccupancy; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class AvailableTimeRange {
        private final LocalDate date;
        private final List<TimeSlotDto> slots;
        private final Integer numberOfSlots;
        private final LocalTime firstAvailableTime;
        private final LocalTime lastAvailableTime;
        private final Integer daysFromPreferred;

        public AvailableTimeRange(LocalDate date, List<TimeSlotDto> slots, Integer numberOfSlots,
                LocalTime firstAvailableTime, LocalTime lastAvailableTime, Integer daysFromPreferred) {
            this.date = date;
            this.slots = slots;
            this.numberOfSlots = numberOfSlots;
            this.firstAvailableTime = firstAvailableTime;
            this.lastAvailableTime = lastAvailableTime;
            this.daysFromPreferred = daysFromPreferred;
        }

        public LocalDate getDate() { return date; }
        public List<TimeSlotDto> getSlots() { return slots; }
        public Integer getNumberOfSlots() { return numberOfSlots; }
        public LocalTime getFirstAvailableTime() { return firstAvailableTime; }
        public LocalTime getLastAvailableTime() { return lastAvailableTime; }
        public Integer getDaysFromPreferred() { return daysFromPreferred; }
    }
}
