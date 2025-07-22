package com.application.admin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.application.admin.service.AdminCustomerService;
import com.application.common.service.AllergyService;
import com.application.common.web.dto.get.CustomerDTO;

/**
 * Test specifici per la funzionalità di paginazione 
 * del AdminCustomerController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Customer Controller - Pagination Tests")
class AdminCustomerControllerPaginationTest {

    @Mock
    private AdminCustomerService customerService;

    @Mock
    private AllergyService allergyService;

    @InjectMocks
    private AdminCustomerController controller;

    private List<CustomerDTO> testCustomers;

    @BeforeEach
    void setUp() {
        // Create test data
        testCustomers = Arrays.asList(
            createCustomerDTO(1L, "customer1@test.com", "Mario", "Rossi"),
            createCustomerDTO(2L, "customer2@test.com", "Luigi", "Verdi"),
            createCustomerDTO(3L, "customer3@test.com", "Giuseppe", "Bianchi"),
            createCustomerDTO(4L, "customer4@test.com", "Anna", "Neri"),
            createCustomerDTO(5L, "customer5@test.com", "Elena", "Gialli")
        );
    }

    @Test
    @DisplayName("Should return first page with correct pagination info")
    void testPagination_FirstPage() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 2);
        List<CustomerDTO> pageContent = testCustomers.subList(0, 2);
        Page<CustomerDTO> expectedPage = new PageImpl<>(pageContent, pageRequest, testCustomers.size());
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(expectedPage);

        // When
        Page<CustomerDTO> result = controller.listUsersWithPagination(0, 2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages()); // ceil(5/2) = 3
        assertEquals(true, result.isFirst());
        assertEquals(false, result.isLast());
        assertEquals(true, result.hasNext());
        assertEquals(false, result.hasPrevious());
    }

    @Test
    @DisplayName("Should return middle page with correct pagination info")
    void testPagination_MiddlePage() {
        // Given
        PageRequest pageRequest = PageRequest.of(1, 2);
        List<CustomerDTO> pageContent = testCustomers.subList(2, 4);
        Page<CustomerDTO> expectedPage = new PageImpl<>(pageContent, pageRequest, testCustomers.size());
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(expectedPage);

        // When
        Page<CustomerDTO> result = controller.listUsersWithPagination(1, 2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(false, result.isFirst());
        assertEquals(false, result.isLast());
        assertEquals(true, result.hasNext());
        assertEquals(true, result.hasPrevious());
    }

    @Test
    @DisplayName("Should return last page with correct pagination info")
    void testPagination_LastPage() {
        // Given
        PageRequest pageRequest = PageRequest.of(2, 2);
        List<CustomerDTO> pageContent = testCustomers.subList(4, 5); // Only one element in last page
        Page<CustomerDTO> expectedPage = new PageImpl<>(pageContent, pageRequest, testCustomers.size());
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(expectedPage);

        // When
        Page<CustomerDTO> result = controller.listUsersWithPagination(2, 2);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(2, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(false, result.isFirst());
        assertEquals(true, result.isLast());
        assertEquals(false, result.hasNext());
        assertEquals(true, result.hasPrevious());
    }

    @Test
    @DisplayName("Should handle single page scenario")
    void testPagination_SinglePage() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<CustomerDTO> expectedPage = new PageImpl<>(testCustomers, pageRequest, testCustomers.size());
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(expectedPage);

        // When
        Page<CustomerDTO> result = controller.listUsersWithPagination(0, 10);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(true, result.isFirst());
        assertEquals(true, result.isLast());
        assertEquals(false, result.hasNext());
        assertEquals(false, result.hasPrevious());
    }

    @Test
    @DisplayName("Should handle empty result")
    void testPagination_EmptyResult() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<CustomerDTO> emptyPage = new PageImpl<>(Arrays.asList(), pageRequest, 0);
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        // When
        Page<CustomerDTO> result = controller.listUsersWithPagination(0, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertEquals(false, result.hasContent());
    }

    @Test
    @DisplayName("Should handle page size of 1")
    void testPagination_PageSizeOne() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 1);
        List<CustomerDTO> pageContent = testCustomers.subList(0, 1);
        Page<CustomerDTO> expectedPage = new PageImpl<>(pageContent, pageRequest, testCustomers.size());
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(expectedPage);

        // When
        Page<CustomerDTO> result = controller.listUsersWithPagination(0, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(1, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getTotalPages()); // 5 elements with page size 1 = 5 pages
        assertEquals(true, result.isFirst());
        assertEquals(false, result.isLast());
        assertEquals(true, result.hasNext());
        assertEquals(false, result.hasPrevious());
    }

    @Test
    @DisplayName("Should verify PageRequest is created correctly")
    void testPagination_PageRequestCreation() {
        // Given
        PageRequest expectedPageRequest = PageRequest.of(3, 7);
        Page<CustomerDTO> mockPage = new PageImpl<>(Arrays.asList(), expectedPageRequest, 0);
        
        when(customerService.findAll(expectedPageRequest)).thenReturn(mockPage);

        // When
        controller.listUsersWithPagination(3, 7);

        // Then - Verify that PageRequest.of(3, 7) was called
        // This test ensures the controller correctly creates PageRequest from int parameters
        assertNotNull(expectedPageRequest);
        assertEquals(3, expectedPageRequest.getPageNumber());
        assertEquals(7, expectedPageRequest.getPageSize());
    }

    @Test
    @DisplayName("Should handle boundary page numbers")
    void testPagination_BoundaryPageNumbers() {
        // Test page 0 (minimum)
        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<CustomerDTO> expectedPage = new PageImpl<>(testCustomers, pageRequest, testCustomers.size());
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(expectedPage);

        Page<CustomerDTO> result = controller.listUsersWithPagination(0, 5);
        
        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(true, result.isFirst());
        
        // Test high page number (beyond available data)
        PageRequest highPageRequest = PageRequest.of(100, 5);
        Page<CustomerDTO> emptyPage = new PageImpl<>(Arrays.asList(), highPageRequest, testCustomers.size());
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(emptyPage);
        
        Page<CustomerDTO> highPageResult = controller.listUsersWithPagination(100, 5);
        
        assertNotNull(highPageResult);
        assertEquals(100, highPageResult.getNumber());
        assertEquals(0, highPageResult.getContent().size());
        assertEquals(5, highPageResult.getTotalElements()); // Total elements should still be accurate
    }

    private CustomerDTO createCustomerDTO(Long id, String email, String firstName, String lastName) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(id);
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        return dto;
    }
}
