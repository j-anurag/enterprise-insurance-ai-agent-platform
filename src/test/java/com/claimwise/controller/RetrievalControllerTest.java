package com.claimwise.controller;

import com.claimwise.dto.RetrievalRequestDto;
import com.claimwise.dto.RetrievalResponseDto;
import com.claimwise.dto.RetrievedChunkDto;
import com.claimwise.exception.GlobalExceptionHandler;
import com.claimwise.service.RetrievalService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RetrievalControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RetrievalService retrievalService;

    @InjectMocks
    private RetrievalController retrievalController;

    private RetrievalResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(retrievalController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = RetrievalResponseDto.builder()
                .query("water damage")
                .totalResults(1)
                .results(List.of(RetrievedChunkDto.builder()
                        .chunkId(1L)
                        .documentId(100L)
                        .chunkIndex(0)
                        .chunkText("Water damage coverage details up to $50,000.")
                        .similarityScore(0.92)
                        .metadata("{\"charStart\":0}")
                        .build()))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/retrieval/search - returns 200 OK with similarity results")
    void shouldSearchSuccessfully() throws Exception {
        RetrievalRequestDto request = RetrievalRequestDto.builder()
                .query("water damage")
                .policyId(10L)
                .topK(3)
                .build();

        when(retrievalService.search(any(RetrievalRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/retrieval/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("water damage"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.results[0].chunkId").value(1))
                .andExpect(jsonPath("$.results[0].similarityScore").value(0.92))
                .andExpect(jsonPath("$.results[0].chunkText").value("Water damage coverage details up to $50,000."));
    }

    @Test
    @DisplayName("POST /api/v1/retrieval/search - returns 400 Bad Request when query is blank")
    void shouldReturnBadRequestWhenQueryIsBlank() throws Exception {
        RetrievalRequestDto invalidRequest = RetrievalRequestDto.builder()
                .query("")
                .build();

        mockMvc.perform(post("/api/v1/retrieval/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.query").exists());
    }
}
