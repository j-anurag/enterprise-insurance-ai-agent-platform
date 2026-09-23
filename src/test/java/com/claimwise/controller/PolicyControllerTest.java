package com.claimwise.controller;

import com.claimwise.dto.PolicyRequestDto;
import com.claimwise.dto.PolicyResponseDto;
import com.claimwise.exception.GlobalExceptionHandler;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.PolicyStatus;
import com.claimwise.model.PolicyType;
import com.claimwise.service.PolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private PolicyService policyService;

    @InjectMocks
    private PolicyController policyController;

    private PolicyResponseDto samplePolicyResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(policyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        samplePolicyResponse = PolicyResponseDto.builder()
                .id(10L)
                .policyNumber("POL-1001")
                .customerId(1L)
                .customerNumber("CUST-1001")
                .policyType(PolicyType.AUTO)
                .startDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2027, 1, 1))
                .status(PolicyStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/policies - success returns 201 Created")
    void shouldCreatePolicySuccessfully() throws Exception {
        PolicyRequestDto request = PolicyRequestDto.builder()
                .policyNumber("POL-1001")
                .customerId(1L)
                .policyType(PolicyType.AUTO)
                .startDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2027, 1, 1))
                .status(PolicyStatus.ACTIVE)
                .build();

        when(policyService.createPolicy(any(PolicyRequestDto.class))).thenReturn(samplePolicyResponse);

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.policyNumber").value("POL-1001"))
                .andExpect(jsonPath("$.policyType").value("AUTO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/policies - validation failure returns 400 Bad Request")
    void shouldReturnBadRequestWhenPolicyValidationFails() throws Exception {
        PolicyRequestDto invalidRequest = PolicyRequestDto.builder()
                .policyNumber("")
                .customerId(null)
                .policyType(null)
                .startDate(null)
                .expiryDate(null)
                .status(null)
                .build();

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.policyNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.customerId").exists());
    }

    @Test
    @DisplayName("GET /api/v1/policies - returns 200 OK with list")
    void shouldReturnAllPolicies() throws Exception {
        when(policyService.getAllPolicies()).thenReturn(List.of(samplePolicyResponse));

        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyNumber").value("POL-1001"))
                .andExpect(jsonPath("$[0].customerNumber").value("CUST-1001"));
    }

    @Test
    @DisplayName("GET /api/v1/policies/{policyNumber} - returns 200 OK when found")
    void shouldReturnPolicyByNumber() throws Exception {
        when(policyService.getPolicyByPolicyNumber("POL-1001")).thenReturn(samplePolicyResponse);

        mockMvc.perform(get("/api/v1/policies/POL-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("POL-1001"))
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/policies/{policyNumber} - returns 404 Not Found when not exists")
    void shouldReturnNotFoundWhenPolicyDoesNotExist() throws Exception {
        when(policyService.getPolicyByPolicyNumber("POL-NONEXISTENT"))
                .thenThrow(new ResourceNotFoundException("Policy not found with policy number: POL-NONEXISTENT"));

        mockMvc.perform(get("/api/v1/policies/POL-NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Policy not found with policy number: POL-NONEXISTENT"));
    }
}
