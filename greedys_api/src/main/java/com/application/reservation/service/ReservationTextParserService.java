package com.application.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.common.service.PhoneNormalizer;
import com.application.reservation.web.dto.parsing.ParsedReservationDTO;
import com.application.reservation.web.dto.parsing.ReservationParseInput;
import com.application.reservation.web.dto.parsing.ReservationParseResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for parsing free text into structured reservation data using NLP patterns
 */
@Service
@Slf4j
public class ReservationTextParserService {

    @Autowired
    private PhoneNormalizer phoneNormalizer;

    // Pattern regex per diversi elementi
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:tel(?:efono)?[\\s:\\-]*)?([\\+]?[\\d\\s\\-\\(\\)]{8,15})", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "(?:email?[\\s:\\-]*)?([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(?:ore|ore?|alle|h|hours?)[\\s:\\-]*([0-2]?[0-9])[\\s:\\-\\.]*([0-5][0-9])|" +
        "([0-2]?[0-9])[\\s]*[:\\.]([0-5][0-9])|" +
        "([0-2]?[0-9])\\s*(?:e|:)\\s*([0-5][0-9])|" +
        "(1[3-9]|2[0-3])[\\s]*[:\\.][\\s]*([0-5][0-9])|" +  // 13:00-23:59
        "(1[2-9]|2[0-3])(?![0-9])", // 12-23 senza minuti
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?:il|per|giorno|data)\\s*([0-3]?[0-9])[\\s/\\-\\.]*([0-1]?[0-9])[\\s/\\-\\.]*(?:20)?(2[0-9])|" +
        "([0-3]?[0-9])[\\s]*[-/\\.]\\s*([0-1]?[0-9])[\\s]*[-/\\.]\\s*(?:20)?(2[0-9])|" +
        "(lunedì|martedì|mercoledì|giovedì|venerdì|sabato|domenica|lun|mar|mer|gio|ven|sab|dom)|" +
        "(oggi|domani|dopodomani)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PEOPLE_PATTERN = Pattern.compile(
        "(?:per|di|sono|siamo|in|tavolo\\s*(?:da|per)?)\\s*([0-9]{1,2})\\s*(?:persone?|pax|coperti?|ospiti?)|" +
        "([0-9]{1,2})\\s*(?:persone?|pax|coperti?|ospiti?)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "(?:nome|prenotazione|per|signore?|sig\\.?|dott\\.?|dr\\.?)\\s+([A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ][a-zàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþßÿ]+(?:\\s+[A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ][a-zàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþßÿ]+)*)",
        Pattern.CASE_INSENSITIVE
    );

    // Mapping giorni della settimana
    private static final Map<String, Integer> DAY_MAPPING = new HashMap<>();
    static {
        DAY_MAPPING.put("lunedì", 1);
        DAY_MAPPING.put("lun", 1);
        DAY_MAPPING.put("martedì", 2);
        DAY_MAPPING.put("mar", 2);
        DAY_MAPPING.put("mercoledì", 3);
        DAY_MAPPING.put("mer", 3);
        DAY_MAPPING.put("giovedì", 4);
        DAY_MAPPING.put("gio", 4);
        DAY_MAPPING.put("venerdì", 5);
        DAY_MAPPING.put("ven", 5);
        DAY_MAPPING.put("sabato", 6);
        DAY_MAPPING.put("sab", 6);
        DAY_MAPPING.put("domenica", 7);
        DAY_MAPPING.put("dom", 7);
    }

    /**
     * Parse text into structured reservation data
     * 
     * @param input Text parsing input with restaurant context
     * @return Response with parsed reservation candidates
     */
    public ReservationParseResponse parseText(ReservationParseInput input) {
        log.info("Parsing reservation text for restaurant {}: '{}'", 
                input.getRestaurantId(), input.getText());

        try {
            String text = preprocessText(input.getText());
            
            // Extract multiple reservations by splitting on common separators
            List<String> reservationSegments = splitIntoReservations(text);
            
            List<ParsedReservationDTO> parsedReservations = new ArrayList<>();
            
            for (String segment : reservationSegments) {
                ParsedReservationDTO parsed = parseReservationSegment(segment, input);
                if (parsed != null && parsed.isValid()) {
                    parsedReservations.add(parsed);
                }
            }

            // Calculate overall confidence
            double overallConfidence = calculateOverallConfidence(parsedReservations);

            ReservationParseResponse response = ReservationParseResponse.builder()
                    .originalText(input.getText())
                    .parsedReservations(parsedReservations)
                    .totalReservations(parsedReservations.size())
                    .overallConfidence(overallConfidence)
                    .processingTime(System.currentTimeMillis())
                    .build();

            log.info("Parsing completed: {} reservations found with confidence {}", 
                    parsedReservations.size(), overallConfidence);

            return response;

        } catch (Exception e) {
            log.error("Error parsing reservation text", e);
            return ReservationParseResponse.builder()
                    .originalText(input.getText())
                    .parsedReservations(new ArrayList<>())
                    .totalReservations(0)
                    .overallConfidence(0.0)
                    .error("Errore durante il parsing: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Preprocess text for better parsing
     */
    private String preprocessText(String text) {
        if (text == null) return "";
        
        return text
            .toLowerCase()
            .replaceAll("\\s+", " ") // Normalize whitespace
            .replaceAll("[\"'`]", "") // Remove quotes
            .trim();
    }

    /**
     * Split text into individual reservation segments
     */
    private List<String> splitIntoReservations(String text) {
        // Split on common reservation separators
        String[] segments = text.split("(?i)(?:\\s*[;\\n\\r]+\\s*|\\s*(?:e\\s+poi|inoltre|altra\\s+prenotazione|secondo\\s+tavolo)\\s*)");
        
        return Arrays.stream(segments)
                .map(String::trim)
                .filter(s -> !s.isEmpty() && s.length() > 10) // Minimum meaningful length
                .collect(Collectors.toList());
    }

    /**
     * Parse individual reservation segment
     */
    private ParsedReservationDTO parseReservationSegment(String segment, ReservationParseInput input) {
        log.debug("Parsing segment: '{}'", segment);

        ParsedReservationDTO.ParsedReservationDTOBuilder builder = ParsedReservationDTO.builder();
        double confidenceScore = 0.0;
        List<String> extractionLog = new ArrayList<>();

        // Extract phone numbers
        List<String> phones = extractPhones(segment);
        if (!phones.isEmpty()) {
            builder.phoneNumber(phones.get(0));
            confidenceScore += 0.3;
            extractionLog.add("Found phone: " + phones.get(0));
        }

        // Extract emails
        List<String> emails = extractEmails(segment);
        if (!emails.isEmpty()) {
            builder.email(emails.get(0));
            confidenceScore += 0.2;
            extractionLog.add("Found email: " + emails.get(0));
        }

        // Extract names
        List<String> names = extractNames(segment);
        if (!names.isEmpty()) {
            builder.customerName(names.get(0));
            confidenceScore += 0.2;
            extractionLog.add("Found name: " + names.get(0));
        }

        // Extract date and time
        LocalDateTime dateTime = extractDateTime(segment, input.getDefaultDate());
        if (dateTime != null) {
            builder.reservationDateTime(dateTime);
            confidenceScore += 0.4; // High importance
            extractionLog.add("Found date/time: " + dateTime);
        }

        // Extract number of people
        Integer people = extractPeopleCount(segment);
        if (people != null) {
            builder.numberOfPeople(people);
            confidenceScore += 0.3;
            extractionLog.add("Found people count: " + people);
        }

        // Extract notes (remaining text)
        String notes = extractNotes(segment, phones, emails, names);
        if (notes != null && !notes.trim().isEmpty()) {
            builder.notes(notes.trim());
            extractionLog.add("Extracted notes");
        }

        // Set metadata
        builder.confidence(Math.min(1.0, confidenceScore))
                .restaurantId(input.getRestaurantId())
                .originalSegment(segment)
                .extractionLog(extractionLog);

        return builder.build();
    }

    /**
     * Extract phone numbers from text
     */
    private List<String> extractPhones(String text) {
        List<String> phones = new ArrayList<>();
        Matcher matcher = PHONE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String phone = matcher.group(1).replaceAll("\\s+", "");
            
            // Normalize using PhoneNormalizer
            String normalized = phoneNormalizer.toE164(phone);
            if (normalized != null) {
                phones.add(normalized);
            } else if (phone.length() >= 8) {
                phones.add(phone); // Keep original if normalization fails
            }
        }
        
        return phones;
    }

    /**
     * Extract email addresses from text
     */
    private List<String> extractEmails(String text) {
        List<String> emails = new ArrayList<>();
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        
        while (matcher.find()) {
            emails.add(matcher.group(1).toLowerCase());
        }
        
        return emails;
    }

    /**
     * Extract names from text
     */
    private List<String> extractNames(String text) {
        List<String> names = new ArrayList<>();
        Matcher matcher = NAME_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name.length() >= 2 && name.length() <= 50) {
                names.add(capitalizeWords(name));
            }
        }
        
        return names;
    }

    /**
     * Extract date and time information
     */
    private LocalDateTime extractDateTime(String text, LocalDate defaultDate) {
        LocalDate date = extractDate(text, defaultDate);
        LocalTime time = extractTime(text);
        
        if (date != null && time != null) {
            return LocalDateTime.of(date, time);
        } else if (time != null) {
            // Use default date if only time is found
            LocalDate useDate = defaultDate != null ? defaultDate : LocalDate.now();
            return LocalDateTime.of(useDate, time);
        }
        
        return null;
    }

    /**
     * Extract date from text
     */
    private LocalDate extractDate(String text, LocalDate defaultDate) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            // Handle relative dates
            if (matcher.group(8) != null) {
                String relative = matcher.group(8).toLowerCase();
                switch (relative) {
                    case "oggi":
                        return LocalDate.now();
                    case "domani":
                        return LocalDate.now().plusDays(1);
                    case "dopodomani":
                        return LocalDate.now().plusDays(2);
                }
            }
            
            // Handle day names
            if (matcher.group(7) != null) {
                String dayName = matcher.group(7).toLowerCase();
                Integer targetDay = DAY_MAPPING.get(dayName);
                if (targetDay != null) {
                    return findNextWeekday(targetDay);
                }
            }
            
            // Handle explicit dates (dd/mm/yyyy)
            if (matcher.group(1) != null && matcher.group(2) != null) {
                try {
                    int day = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2));
                    int year = matcher.group(3) != null ? 
                        Integer.parseInt("20" + matcher.group(3)) : LocalDate.now().getYear();
                    
                    return LocalDate.of(year, month, day);
                } catch (Exception e) {
                    log.debug("Failed to parse date: {}", matcher.group());
                }
            }
            
            // Handle alternative date format
            if (matcher.group(4) != null && matcher.group(5) != null) {
                try {
                    int day = Integer.parseInt(matcher.group(4));
                    int month = Integer.parseInt(matcher.group(5));
                    int year = matcher.group(6) != null ? 
                        Integer.parseInt("20" + matcher.group(6)) : LocalDate.now().getYear();
                    
                    return LocalDate.of(year, month, day);
                } catch (Exception e) {
                    log.debug("Failed to parse alternative date: {}", matcher.group());
                }
            }
        }
        
        return defaultDate;
    }

    /**
     * Extract time from text
     */
    private LocalTime extractTime(String text) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        
        while (matcher.find()) {
            try {
                Integer hour = null;
                Integer minute = 0;
                
                // Try different capturing groups
                if (matcher.group(1) != null) {
                    hour = Integer.parseInt(matcher.group(1));
                    minute = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
                } else if (matcher.group(3) != null) {
                    hour = Integer.parseInt(matcher.group(3));
                    minute = Integer.parseInt(matcher.group(4));
                } else if (matcher.group(5) != null) {
                    hour = Integer.parseInt(matcher.group(5));
                    minute = Integer.parseInt(matcher.group(6));
                } else if (matcher.group(7) != null) {
                    hour = Integer.parseInt(matcher.group(7));
                    minute = Integer.parseInt(matcher.group(8));
                } else if (matcher.group(9) != null) {
                    hour = Integer.parseInt(matcher.group(9));
                    minute = 0;
                }
                
                if (hour != null && hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                    return LocalTime.of(hour, minute);
                }
                
            } catch (Exception e) {
                log.debug("Failed to parse time: {}", matcher.group());
            }
        }
        
        return null;
    }

    /**
     * Extract number of people
     */
    private Integer extractPeopleCount(String text) {
        Matcher matcher = PEOPLE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            try {
                String numberStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                int count = Integer.parseInt(numberStr);
                
                if (count >= 1 && count <= 50) { // Reasonable limits
                    return count;
                }
            } catch (Exception e) {
                log.debug("Failed to parse people count: {}", matcher.group());
            }
        }
        
        return null;
    }

    /**
     * Extract notes by removing identified elements
     */
    private String extractNotes(String segment, List<String> phones, List<String> emails, List<String> names) {
        String notes = segment;
        
        // Remove identified patterns
        notes = PHONE_PATTERN.matcher(notes).replaceAll("");
        notes = EMAIL_PATTERN.matcher(notes).replaceAll("");
        notes = TIME_PATTERN.matcher(notes).replaceAll("");
        notes = DATE_PATTERN.matcher(notes).replaceAll("");
        notes = PEOPLE_PATTERN.matcher(notes).replaceAll("");
        
        // Remove identified names
        for (String name : names) {
            notes = notes.replaceAll("(?i)" + Pattern.quote(name), "");
        }
        
        // Clean up
        notes = notes.replaceAll("\\s+", " ").trim();
        notes = notes.replaceAll("^[\\s,;:-]+|[\\s,;:-]+$", "");
        
        return notes.length() > 5 ? notes : null;
    }

    /**
     * Find next occurrence of a weekday
     */
    private LocalDate findNextWeekday(int targetDay) {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfWeek().getValue();
        
        int daysToAdd = targetDay - currentDay;
        if (daysToAdd <= 0) {
            daysToAdd += 7; // Next week
        }
        
        return today.plusDays(daysToAdd);
    }

    /**
     * Capitalize words in a string
     */
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        
        return Arrays.stream(text.split("\\s+"))
                .map(word -> word.isEmpty() ? word : 
                     Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Calculate overall confidence for all parsed reservations
     */
    private double calculateOverallConfidence(List<ParsedReservationDTO> reservations) {
        if (reservations.isEmpty()) {
            return 0.0;
        }
        
        return reservations.stream()
                .mapToDouble(ParsedReservationDTO::getConfidence)
                .average()
                .orElse(0.0);
    }
}