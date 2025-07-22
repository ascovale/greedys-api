package com.application.common.controller.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

/**
 * Unit tests for VatNumberValidator
 * Tests various VAT number formats for different European countries
 */
class VatNumberValidatorTest {

    private VatNumberValidator validator;
    
    @Mock
    private ValidVatNumber validVatNumber;
    
    @Mock
    private ConstraintValidatorContext context;
    
    @Mock
    private ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new VatNumberValidator();
        
        // Setup default mock behavior
        when(validVatNumber.allowNull()).thenReturn(true);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        
        validator.initialize(validVatNumber);
    }

    @Test
    @DisplayName("Should accept null values when allowNull is true")
    void testNullValuesAllowed() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("Should reject null values when allowNull is false")
    void testNullValuesNotAllowed() {
        when(validVatNumber.allowNull()).thenReturn(false);
        validator.initialize(validVatNumber);
        
        assertFalse(validator.isValid(null, context));
    }

    @Test
    @DisplayName("Should accept empty string when allowNull is true")
    void testEmptyStringAllowed() {
        assertTrue(validator.isValid("", context));
        assertTrue(validator.isValid("   ", context));
    }

    @Test
    @DisplayName("Should validate Italian VAT numbers correctly")
    void testItalianVatNumbers() {
        // Valid Italian VAT numbers
        assertTrue(validator.isValid("IT12345678901", context));
        assertTrue(validator.isValid("it12345678901", context)); // lowercase should work
        assertTrue(validator.isValid("IT 1234 5678 901", context)); // with spaces
        
        // Invalid Italian VAT numbers
        assertFalse(validator.isValid("IT1234567890", context)); // too short
        assertFalse(validator.isValid("IT123456789012", context)); // too long
        assertFalse(validator.isValid("IT12345678A01", context)); // contains letter in number part
    }

    @Test
    @DisplayName("Should validate French VAT numbers correctly")
    void testFrenchVatNumbers() {
        // Valid French VAT numbers
        assertTrue(validator.isValid("FR12345678901", context));
        assertTrue(validator.isValid("FRAB345678901", context));
        assertTrue(validator.isValid("FRK7401234567", context));
        
        // Invalid French VAT numbers
        assertFalse(validator.isValid("FR1234567890", context)); // too short
        assertFalse(validator.isValid("FR12345678901A", context)); // too long
    }

    @Test
    @DisplayName("Should validate German VAT numbers correctly")
    void testGermanVatNumbers() {
        // Valid German VAT numbers
        assertTrue(validator.isValid("DE123456789", context));
        assertTrue(validator.isValid("DE987654321", context));
        
        // Invalid German VAT numbers
        assertFalse(validator.isValid("DE12345678", context)); // too short
        assertFalse(validator.isValid("DE1234567890", context)); // too long
        assertFalse(validator.isValid("DE12345678A", context)); // contains letter
    }

    @Test
    @DisplayName("Should validate Spanish VAT numbers correctly")
    void testSpanishVatNumbers() {
        // Valid Spanish VAT numbers
        assertTrue(validator.isValid("ESA12345674", context));
        assertTrue(validator.isValid("ESB98765432", context));
        assertTrue(validator.isValid("ESX1234567Z", context));
        
        // Invalid Spanish VAT numbers
        assertFalse(validator.isValid("ES12345678A", context)); // doesn't start with letter
        assertFalse(validator.isValid("ESA1234567", context)); // too short
        assertFalse(validator.isValid("ESA123456789", context)); // too long
    }

    @Test
    @DisplayName("Should validate Dutch VAT numbers correctly")
    void testDutchVatNumbers() {
        // Valid Dutch VAT numbers
        assertTrue(validator.isValid("NL123456789B01", context));
        assertTrue(validator.isValid("NL987654321B12", context));
        
        // Invalid Dutch VAT numbers
        assertFalse(validator.isValid("NL123456789", context)); // missing B and suffix
        assertFalse(validator.isValid("NL123456789A01", context)); // wrong letter
        assertFalse(validator.isValid("NL123456789B1", context)); // suffix too short
    }

    @Test
    @DisplayName("Should validate UK VAT numbers correctly")
    void testUKVatNumbers() {
        // Valid UK VAT numbers
        assertTrue(validator.isValid("GB123456789", context));
        assertTrue(validator.isValid("GB123456789012", context));
        assertTrue(validator.isValid("GBGD123", context));
        assertTrue(validator.isValid("GBHA456", context));
        
        // Invalid UK VAT numbers
        assertFalse(validator.isValid("GB12345678", context)); // too short
        assertFalse(validator.isValid("GBGD12", context)); // GD format too short
    }

    @Test
    @DisplayName("Should reject unsupported country codes")
    void testUnsupportedCountryCodes() {
        assertFalse(validator.isValid("US123456789", context)); // USA not supported
        assertFalse(validator.isValid("CA123456789", context)); // Canada not supported
        assertFalse(validator.isValid("JP123456789", context)); // Japan not supported
        
        // Verify custom error message is set
        verify(context, atLeast(1)).disableDefaultConstraintViolation();
        verify(context, atLeast(1)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("Should reject VAT numbers that are too short")
    void testTooShortVatNumbers() {
        assertFalse(validator.isValid("IT", context));
        assertFalse(validator.isValid("ABC", context));
        assertFalse(validator.isValid("I1", context));
        
        // Verify custom error message is set
        verify(context, atLeast(1)).disableDefaultConstraintViolation();
        verify(context, atLeast(1)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("Should handle various input formats")
    void testInputFormatHandling() {
        // Should handle spaces and hyphens
        assertTrue(validator.isValid("IT 1234 5678 901", context));
        assertTrue(validator.isValid("IT-1234-5678-901", context));
        assertTrue(validator.isValid("IT 1234-5678 901", context));
        
        // Should handle mixed case
        assertTrue(validator.isValid("it12345678901", context));
        assertTrue(validator.isValid("It12345678901", context));
        assertTrue(validator.isValid("iT12345678901", context));
    }

    @Test
    @DisplayName("Should validate multiple European countries")
    void testMultipleEuropeanCountries() {
        // Test a selection of European countries
        assertTrue(validator.isValid("BE1234567890", context)); // Belgium
        assertTrue(validator.isValid("ATU12345678", context)); // Austria
        assertTrue(validator.isValid("PT123456789", context)); // Portugal
        assertTrue(validator.isValid("FI12345678", context)); // Finland
        assertTrue(validator.isValid("SE123456789012", context)); // Sweden
        assertTrue(validator.isValid("DK12345678", context)); // Denmark
        assertTrue(validator.isValid("PL1234567890", context)); // Poland
        assertTrue(validator.isValid("EE123456789", context)); // Estonia
        assertTrue(validator.isValid("LV12345678901", context)); // Latvia
        assertTrue(validator.isValid("LT123456789", context)); // Lithuania
        assertTrue(validator.isValid("CY12345678A", context)); // Cyprus
        assertTrue(validator.isValid("MT12345678", context)); // Malta
    }
}
