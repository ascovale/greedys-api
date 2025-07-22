package com.application.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.application.admin.service.AdminCustomerService;
import com.application.common.service.AllergyService;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.dto.post.NewAllergyDTO;
import com.application.customer.model.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test unitari per AdminCustomerController
 * Testa tutte le funzionalità principali del controller includendo:
 * - Paginazione dei customer
 * - Gestione allergie
 * - Blocco/Sblocco customer
 * - Gestione ruoli e privilegi
 */
@DisplayName("Admin Customer Controller Unit Tests")
class AdminCustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminCustomerService customerService;

    @Mock
    private AllergyService allergyService;

    private ObjectMapper objectMapper;
    private AdminCustomerController controller;

    private CustomerDTO customerDTO1;
    private CustomerDTO customerDTO2;
    private CustomerDTO customerDTO3;
    private NewAllergyDTO allergyDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AdminCustomerController(customerService, allergyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        // Setup test data
        customerDTO1 = new CustomerDTO();
        customerDTO1.setId(1L);
        customerDTO1.setEmail("customer1@test.com");
        customerDTO1.setFirstName("Mario");
        customerDTO1.setLastName("Rossi");

        customerDTO2 = new CustomerDTO();
        customerDTO2.setId(2L);
        customerDTO2.setEmail("customer2@test.com");
        customerDTO2.setFirstName("Luigi");
        customerDTO2.setLastName("Verdi");

        customerDTO3 = new CustomerDTO();
        customerDTO3.setId(3L);
        customerDTO3.setEmail("customer3@test.com");
        customerDTO3.setFirstName("Giuseppe");
        customerDTO3.setLastName("Bianchi");

        allergyDTO = new NewAllergyDTO();
        allergyDTO.setName("Lactose");
        allergyDTO.setDescription("Lactose intolerance");
    }

    @Test
    @DisplayName("Should return paginated customers - First page")
    void testListUsersWithPagination_FirstPage() throws Exception {
        // Given
        List<CustomerDTO> customers = Arrays.asList(customerDTO1, customerDTO2);
        Page<CustomerDTO> customerPage = new PageImpl<>(customers, PageRequest.of(0, 2), 3);
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(customerPage);

        // When & Then
        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "0")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].email").value("customer1@test.com"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].email").value("customer2@test.com"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        // Verify service interaction
        verify(customerService, times(1)).findAll(eq(PageRequest.of(0, 2)));
    }

    @Test
    @DisplayName("Should return paginated customers - Second page")
    void testListUsersWithPagination_SecondPage() throws Exception {
        // Given
        List<CustomerDTO> customers = Arrays.asList(customerDTO3);
        Page<CustomerDTO> customerPage = new PageImpl<>(customers, PageRequest.of(1, 2), 3);
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(customerPage);

        // When & Then
        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "1")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));

        verify(customerService, times(1)).findAll(eq(PageRequest.of(1, 2)));
    }

    @Test
    @DisplayName("Should return empty page when no customers found")
    void testListUsersWithPagination_EmptyPage() throws Exception {
        // Given
        Page<CustomerDTO> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("Should handle large page size")
    void testListUsersWithPagination_LargePageSize() throws Exception {
        // Given
        List<CustomerDTO> customers = Arrays.asList(customerDTO1, customerDTO2, customerDTO3);
        Page<CustomerDTO> customerPage = new PageImpl<>(customers, PageRequest.of(0, 100), 3);
        
        when(customerService.findAll(any(PageRequest.class))).thenReturn(customerPage);

        // When & Then
        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "0")
                .param("size", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    @DisplayName("Should verify correct PageRequest parameters are passed")
    void testListUsersWithPagination_VerifyPageRequestParameters() throws Exception {
        // Given
        Page<CustomerDTO> customerPage = new PageImpl<>(Arrays.asList(customerDTO1), PageRequest.of(5, 20), 100);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(customerPage);

        // When
        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "5")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then - Verify exact PageRequest was created
        verify(customerService, times(1)).findAll(eq(PageRequest.of(5, 20)));
    }

    @Test
    @DisplayName("Should block customer successfully")
    void testBlockCustomer() throws Exception {
        // Given
        doNothing().when(customerService).updateCustomerStatus(anyLong(), eq(Customer.Status.BLOCKED));

        // When & Then
        mockMvc.perform(put("/admin/customer/1/block")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User blocked successfully"));

        verify(customerService, times(1)).updateCustomerStatus(1L, Customer.Status.BLOCKED);
    }

    @Test
    @DisplayName("Should enable customer successfully")
    void testEnableCustomer() throws Exception {
        // Given
        doNothing().when(customerService).updateCustomerStatus(anyLong(), eq(Customer.Status.ENABLED));

        // When & Then
        mockMvc.perform(put("/admin/customer/1/enable")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User enabled successfully"));

        verify(customerService, times(1)).updateCustomerStatus(1L, Customer.Status.ENABLED);
    }

    @Test
    @DisplayName("Should create allergy successfully")
    void testCreateAllergy() throws Exception {
        // Given
        doNothing().when(allergyService).createAllergy(any(NewAllergyDTO.class));

        // When & Then
        mockMvc.perform(post("/admin/customer/allergy/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(allergyDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Allergy created successfully"));

        verify(allergyService, times(1)).createAllergy(any(NewAllergyDTO.class));
    }

    @Test
    @DisplayName("Should delete allergy successfully")
    void testDeleteAllergy() throws Exception {
        // Given
        doNothing().when(allergyService).deleteAllergy(anyLong());

        // When & Then
        mockMvc.perform(delete("/admin/customer/allergy/1/delete")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Allergy deleted successfully"));

        verify(allergyService, times(1)).deleteAllergy(1L);
    }

    @Test
    @DisplayName("Should modify allergy successfully")
    void testModifyAllergy() throws Exception {
        // Given
        doNothing().when(allergyService).modifyAllergy(anyLong(), any(NewAllergyDTO.class));

        // When & Then
        mockMvc.perform(put("/admin/customer/allergy/1/modify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(allergyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Allergy modified successfully"));

        verify(allergyService, times(1)).modifyAllergy(eq(1L), any(NewAllergyDTO.class));
    }

    @Test
    @DisplayName("Should add role to customer successfully")
    void testAddRoleToCustomer() throws Exception {
        // Given
        doNothing().when(customerService).addRoleToCustomer(anyLong(), anyString());

        // When & Then
        mockMvc.perform(put("/admin/customer/1/add_role")
                .param("role", "CUSTOMER_PREMIUM")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role added successfully"));

        verify(customerService, times(1)).addRoleToCustomer(1L, "CUSTOMER_PREMIUM");
    }

    @Test
    @DisplayName("Should remove role from customer successfully")
    void testRemoveRoleFromCustomer() throws Exception {
        // Given
        doNothing().when(customerService).removeRoleFromCustomer(anyLong(), anyString());

        // When & Then
        mockMvc.perform(put("/admin/customer/1/remove_role")
                .param("role", "CUSTOMER_PREMIUM")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role removed successfully"));

        verify(customerService, times(1)).removeRoleFromCustomer(1L, "CUSTOMER_PREMIUM");
    }

    @Test
    @DisplayName("Should add privilege to role successfully")
    void testAddPrivilegeToRole() throws Exception {
        // Given
        doNothing().when(customerService).addPrivilegeToRole(anyString(), anyString());

        // When & Then
        mockMvc.perform(put("/admin/customer/role/CUSTOMER_PREMIUM/add_permission")
                .param("permission", "PRIVILEGE_SPECIAL_DISCOUNT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Permission added successfully"));

        verify(customerService, times(1)).addPrivilegeToRole("CUSTOMER_PREMIUM", "PRIVILEGE_SPECIAL_DISCOUNT");
    }

    @Test
    @DisplayName("Should remove privilege from role successfully")
    void testRemovePrivilegeFromRole() throws Exception {
        // Given
        doNothing().when(customerService).removePrivilegeFromRole(anyString(), anyString());

        // When & Then
        mockMvc.perform(put("/admin/customer/role/CUSTOMER_PREMIUM/remove_permission")
                .param("permission", "PRIVILEGE_SPECIAL_DISCOUNT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Permission removed successfully"));

        verify(customerService, times(1)).removePrivilegeFromRole("CUSTOMER_PREMIUM", "PRIVILEGE_SPECIAL_DISCOUNT");
    }

    @Test
    @DisplayName("Should validate pagination parameters are numbers")
    void testPaginationEndpoint_ValidateParameters() throws Exception {
        // Given
        Page<CustomerDTO> customerPage = new PageImpl<>(Arrays.asList(customerDTO1), PageRequest.of(0, 10), 1);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(customerPage);

        // Test with valid numbers
        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify correct parsing and call
        verify(customerService, times(1)).findAll(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should handle pagination edge cases")
    void testPaginationEndpoint_EdgeCases() throws Exception {
        // Test page 0 size 1 (minimum values)
        Page<CustomerDTO> singlePage = new PageImpl<>(Arrays.asList(customerDTO1), PageRequest.of(0, 1), 1);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(singlePage);

        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "0")
                .param("size", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(customerService, times(1)).findAll(PageRequest.of(0, 1));
    }

    @Test
    @DisplayName("Should test pagination logic consistency")
    void testPaginationLogic() throws Exception {
        // Test that PageRequest.of() is called with correct parameters from controller
        // This ensures the controller properly creates the PageRequest object
        
        // Given - setup mock to capture the exact PageRequest parameter
        Page<CustomerDTO> mockPage = new PageImpl<>(Arrays.asList(), PageRequest.of(3, 15), 0);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(mockPage);

        // When
        mockMvc.perform(get("/admin/customer/customers/page")
                .param("page", "3")
                .param("size", "15"))
                .andExpect(status().isOk());

        // Then - verify the exact PageRequest was created correctly
        verify(customerService, times(1)).findAll(eq(PageRequest.of(3, 15)));
    }
}
