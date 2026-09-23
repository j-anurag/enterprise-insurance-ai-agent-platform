package com.claimwise.controller;

import com.claimwise.dto.PolicyAnswerResponseDto;
import com.claimwise.dto.PolicyAnswerSourceDto;
import com.claimwise.dto.PolicyQuestionRequestDto;
import com.claimwise.exception.ExternalServiceException;
import com.claimwise.exception.GlobalExceptionHandler;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.service.PolicyRagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PolicyQaControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PolicyRagService policyRagService;

    @InjectMocks
    private PolicyQaController policyQaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(policyQaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/ask - returns 200 OK with grounded answer and sources")
    void shouldReturn200WithAnswer() throws Exception {
        PolicyQuestionRequestDto request = PolicyQuestionRequestDto.builder()
                .question("Does this policy cover water damage?")
                .build();

        PolicyAnswerResponseDto mockResponse = PolicyAnswerResponseDto.builder()
                .question("Does this policy cover water damage?")
                .answer("Yes, water damage is covered up to $25,000 under section 4.")
                .policyId(1L)
                .sources(List.of(
                        PolicyAnswerSourceDto.builder()
                                .chunkId(10L)
                                .documentId(3L)
                                .chunkIndex(1)
                                .similarityScore(0.85)
                                .snippet("Water damage is covered up to $25,000...")
                                .build()
                ))
                .build();

        when(policyRagService.askQuestion(eq(1L), any(PolicyQuestionRequestDto.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/policies/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("Does this policy cover water damage?"))
                .andExpect(jsonPath("$.answer").value("Yes, water damage is covered up to $25,000 under section 4."))
                .andExpect(jsonPath("$.policyId").value(1))
                .andExpect(jsonPath("$.sources[0].chunkId").value(10))
                .andExpect(jsonPath("$.sources[0].similarityScore").value(0.85));
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/ask - blank question returns 400 Bad Request")
    void shouldReturn400WhenQuestionIsBlank() throws Exception {
        PolicyQuestionRequestDto invalidRequest = PolicyQuestionRequestDto.builder()
                .question("   ")
                .build();

        mockMvc.perform(post("/api/v1/policies/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.question").exists());
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/ask - question too short returns 400 Bad Request")
    void shouldReturn400WhenQuestionIsTooShort() throws Exception {
        PolicyQuestionRequestDto invalidRequest = PolicyQuestionRequestDto.builder()
                .question("Hi")
                .build();

        mockMvc.perform(post("/api/v1/policies/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.question").exists());
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/ask - question too long returns 400 Bad Request")
    void shouldReturn400WhenQuestionIsTooLong() throws Exception {
        String longQuestion = "a".repeat(1001);
        PolicyQuestionRequestDto invalidRequest = PolicyQuestionRequestDto.builder()
                .question(longQuestion)
                .build();

        mockMvc.perform(post("/api/v1/policies/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.question").exists());
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/ask - non-existent policy returns 404 Not Found")
    void shouldReturn404WhenPolicyNotFound() throws Exception {
        PolicyQuestionRequestDto request = PolicyQuestionRequestDto.builder()
                .question("Is flood covered?")
                .build();

        when(policyRagService.askQuestion(eq(999L), any(PolicyQuestionRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Policy not found with id: 999"));

        mockMvc.perform(post("/api/v1/policies/999/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Policy not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/ask - Groq 429 returns 429 Too Many Requests")
    void shouldReturn429WhenGroqRateLimited() throws Exception {
        PolicyQuestionRequestDto request = PolicyQuestionRequestDto.builder()
                .question("Is flood covered?")
                .build();

        when(policyRagService.askQuestion(eq(1L), any(PolicyQuestionRequestDto.class)))
                .thenThrow(new ExternalServiceException("Groq API rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(post("/api/v1/policies/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Groq API rate limit exceeded"));
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/ask - Groq 502 returns 502 Bad Gateway")
    void shouldReturn502WhenGroqFails() throws Exception {
        PolicyQuestionRequestDto request = PolicyQuestionRequestDto.builder()
                .question("Is flood covered?")
                .build();

        when(policyRagService.askQuestion(eq(1L), any(PolicyQuestionRequestDto.class)))
                .thenThrow(new ExternalServiceException("Groq service returned error: 500", HttpStatus.BAD_GATEWAY));

        mockMvc.perform(post("/api/v1/policies/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Groq service returned error: 500"));
    }
}
