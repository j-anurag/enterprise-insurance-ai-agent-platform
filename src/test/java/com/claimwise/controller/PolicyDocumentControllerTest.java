package com.claimwise.controller;

import com.claimwise.dto.DocumentIngestResponseDto;
import com.claimwise.dto.PolicyDocumentRequestDto;
import com.claimwise.dto.PolicyDocumentResponseDto;
import com.claimwise.exception.GlobalExceptionHandler;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.service.PolicyDocumentService;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PolicyDocumentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private PolicyDocumentService policyDocumentService;

    @InjectMocks
    private PolicyDocumentController policyDocumentController;

    private PolicyDocumentResponseDto sampleDocumentResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(policyDocumentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleDocumentResponse = PolicyDocumentResponseDto.builder()
                .id(100L)
                .policyId(10L)
                .documentName("home_policy.pdf")
                .documentType("application/pdf")
                .storageReference("/data/docs/home_policy.pdf")
                .extractedTextLength(2500)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/documents - creates document metadata with 201 Created")
    void shouldCreatePolicyDocument() throws Exception {
        PolicyDocumentRequestDto request = PolicyDocumentRequestDto.builder()
                .documentName("home_policy.pdf")
                .documentType("application/pdf")
                .storageReference("/data/docs/home_policy.pdf")
                .build();

        when(policyDocumentService.createPolicyDocument(eq(10L), any(PolicyDocumentRequestDto.class)))
                .thenReturn(sampleDocumentResponse);

        mockMvc.perform(post("/api/v1/policies/10/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.policyId").value(10))
                .andExpect(jsonPath("$.documentName").value("home_policy.pdf"));
    }

    @Test
    @DisplayName("POST /api/v1/policies/{policyId}/documents - validation failure returns 400 Bad Request")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        PolicyDocumentRequestDto invalidRequest = PolicyDocumentRequestDto.builder()
                .documentName("")
                .documentType(null)
                .storageReference(null)
                .build();

        mockMvc.perform(post("/api/v1/policies/10/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.documentName").exists());
    }

    @Test
    @DisplayName("GET /api/v1/policies/{policyId}/documents - returns 200 with document list")
    void shouldGetDocumentsByPolicyId() throws Exception {
        when(policyDocumentService.getDocumentsByPolicyId(10L)).thenReturn(List.of(sampleDocumentResponse));

        mockMvc.perform(get("/api/v1/policies/10/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].documentName").value("home_policy.pdf"));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{documentId} - returns 200 when found")
    void shouldGetDocumentById() throws Exception {
        when(policyDocumentService.getDocumentById(100L)).thenReturn(sampleDocumentResponse);

        mockMvc.perform(get("/api/v1/documents/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.documentName").value("home_policy.pdf"));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{documentId} - returns 404 when document not found")
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        when(policyDocumentService.getDocumentById(999L))
                .thenThrow(new ResourceNotFoundException("Policy document not found with id: 999"));

        mockMvc.perform(get("/api/v1/documents/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Policy document not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/v1/documents/{documentId}/ingest - triggers pipeline and returns 200 OK")
    void shouldIngestDocument() throws Exception {
        DocumentIngestResponseDto ingestResponse = DocumentIngestResponseDto.builder()
                .documentId(100L)
                .extractedCharacterCount(2500)
                .totalChunksCreated(6)
                .build();

        when(policyDocumentService.ingestDocument(100L)).thenReturn(ingestResponse);

        mockMvc.perform(post("/api/v1/documents/100/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(100))
                .andExpect(jsonPath("$.extractedCharacterCount").value(2500))
                .andExpect(jsonPath("$.totalChunksCreated").value(6));
    }
}
