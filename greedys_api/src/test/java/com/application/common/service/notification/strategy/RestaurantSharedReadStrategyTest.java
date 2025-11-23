package com.application.common.service.notification.strategy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.application.common.persistence.dao.RestaurantNotificationDAO;

/**
 * Unit tests for RestaurantSharedReadStrategy
 * 
 * Tests all scope scenarios:
 * - RESTAURANT: Mark all in restaurant as read
 * - RESTAURANT_HUB: Mark all in hub as read
 * - RESTAURANT_HUB_ALL: Admin broadcast - mark ALL as read
 * 
 * @author System
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantSharedReadStrategy Tests")
class RestaurantSharedReadStrategyTest {
    
    @Mock
    private RestaurantNotificationDAO restaurantNotificationDAO;
    
    @InjectMocks
    private RestaurantSharedReadStrategy strategy;
    
    private SharedReadParams params;
    private Instant now;
    
    @BeforeEach
    void setup() {
        now = Instant.now();
        params = SharedReadParams.builder()
            .readByUserId(100L)
            .readAt(now)
            .build();
    }
    
    // ========== RESTAURANT SCOPE TESTS ==========
    
    @Test
    @DisplayName("RESTAURANT scope: Should mark all restaurant notifications as read")
    void testRestaurantScopeMarksAllRestaurantAsRead() {
        // Arrange
        Long restaurantId = 5L;
        params.setNotificationId(1L);
        params.setRestaurantId(restaurantId);
        params.setScope(SharedReadScope.RESTAURANT.name());
        
        when(restaurantNotificationDAO.markAsReadRestaurant(
            eq(restaurantId),
            eq(100L),
            eq(now)
        )).thenReturn(3);  // 3 rows updated
        
        // Act
        strategy.markAsRead(params);
        
        // Assert
        verify(restaurantNotificationDAO, times(1)).markAsReadRestaurant(
            eq(restaurantId),
            eq(100L),
            eq(now)
        );
    }
    
    @Test
    @DisplayName("RESTAURANT scope: Should throw if restaurantId missing")
    void testRestaurantScopeThrowsIfNoRestaurantId() {
        // Arrange
        params.setNotificationId(1L);
        params.setRestaurantId(null);  // ❌ Missing
        params.setScope(SharedReadScope.RESTAURANT.name());
        
        // Act & Assert
        assertThatThrownBy(() -> strategy.markAsRead(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("restaurantId");
    }
    
    // ========== RESTAURANT_HUB SCOPE TESTS ==========
    
    @Test
    @DisplayName("RESTAURANT_HUB scope: Should mark all hub notifications as read")
    void testRestaurantHubScopeMarksAllHubAsRead() {
        // Arrange
        Long hubId = 10L;
        params.setNotificationId(2L);
        params.setRestaurantUserHubId(hubId);
        params.setScope(SharedReadScope.RESTAURANT_HUB.name());
        
        when(restaurantNotificationDAO.markAsReadRestaurantHub(
            eq(hubId),
            eq(100L),
            eq(now)
        )).thenReturn(7);  // 7 rows across 3 restaurants in hub
        
        // Act
        strategy.markAsRead(params);
        
        // Assert
        verify(restaurantNotificationDAO, times(1)).markAsReadRestaurantHub(
            eq(hubId),
            eq(100L),
            eq(now)
        );
    }
    
    @Test
    @DisplayName("RESTAURANT_HUB scope: Should throw if hubId missing")
    void testRestaurantHubScopeThrowsIfNoHubId() {
        // Arrange
        params.setNotificationId(2L);
        params.setRestaurantUserHubId(null);  // ❌ Missing
        params.setScope(SharedReadScope.RESTAURANT_HUB.name());
        
        // Act & Assert
        assertThatThrownBy(() -> strategy.markAsRead(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("restaurantUserHubId");
    }
    
    // ========== RESTAURANT_HUB_ALL SCOPE TESTS ==========
    
    @Test
    @DisplayName("RESTAURANT_HUB_ALL scope: Should mark ALL notifications as read (broadcast)")
    void testRestaurantHubAllScopeMarksAllAsRead() {
        // Arrange
        Long hubId = 10L;
        params.setNotificationId(3L);
        params.setRestaurantUserHubId(hubId);
        params.setScope(SharedReadScope.RESTAURANT_HUB_ALL.name());
        
        when(restaurantNotificationDAO.markAsReadRestaurantHubAll(
            eq(hubId),
            eq(100L),
            eq(now)
        )).thenReturn(15);  // ALL 15 rows marked as read immediately
        
        // Act
        strategy.markAsRead(params);
        
        // Assert
        verify(restaurantNotificationDAO, times(1)).markAsReadRestaurantHubAll(
            eq(hubId),
            eq(100L),
            eq(now)
        );
    }
    
    // ========== UNSUPPORTED SCOPE TESTS ==========
    
    @Test
    @DisplayName("Should throw for unsupported scope")
    void testThrowsForUnsupportedScope() {
        // Arrange
        params.setNotificationId(4L);
        params.setRestaurantId(5L);
        params.setScope("INVALID_SCOPE");  // ❌ Not in SUPPORTED_SCOPES
        
        // Act & Assert
        assertThatThrownBy(() -> strategy.markAsRead(params))
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    // ========== BATCH OPERATIONS TESTS ==========
    
    @Test
    @DisplayName("Should process multiple params in batch")
    void testBatchProcessing() {
        // Arrange
        SharedReadParams param1 = SharedReadParams.builder()
            .notificationId(1L)
            .readByUserId(100L)
            .readAt(now)
            .restaurantId(5L)
            .scope(SharedReadScope.RESTAURANT.name())
            .build();
        
        SharedReadParams param2 = SharedReadParams.builder()
            .notificationId(2L)
            .readByUserId(100L)
            .readAt(now)
            .restaurantUserHubId(10L)
            .scope(SharedReadScope.RESTAURANT_HUB.name())
            .build();
        
        List<SharedReadParams> params = Arrays.asList(param1, param2);
        
        when(restaurantNotificationDAO.markAsReadRestaurant(5L, 100L, now))
            .thenReturn(3);
        when(restaurantNotificationDAO.markAsReadRestaurantHub(10L, 100L, now))
            .thenReturn(7);
        
        // Act
        strategy.markMultipleAsRead(params);
        
        // Assert
        verify(restaurantNotificationDAO, times(1)).markAsReadRestaurant(5L, 100L, now);
        verify(restaurantNotificationDAO, times(1)).markAsReadRestaurantHub(10L, 100L, now);
    }
    
    // ========== METADATA TESTS ==========
    
    @Test
    @DisplayName("Should support correct scopes")
    void testSupportedScopes() {
        // Assert
        assertThat(strategy.getSupportedScopes())
            .containsExactlyInAnyOrder(
                SharedReadScope.RESTAURANT,
                SharedReadScope.RESTAURANT_HUB,
                SharedReadScope.RESTAURANT_HUB_ALL
            );
    }
    
    @Test
    @DisplayName("Should return strategy name")
    void testGetStrategyName() {
        // Assert
        assertThat(strategy.getStrategyName())
            .isEqualTo("RestaurantSharedReadStrategy");
    }
}
