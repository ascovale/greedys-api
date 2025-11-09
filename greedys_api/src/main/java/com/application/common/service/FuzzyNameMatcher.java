package com.application.common.service;

import java.text.Normalizer;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for fuzzy name matching using simplified Jaro-Winkler algorithm
 */
@Service
@Slf4j
public class FuzzyNameMatcher {

    private static final double DEFAULT_THRESHOLD = 0.7;

    /**
     * Calculate similarity between two names using Jaro-Winkler distance
     * 
     * @param name1 First name to compare
     * @param name2 Second name to compare
     * @return Similarity score between 0.0 and 1.0 (1.0 = perfect match)
     */
    public double calculateSimilarity(String name1, String name2) {
        if (name1 == null && name2 == null) {
            return 1.0;
        }
        
        if (name1 == null || name2 == null) {
            return 0.0;
        }

        // Normalize both names
        String normalized1 = normalizeName(name1);
        String normalized2 = normalizeName(name2);

        if (normalized1.isEmpty() && normalized2.isEmpty()) {
            return 1.0;
        }
        
        if (normalized1.isEmpty() || normalized2.isEmpty()) {
            return 0.0;
        }

        // Calculate Jaro-Winkler distance
        double similarity = jaroWinklerSimilarity(normalized1, normalized2);
        
        log.debug("Name similarity between '{}' and '{}': {} (normalized: '{}' vs '{}')", 
                 name1, name2, similarity, normalized1, normalized2);
        
        return similarity;
    }

    /**
     * Calculate similarity between two full names (first + last)
     * Uses component-wise matching for better accuracy
     */
    public double calculateFullNameSimilarity(String firstName1, String lastName1, 
                                            String firstName2, String lastName2) {
        
        // Handle null cases
        String fullName1 = buildFullName(firstName1, lastName1);
        String fullName2 = buildFullName(firstName2, lastName2);
        
        if (fullName1.isEmpty() && fullName2.isEmpty()) {
            return 1.0;
        }
        
        if (fullName1.isEmpty() || fullName2.isEmpty()) {
            return 0.0;
        }

        // Split names into components
        String[] components1 = splitNameComponents(fullName1);
        String[] components2 = splitNameComponents(fullName2);

        // Try different matching strategies
        double directSimilarity = calculateSimilarity(fullName1, fullName2);
        double componentSimilarity = calculateComponentSimilarity(components1, components2);
        double swappedSimilarity = calculateSwappedSimilarity(components1, components2);

        // Return the highest similarity score
        double maxSimilarity = Math.max(directSimilarity, 
                                      Math.max(componentSimilarity, swappedSimilarity));

        log.debug("Full name similarity: direct={}, component={}, swapped={}, max={}", 
                 directSimilarity, componentSimilarity, swappedSimilarity, maxSimilarity);
        
        return maxSimilarity;
    }

    /**
     * Check if two names are considered a match based on threshold
     */
    public boolean isMatch(String name1, String name2, double threshold) {
        return calculateSimilarity(name1, name2) >= threshold;
    }

    /**
     * Check if two full names are considered a match based on threshold
     */
    public boolean isFullNameMatch(String firstName1, String lastName1, 
                                  String firstName2, String lastName2, 
                                  double threshold) {
        return calculateFullNameSimilarity(firstName1, lastName1, firstName2, lastName2) >= threshold;
    }

    /**
     * Simplified Jaro-Winkler similarity algorithm implementation
     */
    private double jaroWinklerSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        int s1Length = s1.length();
        int s2Length = s2.length();
        
        if (s1Length == 0 || s2Length == 0) {
            return 0.0;
        }

        // Jaro similarity calculation
        double jaroSim = jaroSimilarity(s1, s2);
        
        if (jaroSim < 0.7) {
            return jaroSim;
        }

        // Winkler modification - bonus for common prefix
        int prefixLength = 0;
        int maxPrefix = Math.min(4, Math.min(s1Length, s2Length));
        
        for (int i = 0; i < maxPrefix && s1.charAt(i) == s2.charAt(i); i++) {
            prefixLength++;
        }

        return jaroSim + (0.1 * prefixLength * (1.0 - jaroSim));
    }

    /**
     * Simplified Jaro similarity calculation
     */
    private double jaroSimilarity(String s1, String s2) {
        int s1Length = s1.length();
        int s2Length = s2.length();

        if (s1Length == 0 && s2Length == 0) {
            return 1.0;
        }

        if (s1Length == 0 || s2Length == 0) {
            return 0.0;
        }

        // Calculate match window
        int matchWindow = Math.max(s1Length, s2Length) / 2 - 1;
        if (matchWindow < 0) {
            matchWindow = 0;
        }

        boolean[] s1Matches = new boolean[s1Length];
        boolean[] s2Matches = new boolean[s2Length];

        int matches = 0;
        int transpositions = 0;

        // Identify matches
        for (int i = 0; i < s1Length; i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(i + matchWindow + 1, s2Length);

            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        // Count transpositions
        int k = 0;
        for (int i = 0; i < s1Length; i++) {
            if (!s1Matches[i]) {
                continue;
            }
            while (!s2Matches[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        return (((double) matches / s1Length) + 
                ((double) matches / s2Length) + 
                ((double) (matches - transpositions / 2.0) / matches)) / 3.0;
    }

    /**
     * Normalize a name for comparison
     */
    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }

        // Remove accents and normalize Unicode
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                                     .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convert to lowercase and remove extra whitespace
        normalized = normalized.toLowerCase()
                              .trim()
                              .replaceAll("\\s+", " ");

        // Remove common prefixes and suffixes
        normalized = removeCommonTitles(normalized);

        return normalized;
    }

    /**
     * Build full name from components, handling nulls
     */
    private String buildFullName(String firstName, String lastName) {
        StringBuilder fullName = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        
        return fullName.toString();
    }

    /**
     * Split name into components for component-wise matching
     */
    private String[] splitNameComponents(String fullName) {
        return normalizeName(fullName).split("\\s+");
    }

    /**
     * Calculate similarity using component-wise matching
     */
    private double calculateComponentSimilarity(String[] components1, String[] components2) {
        if (components1.length == 0 && components2.length == 0) {
            return 1.0;
        }
        
        if (components1.length == 0 || components2.length == 0) {
            return 0.0;
        }

        double totalSimilarity = 0.0;
        int matches = 0;

        // Match each component with the best matching component from the other name
        for (String comp1 : components1) {
            double bestMatch = 0.0;
            for (String comp2 : components2) {
                double similarity = jaroWinklerSimilarity(comp1, comp2);
                bestMatch = Math.max(bestMatch, similarity);
            }
            totalSimilarity += bestMatch;
            matches++;
        }

        return matches > 0 ? totalSimilarity / matches : 0.0;
    }

    /**
     * Calculate similarity with swapped components (e.g., "Mario Rossi" vs "Rossi Mario")
     */
    private double calculateSwappedSimilarity(String[] components1, String[] components2) {
        if (components1.length != 2 || components2.length != 2) {
            return 0.0; // Only meaningful for exactly 2 components
        }

        // Try swapped matching: first1 vs last2, last1 vs first2
        double firstToLast = jaroWinklerSimilarity(components1[0], components2[1]);
        double lastToFirst = jaroWinklerSimilarity(components1[1], components2[0]);

        return (firstToLast + lastToFirst) / 2.0;
    }

    /**
     * Remove common titles and honorifics
     */
    private String removeCommonTitles(String name) {
        // Common Italian titles and prefixes
        String[] titles = {
            "sig", "sig.", "signor", "signora", "signorina",
            "dott", "dott.", "dottore", "dottoressa",
            "prof", "prof.", "professore", "professoressa",
            "ing", "ing.", "ingegnere",
            "avv", "avv.", "avvocato", "avvocatessa",
            "dr", "dr.", "mr", "mrs", "ms", "miss"
        };

        for (String title : titles) {
            // Remove title at the beginning
            if (name.startsWith(title + " ")) {
                name = name.substring(title.length() + 1);
            }
            if (name.startsWith(title)) {
                name = name.substring(title.length());
            }
        }

        return name.trim();
    }
}