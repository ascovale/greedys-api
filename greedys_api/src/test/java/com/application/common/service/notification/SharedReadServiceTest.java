package com.application.common.service.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.application.common.service.notification.strategy.SharedReadParams;
import com.application.common.service.notification.strategy.SharedReadScope;
import com.application.common.service.notification.strategy.SharedReadStrategy;
import com.application.common.service.notification.strategy.SharedReadStrategyFactory;

/**
 * Integration tests for SharedReadService orchestrator
 * 
 * Tests:
 * - Strategy selection via factory
 * - Scope validation
 * - Early exit for NONE scope
 * - Proper delegation to strategy
 * - Error handling
 * 
 * @author System
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SharedReadService Integration Tests")
class SharedReadServiceTest {
    
    @Mock
    private SharedReadStrategyFactory strategyFactory;
    
    @Mock
    private SharedReadStrategy mockStrategy;
    
    @InjectMocks
    private SharedReadService sharedReadService;
    
    private Instant now;
    
    @BeforeEach
    void setup() {
        now = Instant.now();
    }
    
    // ========== NONE SCOPE (Early Exit) ==========
    
    @Test
    @DisplayName("NONE scope: Should return early without calling strategy")
    void testNoneScopeReturnsEarly() {
        // Arrange
        Long notificationId = 1L;
        SharedReadParams params = SharedReadParams.builder()
            .readByUserId(100L)
            .readAt(now)
            .build();
        
        // Act
        sharedReadService.markAsRead(
            notificationId,
            "RESTAURANT",
            SharedReadScope.NONE,
            params
        );
        
        // Assert: Factory should NOT be called
        verifyNoInteractions(strategyFactory);
        verifyNoInteractions(mockStrategy);
    }
    
    // ========== SHARED READ SCOPES ==========
    
    @Test
    @DisplayName("RESTAURANT scope: Should select strategy and delegate")
    void testRestaurantScopeDelegates() {
        // Arrange
        Long notificationId = 1L;
        SharedReadParams params = SharedReadParams.builder()
            .notificationId(notificationId)
            .readByUserId(100L)
            .readAt(now)
            .restaurantId(5L)
            .build();
        
        when(strategyFactory.getStrategy("RESTAURANT"))
            .thenReturn(mockStrategy);
        when(mockStrategy.supportsScope(SharedReadScope.RESTAURANT))
            .thenReturn(true);
        
        // Act
        sharedReadService.markAsRead(
            notificationId,
            "RESTAURANT",
            SharedReadScope.RESTAURANT,
            params
        );
        
        // Assert
        verify(strategyFactory, times(1)).getStrategy("RESTAURANT");
        verify(mockStrategy, times(1)).markAsRead(any(SharedReadParams.class));
    }
    
    @Test
    @DisplayName("RESTAURANT_HUB scope: Should select strategy and delegate")
    void testRestaurantHubScopeDelegates() {
        // Arrange
        Long notificationId = 2L;
        SharedReadParams params = SharedReadParams.builder()
            .notificationId(notificationId)
            .readByUserId(100L)
            .readAt(now)
            .restaurantUserHubId(10L)
            .build();
        
        when(strategyFactory.getStrategy("RESTAURANT"))
            .thenReturn(mockStrategy);
        when(mockStrategy.supportsScope(SharedReadScope.RESTAURANT_HUB))
            .thenReturn(true);
        
        // Act
        sharedReadService.markAsRead(
            notificationId,
            "RESTAURANT",
            SharedReadScope.RESTAURANT_HUB,
            params
        );
        
        // Assert
        verify(strategyFactory, times(1)).getStrategy("RESTAURANT");
        verify(mockStrategy, times(1)).markAsRead(any(SharedReadParams.class));
    }
    
    @Test
    @DisplayName("AGENCY scope: Should select agency strategy")
    void testAgencyScopeSelectsAgencyStrategy() {
        // Arrange
        Long notificationId = 3L;
        SharedReadParams params = SharedReadParams.builder()
            .notificationId(notificationId)
            .readByUserId(100L)
            .readAt(now)
            .agencyId(2L)
            .build();
        
        when(strategyFactory.getStrategy("AGENCY"))
            .thenReturn(mockStrategy);
        when(mockStrategy.supportsScope(SharedReadScope.RESTAURANT))  // Maps to AGENCY
            .thenReturn(true);
        
        // Act
        sharedReadService.markAsRead(
            notificationId,
            "AGENCY",
            SharedReadScope.RESTAURANT,  // Reused enum for AGENCY
            params
        );
        
        // Assert
        verify(strategyFactory, times(1)).getStrategy("AGENCY");
    }
    
    // ========== VALIDATION TESTS ==========
    
    @Test
    @DisplayName("Should throw if notificationId is null")
    void testThrowsIfNotificationIdNull() {
        // Arrange
        SharedReadParams params = SharedReadParams.builder()
            .readByUserId(100L)
            .readAt(now)
            .build();
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sharedReadService.markAsRead(
                null,  // ❌ Null
                "RESTAURANT",
                SharedReadScope.RESTAURANT,
                params
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("notificationId");
    }
    
    @Test
    @DisplayName("Should throw if entityType is null")
    void testThrowsIfEntityTypeNull() {
        // Arrange
        SharedReadParams params = SharedReadParams.builder()
            .readByUserId(100L)
            .readAt(now)
            .build();
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sharedReadService.markAsRead(
                1L,
                null,  // ❌ Null
                SharedReadScope.RESTAURANT,
                params
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("entityType");
    }
    
    @Test
    @DisplayName("Should throw if params is null")
    void testThrowsIfParamsNull() {
        // Act & Assert
        assertThatThrownBy(() -> 
            sharedReadService.markAsRead(
                1L,
                "RESTAURANT",
                SharedReadScope.RESTAURANT,
                (SharedReadParams) null  // ✅ Cast to disambiguate
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("params");
    }
    
    @Test
    @DisplayName("Should throw if strategy doesn't support scope")
    void testThrowsIfScopeNotSupported() {
        // Arrange
        SharedReadParams params = SharedReadParams.builder()
            .readByUserId(100L)
            .readAt(now)
            .restaurantId(5L)
            .build();
        
        when(strategyFactory.getStrategy("RESTAURANT"))
            .thenReturn(mockStrategy);
        when(mockStrategy.supportsScope(SharedReadScope.RESTAURANT))
            .thenReturn(false);  // ❌ Not supported
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sharedReadService.markAsRead(
                1L,
                "RESTAURANT",
                SharedReadScope.RESTAURANT,
                params
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("does not support scope");
    }
    
    // ========== NULL SCOPE DEFAULTS ==========
    
    @Test
    @DisplayName("Null scope should default to NONE")
    void testNullScopeDefaults() {
        // Arrange
        SharedReadParams params = SharedReadParams.builder()
            .readByUserId(100L)
            .readAt(now)
            .build();
        
        // Act
        sharedReadService.markAsRead(
            1L,
            "RESTAURANT",
            null,  // Will default to NONE
            params
        );
        
        // Assert: Factory should NOT be called (NONE scope skipped)
        verifyNoInteractions(strategyFactory);
    }
    
    // ========== HELPER METHOD TESTS ==========
    
    @Test
    @DisplayName("requiresSharedRead: Should return false for NONE")
    void testRequiresSharedReadFalseForNone() {
        assertThat(sharedReadService.requiresSharedRead(SharedReadScope.NONE))
            .isFalse();
    }
    
    @Test
    @DisplayName("requiresSharedRead: Should return true for RESTAURANT")
    void testRequiresSharedReadTrueForRestaurant() {
        assertThat(sharedReadService.requiresSharedRead(SharedReadScope.RESTAURANT))
            .isTrue();
    }
    
    @Test
    @DisplayName("requiresSharedRead: Should return false for null")
    void testRequiresSharedReadFalseForNull() {
        assertThat(sharedReadService.requiresSharedRead(null))
            .isFalse();
    }
    
    @Test
    @DisplayName("supportsEntityType: Should check factory")
    void testSupportsEntityType() {
        // Arrange
        when(strategyFactory.supportsEntityType("RESTAURANT"))
            .thenReturn(true);
        
        // Act
        boolean supports = sharedReadService.supportsEntityType("RESTAURANT");
        
        // Assert
        assertThat(supports).isTrue();
        verify(strategyFactory, times(1)).supportsEntityType("RESTAURANT");
    }
}
