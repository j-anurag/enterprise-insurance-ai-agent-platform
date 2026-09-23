package com.claimwise.controller;

import com.claimwise.dto.CustomerRequestDto;
import com.claimwise.dto.CustomerResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.GlobalExceptionHandler;
import com.claimwise.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers - success returns 201 Created")
    void shouldCreateCustomerSuccessfully() throws Exception {
        CustomerRequestDto request = CustomerRequestDto.builder()
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        CustomerResponseDto response = CustomerResponseDto.builder()
                .id(1L)
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(customerService.createCustomer(any(CustomerRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerNumber").value("CUST-1001"))
                .andExpect(jsonPath("$.fullName").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/customers - validation failure returns 400 Bad Request")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        CustomerRequestDto invalidRequest = CustomerRequestDto.builder()
                .customerNumber("")
                .fullName("")
                .email("invalid-email-format")
                .build();

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.customerNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/customers - duplicate customer returns 409 Conflict")
    void shouldReturnConflictWhenDuplicateCustomer() throws Exception {
        CustomerRequestDto request = CustomerRequestDto.builder()
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        when(customerService.createCustomer(any(CustomerRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Customer with number CUST-1001 already exists"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Customer with number CUST-1001 already exists"));
    }

    @Test
    @DisplayName("GET /api/v1/customers - returns 200 OK with list")
    void shouldReturnAllCustomers() throws Exception {
        CustomerResponseDto customer = CustomerResponseDto.builder()
                .id(1L)
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(customerService.getAllCustomers()).thenReturn(List.of(customer));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerNumber").value("CUST-1001"))
                .andExpect(jsonPath("$[0].fullName").value("Alice Smith"));
    }
}
