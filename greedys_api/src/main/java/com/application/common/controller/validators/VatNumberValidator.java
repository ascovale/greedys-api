package com.application.common.controller.validators;

import java.util.Map;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for international VAT/Tax numbers supporting multiple countries worldwide.
 * 
 * This validator supports VAT numbers from:
 * - All 27 European Union countries (VIES format)
 * - European non-EU countries (UK, Switzerland, Norway, etc.)
 * - North America (USA, Canada, Mexico)
 * - Asia-Pacific (Australia, Japan, Singapore, India, etc.)
 * - South America (Brazil, Argentina, Chile, etc.)
 * - Africa (South Africa, Egypt, Morocco, etc.)
 * - Middle East (UAE, Saudi Arabia, Israel, etc.)
 * 
 * Each country has its specific format that must be followed.
 * The validator handles various input formats (spaces, hyphens, case variations).
 * 
 * Examples of supported formats:
 * - EU: IT12345678901 (Italy), FR12345678901 (France), DE123456789 (Germany)
 * - North America: US12-3456789 (USA), CA123456789RT0001 (Canada)
 * - Asia: AU12345678901 (Australia), JP1234567890123 (Japan)
 * - And many more...
 * 
 * @author Generated for Greedys API
 */
public class VatNumberValidator implements ConstraintValidator<ValidVatNumber, String> {
    
    private boolean allowNull;
    
    // Map of country codes to their respective VAT/Tax number patterns
    private static final Map<String, Pattern> VAT_PATTERNS = Map.ofEntries(
        // === EUROPEAN UNION (27 countries) ===
        Map.entry("IT", Pattern.compile("^IT[0-9]{11}$")), // Italy
        Map.entry("FR", Pattern.compile("^FR[A-Z0-9]{2}[0-9]{9}$")), // France  
        Map.entry("DE", Pattern.compile("^DE[0-9]{9}$")), // Germany
        Map.entry("ES", Pattern.compile("^ES[A-Z][0-9]{7}[A-Z0-9]$")), // Spain
        Map.entry("NL", Pattern.compile("^NL[0-9]{9}B[0-9]{2}$")), // Netherlands
        Map.entry("BE", Pattern.compile("^BE[0-9]{10}$")), // Belgium
        Map.entry("AT", Pattern.compile("^ATU[0-9]{8}$")), // Austria
        Map.entry("PT", Pattern.compile("^PT[0-9]{9}$")), // Portugal
        Map.entry("EL", Pattern.compile("^EL[0-9]{9}$")), // Greece
        Map.entry("GR", Pattern.compile("^GR[0-9]{9}$")), // Greece (alternative code)
        Map.entry("IE", Pattern.compile("^IE([0-9]{7}[A-Z]|[0-9][A-Z][0-9]{5}[A-Z])$")), // Ireland
        Map.entry("LU", Pattern.compile("^LU[0-9]{8}$")), // Luxembourg
        Map.entry("FI", Pattern.compile("^FI[0-9]{8}$")), // Finland
        Map.entry("SE", Pattern.compile("^SE[0-9]{12}$")), // Sweden
        Map.entry("DK", Pattern.compile("^DK[0-9]{8}$")), // Denmark
        Map.entry("PL", Pattern.compile("^PL[0-9]{10}$")), // Poland
        Map.entry("CZ", Pattern.compile("^CZ[0-9]{8,10}$")), // Czech Republic
        Map.entry("SK", Pattern.compile("^SK[0-9]{10}$")), // Slovakia
        Map.entry("SI", Pattern.compile("^SI[0-9]{8}$")), // Slovenia
        Map.entry("HU", Pattern.compile("^HU[0-9]{8}$")), // Hungary
        Map.entry("HR", Pattern.compile("^HR[0-9]{11}$")), // Croatia
        Map.entry("RO", Pattern.compile("^RO[0-9]{2,10}$")), // Romania
        Map.entry("BG", Pattern.compile("^BG[0-9]{9,10}$")), // Bulgaria
        Map.entry("LT", Pattern.compile("^LT([0-9]{9}|[0-9]{12})$")), // Lithuania
        Map.entry("LV", Pattern.compile("^LV[0-9]{11}$")), // Latvia
        Map.entry("EE", Pattern.compile("^EE[0-9]{9}$")), // Estonia
        Map.entry("MT", Pattern.compile("^MT[0-9]{8}$")), // Malta
        Map.entry("CY", Pattern.compile("^CY[0-9]{8}[A-Z]$")), // Cyprus
        
        // === EUROPEAN NON-EU COUNTRIES ===
        Map.entry("GB", Pattern.compile("^GB([0-9]{9}|[0-9]{12}|GD[0-9]{3}|HA[0-9]{3})$")), // United Kingdom
        Map.entry("CH", Pattern.compile("^CHE[0-9]{9}(MWST|TVA|IVA)$")), // Switzerland
        Map.entry("NO", Pattern.compile("^NO[0-9]{9}MVA$")), // Norway
        Map.entry("IS", Pattern.compile("^IS[0-9]{5,6}$")), // Iceland
        Map.entry("LI", Pattern.compile("^LI[0-9]{5}$")), // Liechtenstein
        Map.entry("AD", Pattern.compile("^AD[A-Z][0-9]{6}[A-Z0-9]$")), // Andorra
        Map.entry("MC", Pattern.compile("^MC[0-9]{11}$")), // Monaco
        Map.entry("SM", Pattern.compile("^SM[0-9]{5}$")), // San Marino
        Map.entry("VA", Pattern.compile("^VA[0-9]{3}$")), // Vatican City
        
        // === NORTH AMERICA ===
        Map.entry("US", Pattern.compile("^US[0-9]{2}-?[0-9]{7}$")), // USA (EIN format)
        Map.entry("CA", Pattern.compile("^CA[0-9]{9}RT[0-9]{4}$")), // Canada (GST/HST number)
        Map.entry("MX", Pattern.compile("^MX[A-Z0-9]{10,13}$")), // Mexico (RFC)
        
        // === ASIA-PACIFIC ===
        Map.entry("AU", Pattern.compile("^AU[0-9]{11}$")), // Australia (ABN)
        Map.entry("NZ", Pattern.compile("^NZ[0-9]{8,9}$")), // New Zealand (GST number)
        Map.entry("JP", Pattern.compile("^JP[0-9]{13}$")), // Japan (Corporate Number)
        Map.entry("KR", Pattern.compile("^KR[0-9]{10}$")), // South Korea (Business Registration Number)
        Map.entry("SG", Pattern.compile("^SG[0-9]{8}[A-Z]$")), // Singapore (UEN)
        Map.entry("HK", Pattern.compile("^HK[0-9]{8}$")), // Hong Kong (Business Registration Number)
        Map.entry("IN", Pattern.compile("^IN[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9][A-Z][0-9]$")), // India (GSTIN)
        Map.entry("MY", Pattern.compile("^MY[0-9]{12}$")), // Malaysia (GST/SST number)
        Map.entry("TH", Pattern.compile("^TH[0-9]{13}$")), // Thailand (Tax ID)
        Map.entry("PH", Pattern.compile("^PH[0-9]{3}-[0-9]{3}-[0-9]{3}-[0-9]{3}$")), // Philippines (TIN)
        Map.entry("VN", Pattern.compile("^VN[0-9]{10,13}$")), // Vietnam (Tax Code)
        Map.entry("ID", Pattern.compile("^ID[0-9]{15}$")), // Indonesia (NPWP)
        
        // === SOUTH AMERICA ===
        Map.entry("BR", Pattern.compile("^BR[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}/[0-9]{4}-[0-9]{2}$")), // Brazil (CNPJ)
        Map.entry("AR", Pattern.compile("^AR[0-9]{11}$")), // Argentina (CUIT)
        Map.entry("CL", Pattern.compile("^CL[0-9]{8}-[0-9K]$")), // Chile (RUT)
        Map.entry("CO", Pattern.compile("^CO[0-9]{9}$")), // Colombia (NIT)
        Map.entry("PE", Pattern.compile("^PE[0-9]{11}$")), // Peru (RUC)
        Map.entry("UY", Pattern.compile("^UY[0-9]{12}$")), // Uruguay (RUT)
        
        // === AFRICA ===
        Map.entry("ZA", Pattern.compile("^ZA[0-9]{10}$")), // South Africa (VAT number)
        Map.entry("EG", Pattern.compile("^EG[0-9]{9}$")), // Egypt (Tax Registration Number)
        Map.entry("MA", Pattern.compile("^MA[0-9]{8}$")), // Morocco (Tax ID)
        Map.entry("TN", Pattern.compile("^TN[0-9]{7}[A-Z]{3}[0-9]{3}$")), // Tunisia (Tax ID)
        
        // === MIDDLE EAST ===
        Map.entry("AE", Pattern.compile("^AE[0-9]{15}$")), // UAE (TRN)
        Map.entry("SA", Pattern.compile("^SA[0-9]{15}$")), // Saudi Arabia (VAT number)
        Map.entry("IL", Pattern.compile("^IL[0-9]{9}$")), // Israel (VAT number)
        Map.entry("TR", Pattern.compile("^TR[0-9]{10}$")), // Turkey (Tax number)
        
        // === OTHER EUROPEAN COUNTRIES ===
        Map.entry("RU", Pattern.compile("^RU[0-9]{10,12}$")), // Russia (INN)
        Map.entry("UA", Pattern.compile("^UA[0-9]{8,10}$")), // Ukraine (Tax number)
        Map.entry("BY", Pattern.compile("^BY[0-9]{9}$")), // Belarus (Tax number)
        Map.entry("RS", Pattern.compile("^RS[0-9]{9}$")), // Serbia (PIB)
        Map.entry("BA", Pattern.compile("^BA[0-9]{12}$")), // Bosnia and Herzegovina
        Map.entry("ME", Pattern.compile("^ME[0-9]{8}$")), // Montenegro
        Map.entry("MK", Pattern.compile("^MK[0-9]{13}$")), // North Macedonia
        Map.entry("AL", Pattern.compile("^AL[A-Z][0-9]{8}[A-Z]$")) // Albania
    );
    
    @Override
    public void initialize(ValidVatNumber constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }
    
    @Override
    public boolean isValid(String vatNumber, ConstraintValidatorContext context) {
        // Handle null values
        if (vatNumber == null) {
            return allowNull;
        }
        
        // Handle empty or blank values
        if (vatNumber.trim().isEmpty()) {
            return allowNull;
        }
        
        // Convert to uppercase and remove any spaces/hyphens
        String cleanVatNumber = vatNumber.trim().toUpperCase().replaceAll("[-\\s]", "");
        
        // VAT number must be at least 4 characters (2 for country code + minimum digits)
        if (cleanVatNumber.length() < 4) {
            addCustomMessage(context, "VAT/Tax number must contain at least the country code (2 letters) followed by the national number");
            return false;
        }
        
        // Extract country code (first 2 characters)
        String countryCode = cleanVatNumber.substring(0, 2);
        
        // Check if country code exists in our patterns
        if (!VAT_PATTERNS.containsKey(countryCode)) {
            addCustomMessage(context, String.format("Country code '%s' not supported. Supported countries: %s", 
                countryCode, String.join(", ", VAT_PATTERNS.keySet())));
            return false;
        }
        
        // Validate against country-specific pattern
        Pattern pattern = VAT_PATTERNS.get(countryCode);
        boolean isValid = pattern.matcher(cleanVatNumber).matches();
        
        if (!isValid) {
            addCustomMessage(context, String.format("Invalid VAT/Tax number format for %s. Expected format: %s", 
                getCountryName(countryCode), getFormatDescription(countryCode)));
        }
        
        return isValid;
    }
    
    /**
     * Adds a custom error message to the validation context
     */
    private void addCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
    
    /**
     * Returns a human-readable country name for the given country code
     */
    private String getCountryName(String countryCode) {
        return switch (countryCode) {
            // European Union
            case "IT" -> "Italy";
            case "FR" -> "France";
            case "DE" -> "Germany";
            case "ES" -> "Spain";
            case "NL" -> "Netherlands";
            case "BE" -> "Belgium";
            case "AT" -> "Austria";
            case "PT" -> "Portugal";
            case "EL", "GR" -> "Greece";
            case "IE" -> "Ireland";
            case "LU" -> "Luxembourg";
            case "FI" -> "Finland";
            case "SE" -> "Sweden";
            case "DK" -> "Denmark";
            case "PL" -> "Poland";
            case "CZ" -> "Czech Republic";
            case "SK" -> "Slovakia";
            case "SI" -> "Slovenia";
            case "HU" -> "Hungary";
            case "HR" -> "Croatia";
            case "RO" -> "Romania";
            case "BG" -> "Bulgaria";
            case "LT" -> "Lithuania";
            case "LV" -> "Latvia";
            case "EE" -> "Estonia";
            case "MT" -> "Malta";
            case "CY" -> "Cyprus";
            
            // Europe (non-EU)
            case "GB" -> "United Kingdom";
            case "CH" -> "Switzerland";
            case "NO" -> "Norway";
            case "IS" -> "Iceland";
            case "LI" -> "Liechtenstein";
            case "AD" -> "Andorra";
            case "MC" -> "Monaco";
            case "SM" -> "San Marino";
            case "VA" -> "Vatican City";
            case "RU" -> "Russia";
            case "UA" -> "Ukraine";
            case "BY" -> "Belarus";
            case "RS" -> "Serbia";
            case "BA" -> "Bosnia and Herzegovina";
            case "ME" -> "Montenegro";
            case "MK" -> "North Macedonia";
            case "AL" -> "Albania";
            case "TR" -> "Turkey";
            
            // North America
            case "US" -> "United States";
            case "CA" -> "Canada";
            case "MX" -> "Mexico";
            
            // Asia-Pacific
            case "AU" -> "Australia";
            case "NZ" -> "New Zealand";
            case "JP" -> "Japan";
            case "KR" -> "South Korea";
            case "SG" -> "Singapore";
            case "HK" -> "Hong Kong";
            case "IN" -> "India";
            case "MY" -> "Malaysia";
            case "TH" -> "Thailand";
            case "PH" -> "Philippines";
            case "VN" -> "Vietnam";
            case "ID" -> "Indonesia";
            
            // South America
            case "BR" -> "Brazil";
            case "AR" -> "Argentina";
            case "CL" -> "Chile";
            case "CO" -> "Colombia";
            case "PE" -> "Peru";
            case "UY" -> "Uruguay";
            
            // Africa
            case "ZA" -> "South Africa";
            case "EG" -> "Egypt";
            case "MA" -> "Morocco";
            case "TN" -> "Tunisia";
            
            // Middle East
            case "AE" -> "United Arab Emirates";
            case "SA" -> "Saudi Arabia";
            case "IL" -> "Israel";
            
            default -> countryCode;
        };
    }
    
    /**
     * Returns format description for the given country code
     */
    private String getFormatDescription(String countryCode) {
        return switch (countryCode) {
            // European Union
            case "IT" -> "IT + 11 digits (e.g. IT12345678901)";
            case "FR" -> "FR + 2 characters + 9 digits (e.g. FR12345678901)";
            case "DE" -> "DE + 9 digits (e.g. DE123456789)";
            case "ES" -> "ES + 1 letter + 7 digits + 1 alphanumeric character (e.g. ESA12345678)";
            case "NL" -> "NL + 9 digits + B + 2 digits (e.g. NL123456789B01)";
            case "BE" -> "BE + 10 digits (e.g. BE1234567890)";
            case "AT" -> "ATU + 8 digits (e.g. ATU12345678)";
            case "PT" -> "PT + 9 digits (e.g. PT123456789)";
            case "EL", "GR" -> "EL/GR + 9 digits (e.g. EL123456789)";
            case "IE" -> "IE + 7 digits + 1 letter OR IE + 1 digit + 1 letter + 5 digits + 1 letter";
            case "LU" -> "LU + 8 digits (e.g. LU12345678)";
            case "FI" -> "FI + 8 digits (e.g. FI12345678)";
            case "SE" -> "SE + 12 digits (e.g. SE123456789012)";
            case "DK" -> "DK + 8 digits (e.g. DK12345678)";
            case "PL" -> "PL + 10 digits (e.g. PL1234567890)";
            case "CZ" -> "CZ + 8-10 digits (e.g. CZ12345678)";
            case "SK" -> "SK + 10 digits (e.g. SK1234567890)";
            case "SI" -> "SI + 8 digits (e.g. SI12345678)";
            case "HU" -> "HU + 8 digits (e.g. HU12345678)";
            case "HR" -> "HR + 11 digits (e.g. HR12345678901)";
            case "RO" -> "RO + 2-10 digits (e.g. RO123456789)";
            case "BG" -> "BG + 9-10 digits (e.g. BG123456789)";
            case "LT" -> "LT + 9 or 12 digits (e.g. LT123456789)";
            case "LV" -> "LV + 11 digits (e.g. LV12345678901)";
            case "EE" -> "EE + 9 digits (e.g. EE123456789)";
            case "MT" -> "MT + 8 digits (e.g. MT12345678)";
            case "CY" -> "CY + 8 digits + 1 letter (e.g. CY12345678A)";
            
            // Europe (non-EU)
            case "GB" -> "GB + variable format (e.g. GB123456789)";
            case "CH" -> "CHE + 9 digits + MWST/TVA/IVA (e.g. CHE123456789MWST)";
            case "NO" -> "NO + 9 digits + MVA (e.g. NO123456789MVA)";
            case "IS" -> "IS + 5-6 digits (e.g. IS12345)";
            case "LI" -> "LI + 5 digits (e.g. LI12345)";
            case "AD" -> "AD + 1 letter + 6 digits + 1 character (e.g. ADA123456B)";
            case "MC" -> "MC + 11 digits (e.g. MC12345678901)";
            case "SM" -> "SM + 5 digits (e.g. SM12345)";
            case "VA" -> "VA + 3 digits (e.g. VA123)";
            
            // North America
            case "US" -> "US + 2 digits + 7 digits (EIN) (e.g. US12-3456789)";
            case "CA" -> "CA + 9 digits + RT + 4 digits (e.g. CA123456789RT0001)";
            case "MX" -> "MX + 10-13 alphanumeric characters (RFC) (e.g. MXABC123456DEF)";
            
            // Asia-Pacific
            case "AU" -> "AU + 11 digits (ABN) (e.g. AU12345678901)";
            case "NZ" -> "NZ + 8-9 digits (e.g. NZ12345678)";
            case "JP" -> "JP + 13 digits (e.g. JP1234567890123)";
            case "KR" -> "KR + 10 digits (e.g. KR1234567890)";
            case "SG" -> "SG + 8 digits + 1 letter (UEN) (e.g. SG12345678A)";
            case "HK" -> "HK + 8 digits (e.g. HK12345678)";
            case "IN" -> "IN + complex GSTIN format (e.g. IN22AAAAA0000A1Z5)";
            case "MY" -> "MY + 12 digits (e.g. MY123456789012)";
            case "TH" -> "TH + 13 digits (e.g. TH1234567890123)";
            case "PH" -> "PH + XXX-XXX-XXX-XXX format (e.g. PH123-456-789-000)";
            case "VN" -> "VN + 10-13 digits (e.g. VN1234567890)";
            case "ID" -> "ID + 15 digits (NPWP) (e.g. ID123456789012345)";
            
            // South America
            case "BR" -> "BR + CNPJ format XX.XXX.XXX/XXXX-XX (e.g. BR12.345.678/0001-90)";
            case "AR" -> "AR + 11 digits (CUIT) (e.g. AR12345678901)";
            case "CL" -> "CL + 8 digits + 1 character (RUT) (e.g. CL12345678-9)";
            case "CO" -> "CO + 9 digits (NIT) (e.g. CO123456789)";
            case "PE" -> "PE + 11 digits (RUC) (e.g. PE12345678901)";
            case "UY" -> "UY + 12 digits (RUT) (e.g. UY123456789012)";
            
            // Africa
            case "ZA" -> "ZA + 10 digits (e.g. ZA1234567890)";
            case "EG" -> "EG + 9 digits (e.g. EG123456789)";
            case "MA" -> "MA + 8 digits (e.g. MA12345678)";
            case "TN" -> "TN + complex format (e.g. TN1234567ABC123)";
            
            // Middle East
            case "AE" -> "AE + 15 digits (TRN) (e.g. AE123456789012345)";
            case "SA" -> "SA + 15 digits (e.g. SA123456789012345)";
            case "IL" -> "IL + 9 digits (e.g. IL123456789)";
            case "TR" -> "TR + 10 digits (e.g. TR1234567890)";
            
            // Other European
            case "RU" -> "RU + 10-12 digits (INN) (e.g. RU1234567890)";
            case "UA" -> "UA + 8-10 digits (e.g. UA12345678)";
            case "BY" -> "BY + 9 digits (e.g. BY123456789)";
            case "RS" -> "RS + 9 digits (PIB) (e.g. RS123456789)";
            case "BA" -> "BA + 12 digits (e.g. BA123456789012)";
            case "ME" -> "ME + 8 digits (e.g. ME12345678)";
            case "MK" -> "MK + 13 digits (e.g. MK1234567890123)";
            case "AL" -> "AL + 1 letter + 8 digits + 1 letter (e.g. ALA12345678B)";
            
            default -> "Country-specific format for " + countryCode;
        };
    }
}
