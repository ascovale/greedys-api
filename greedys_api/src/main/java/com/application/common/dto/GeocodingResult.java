package com.application.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for geocoding results containing detailed address information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeocodingResult {
    
    private String formattedAddress;
    private String streetNumber;
    private String route;
    private String locality; // City
    private String administrativeAreaLevel1; // State/Province
    private String administrativeAreaLevel2; // County/Province
    private String country;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String placeId;
    
    /**
     * Checks if the postal code matches the one provided
     */
    public boolean postalCodeMatches(String providedPostalCode) {
        if (this.postalCode == null || providedPostalCode == null) {
            return false;
        }
        return this.postalCode.replaceAll("\\s", "")
               .equalsIgnoreCase(providedPostalCode.replaceAll("\\s", ""));
    }
    
    /**
     * Gets the full city name (locality + administrative area if needed)
     */
    public String getFullCityName() {
        if (locality != null) {
            return locality;
        }
        if (administrativeAreaLevel2 != null) {
            return administrativeAreaLevel2;
        }
        return null;
    }
    
    /**
     * Gets state/province information
     */
    public String getStateProvince() {
        return administrativeAreaLevel1;
    }
    
    /**
     * Validates if all essential information is present
     */
    public boolean isComplete() {
        return formattedAddress != null && 
               latitude != null && 
               longitude != null &&
               (locality != null || administrativeAreaLevel2 != null) &&
               country != null;
    }
}
