package com.application.restaurant.service.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.application.common.spring.TwilioConfig;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.RestaurantVerificationDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantVerification;
import com.application.restaurant.web.dto.verification.VerificationRequestDTO;
import com.application.restaurant.web.dto.verification.VerificationResponseDTO;
import com.application.restaurant.web.dto.verification.VerificationStatus;

/**
 * Unit tests for RestaurantTwilioVerificationService
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class RestaurantTwilioVerificationServiceTest {

    @Mock
    private TwilioConfig twilioConfig;

    @Mock
    private RestaurantDAO restaurantDAO;

    @Mock
    private RestaurantVerificationDAO verificationDAO;

    @InjectMocks
    private RestaurantTwilioVerificationService verificationService;

    private Restaurant testRestaurant;
    private VerificationRequestDTO testRequest;

    @BeforeEach
    void setUp() {
        testRestaurant = Restaurant.builder()
            .id(1L)
            .name("Test Restaurant")
            .phoneNumber("+393331234567")
            .phoneVerified(false)
            .build();

        testRequest = VerificationRequestDTO.builder()
            .restaurantId(1L)
            .phoneNumber("3331234567")
            .build();

        // Setup default mock responses
        when(twilioConfig.getVerifyServiceSid()).thenReturn("test-service-sid");
        when(twilioConfig.getVerificationExpiryMinutes()).thenReturn(10);
        when(twilioConfig.getMaxVerificationAttempts()).thenReturn(3);
    }

    @Test
    void testValidateAndFormatPhoneNumber_ValidItalianNumber() {
        // Test with various Italian phone number formats
        String result1 = verificationService.validateAndFormatPhoneNumber("3331234567");
        assertEquals("+393331234567", result1);

        String result2 = verificationService.validateAndFormatPhoneNumber("0331234567");
        assertEquals("+39331234567", result2);

        String result3 = verificationService.validateAndFormatPhoneNumber("+393331234567");
        assertEquals("+393331234567", result3);

        String result4 = verificationService.validateAndFormatPhoneNumber("39 333 123 4567");
        assertEquals("+393331234567", result4);
    }

    @Test
    void testValidateAndFormatPhoneNumber_InvalidNumber() {
        assertThrows(IllegalArgumentException.class, () -> 
            verificationService.validateAndFormatPhoneNumber("123"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            verificationService.validateAndFormatPhoneNumber("abc123"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            verificationService.validateAndFormatPhoneNumber(""));
        
        assertThrows(IllegalArgumentException.class, () -> 
            verificationService.validateAndFormatPhoneNumber(null));
    }

    @Test
    void testValidateRestaurant_RestaurantExists() {
        when(restaurantDAO.findById(1L)).thenReturn(Optional.of(testRestaurant));
        
        Restaurant result = verificationService.validateRestaurant(1L);
        
        assertEquals(testRestaurant, result);
        verify(restaurantDAO).findById(1L);
    }

    @Test
    void testValidateRestaurant_RestaurantNotFound() {
        when(restaurantDAO.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> 
            verificationService.validateRestaurant(1L));
    }

    @Test
    void testMaskPhoneNumber() {
        String masked1 = verificationService.maskPhoneNumber("+393331234567");
        assertEquals("+39****4567", masked1);

        String masked2 = verificationService.maskPhoneNumber("+39123456789");
        assertEquals("+39*****6789", masked2);

        String masked3 = verificationService.maskPhoneNumber("short");
        assertEquals("short", masked3); // Too short to mask
    }

    @Test
    void testIsVerificationStillValid() {
        LocalDateTime now = LocalDateTime.now();
        
        // Valid verification
        RestaurantVerification validVerification = RestaurantVerification.builder()
            .status(VerificationStatus.PENDING)
            .expiresAt(now.plusMinutes(5))
            .attempts(1)
            .build();
        
        when(twilioConfig.getMaxVerificationAttempts()).thenReturn(3);
        assertTrue(verificationService.isVerificationStillValid(validVerification));

        // Expired verification
        RestaurantVerification expiredVerification = RestaurantVerification.builder()
            .status(VerificationStatus.PENDING)
            .expiresAt(now.minusMinutes(1))
            .attempts(1)
            .build();
        
        assertFalse(verificationService.isVerificationStillValid(expiredVerification));

        // Max attempts reached
        RestaurantVerification maxAttemptsVerification = RestaurantVerification.builder()
            .status(VerificationStatus.PENDING)
            .expiresAt(now.plusMinutes(5))
            .attempts(3)
            .build();
        
        assertFalse(verificationService.isVerificationStillValid(maxAttemptsVerification));

        // Already verified
        RestaurantVerification verifiedVerification = RestaurantVerification.builder()
            .status(VerificationStatus.VERIFIED)
            .expiresAt(now.plusMinutes(5))
            .attempts(1)
            .build();
        
        assertFalse(verificationService.isVerificationStillValid(verifiedVerification));
    }

    @Test
    void testGetVerificationStatus_NoVerification() {
        when(restaurantDAO.findById(1L)).thenReturn(Optional.of(testRestaurant));
        when(verificationDAO.findActiveVerificationByRestaurant(1L)).thenReturn(Optional.empty());
        
        VerificationResponseDTO response = verificationService.getVerificationStatus(1L);
        
        assertTrue(response.isSuccess());
        assertEquals(VerificationStatus.NOT_STARTED, response.getStatus());
        assertEquals("No verification found", response.getMessage());
    }

    @Test
    void testGetVerificationStatus_ActiveVerification() {
        LocalDateTime now = LocalDateTime.now();
        RestaurantVerification verification = RestaurantVerification.builder()
            .id(1L)
            .restaurant(testRestaurant)
            .phoneNumber("+393331234567")
            .verificationSid("VE123456")
            .status(VerificationStatus.PENDING)
            .createdAt(now.minusMinutes(2))
            .expiresAt(now.plusMinutes(8))
            .attempts(1)
            .build();

        when(restaurantDAO.findById(1L)).thenReturn(Optional.of(testRestaurant));
        when(verificationDAO.findActiveVerificationByRestaurant(1L)).thenReturn(Optional.of(verification));
        when(twilioConfig.getMaxVerificationAttempts()).thenReturn(3);
        
        VerificationResponseDTO response = verificationService.getVerificationStatus(1L);
        
        assertTrue(response.isSuccess());
        assertEquals(VerificationStatus.PENDING, response.getStatus());
        assertEquals("VE123456", response.getVerificationSid());
        assertEquals("+39****4567", response.getPhoneNumber());
        assertEquals(1, response.getAttempts());
        assertEquals(2, response.getAttemptsRemaining());
    }

    @Test
    void testCancelVerification_Success() {
        RestaurantVerification verification = RestaurantVerification.builder()
            .id(1L)
            .restaurant(testRestaurant)
            .status(VerificationStatus.PENDING)
            .verificationSid("VE123456")
            .build();

        when(restaurantDAO.findById(1L)).thenReturn(Optional.of(testRestaurant));
        when(verificationDAO.findActiveVerificationByRestaurant(1L)).thenReturn(Optional.of(verification));
        when(verificationDAO.save(any(RestaurantVerification.class))).thenReturn(verification);
        
        VerificationResponseDTO response = verificationService.cancelVerification(1L);
        
        assertTrue(response.isSuccess());
        assertEquals(VerificationStatus.CANCELLED, response.getStatus());
        assertEquals("Verification cancelled successfully", response.getMessage());
        verify(verificationDAO).save(verification);
        assertEquals(VerificationStatus.CANCELLED, verification.getStatus());
    }

    @Test
    void testCancelVerification_NoActiveVerification() {
        when(restaurantDAO.findById(1L)).thenReturn(Optional.of(testRestaurant));
        when(verificationDAO.findActiveVerificationByRestaurant(1L)).thenReturn(Optional.empty());
        
        VerificationResponseDTO response = verificationService.cancelVerification(1L);
        
        assertFalse(response.isSuccess());
        assertEquals(VerificationStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("Failed to cancel verification"));
    }
}
