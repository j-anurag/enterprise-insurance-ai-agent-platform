package com.claimwise.controller;

import com.claimwise.dto.ClaimRequestDto;
import com.claimwise.dto.ClaimResponseDto;
import com.claimwise.exception.GlobalExceptionHandler;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.ClaimStatus;
import com.claimwise.service.ClaimService;
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
class ClaimControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ClaimService claimService;

    @InjectMocks
    private ClaimController claimController;

    private ClaimResponseDto sampleClaimResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(claimController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleClaimResponse = ClaimResponseDto.builder()
                .id(100L)
                .claimNumber("CLM-1001")
                .policyId(10L)
                .policyNumber("POL-1001")
                .description("Accident damage repair claim")
                .incidentDate(LocalDate.of(2026, 2, 15))
                .status(ClaimStatus.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/claims - success returns 201 Created")
    void shouldCreateClaimSuccessfully() throws Exception {
        ClaimRequestDto request = ClaimRequestDto.builder()
                .claimNumber("CLM-1001")
                .policyId(10L)
                .description("Accident damage repair claim")
                .incidentDate(LocalDate.of(2026, 2, 15))
                .status(ClaimStatus.SUBMITTED)
                .build();

        when(claimService.createClaim(any(ClaimRequestDto.class))).thenReturn(sampleClaimResponse);

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.claimNumber").value("CLM-1001"))
                .andExpect(jsonPath("$.policyId").value(10))
                .andExpect(jsonPath("$.policyNumber").value("POL-1001"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /api/v1/claims - validation failure returns 400 Bad Request")
    void shouldReturnBadRequestWhenClaimValidationFails() throws Exception {
        ClaimRequestDto invalidRequest = ClaimRequestDto.builder()
                .claimNumber("")
                .policyId(null)
                .description("")
                .incidentDate(null)
                .status(null)
                .build();

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.claimNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.policyId").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    @DisplayName("GET /api/v1/claims - returns 200 OK with list")
    void shouldReturnAllClaims() throws Exception {
        when(claimService.getAllClaims()).thenReturn(List.of(sampleClaimResponse));

        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimNumber").value("CLM-1001"))
                .andExpect(jsonPath("$[0].description").value("Accident damage repair claim"));
    }

    @Test
    @DisplayName("GET /api/v1/claims/policy/{policyId} - returns 200 OK with claims for policy")
    void shouldReturnClaimsByPolicyId() throws Exception {
        when(claimService.getClaimsByPolicyId(10L)).thenReturn(List.of(sampleClaimResponse));

        mockMvc.perform(get("/api/v1/claims/policy/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimNumber").value("CLM-1001"))
                .andExpect(jsonPath("$[0].policyId").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/claims/policy/{policyId} - returns 404 Not Found when policy doesn't exist")
    void shouldReturnNotFoundWhenPolicyDoesNotExist() throws Exception {
        when(claimService.getClaimsByPolicyId(999L))
                .thenThrow(new ResourceNotFoundException("Policy not found with id: 999"));

        mockMvc.perform(get("/api/v1/claims/policy/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Policy not found with id: 999"));
    }
}
