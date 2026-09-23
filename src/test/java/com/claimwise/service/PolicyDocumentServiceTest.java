package com.claimwise.service;

import com.claimwise.dto.DocumentIngestResponseDto;
import com.claimwise.dto.PolicyDocumentRequestDto;
import com.claimwise.dto.PolicyDocumentResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.Customer;
import com.claimwise.model.Policy;
import com.claimwise.model.PolicyDocument;
import com.claimwise.model.PolicyStatus;
import com.claimwise.model.PolicyType;
import com.claimwise.repository.DocumentChunkRepository;
import com.claimwise.repository.PolicyDocumentRepository;
import com.claimwise.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyDocumentServiceTest {

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentIngestionService documentIngestionService;

    @Mock
    private TextChunker textChunker;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private PolicyDocumentServiceImpl policyDocumentService;

    private Policy samplePolicy;
    private PolicyDocument sampleDocument;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .id(1L)
                .customerNumber("CUST-1001")
                .fullName("John Doe")
                .email("john@example.com")
                .build();

        samplePolicy = Policy.builder()
                .id(10L)
                .policyNumber("POL-1001")
                .customer(customer)
                .policyType(PolicyType.HOME)
                .startDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2027, 1, 1))
                .status(PolicyStatus.ACTIVE)
                .build();

        sampleDocument = PolicyDocument.builder()
                .id(100L)
                .policy(samplePolicy)
                .documentName("home_policy_terms.pdf")
                .documentType("application/pdf")
                .storageReference("/data/docs/home_policy_terms.pdf")
                .extractedText("This policy covers structural damage and theft.")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create policy document metadata")
    void shouldCreatePolicyDocument() {
        PolicyDocumentRequestDto request = PolicyDocumentRequestDto.builder()
                .documentName("home_policy_terms.pdf")
                .documentType("application/pdf")
                .storageReference("/data/docs/home_policy_terms.pdf")
                .build();

        when(policyRepository.findById(10L)).thenReturn(Optional.of(samplePolicy));
        when(policyDocumentRepository.existsByPolicyIdAndDocumentName(10L, "home_policy_terms.pdf")).thenReturn(false);
        when(policyDocumentRepository.save(any(PolicyDocument.class))).thenReturn(sampleDocument);

        PolicyDocumentResponseDto response = policyDocumentService.createPolicyDocument(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDocumentName()).isEqualTo("home_policy_terms.pdf");
        assertThat(response.getPolicyId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when policy does not exist")
    void shouldThrowWhenPolicyNotFound() {
        PolicyDocumentRequestDto request = PolicyDocumentRequestDto.builder()
                .documentName("home_policy_terms.pdf")
                .documentType("application/pdf")
                .storageReference("/data/docs/home_policy_terms.pdf")
                .build();

        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyDocumentService.createPolicyDocument(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Policy not found with id: 999");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when document name already exists for policy")
    void shouldThrowWhenDocumentNameIsDuplicate() {
        PolicyDocumentRequestDto request = PolicyDocumentRequestDto.builder()
                .documentName("home_policy_terms.pdf")
                .documentType("application/pdf")
                .storageReference("/data/docs/home_policy_terms.pdf")
                .build();

        when(policyRepository.findById(10L)).thenReturn(Optional.of(samplePolicy));
        when(policyDocumentRepository.existsByPolicyIdAndDocumentName(10L, "home_policy_terms.pdf")).thenReturn(true);

        assertThatThrownBy(() -> policyDocumentService.createPolicyDocument(10L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists for policy id 10");
    }

    @Test
    @DisplayName("Should retrieve document by ID successfully")
    void shouldGetDocumentById() {
        when(policyDocumentRepository.findById(100L)).thenReturn(Optional.of(sampleDocument));

        PolicyDocumentResponseDto response = policyDocumentService.getDocumentById(100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDocumentName()).isEqualTo("home_policy_terms.pdf");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when document ID does not exist")
    void shouldThrowWhenDocumentIdNotFound() {
        when(policyDocumentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyDocumentService.getDocumentById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Policy document not found with id: 999");
    }

    @Test
    @DisplayName("Should retrieve all documents for a policy")
    void shouldGetDocumentsByPolicyId() {
        when(policyRepository.existsById(10L)).thenReturn(true);
        when(policyDocumentRepository.findByPolicyId(10L)).thenReturn(List.of(sampleDocument));

        List<PolicyDocumentResponseDto> documents = policyDocumentService.getDocumentsByPolicyId(10L);

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getDocumentName()).isEqualTo("home_policy_terms.pdf");
    }

    @Test
    @DisplayName("Should run complete ingestion pipeline: extract, chunk, embed, persist")
    void shouldIngestDocumentSuccessfully() {
        when(policyDocumentRepository.findById(100L)).thenReturn(Optional.of(sampleDocument));
        when(documentIngestionService.extractText("/data/docs/home_policy_terms.pdf"))
                .thenReturn("Extracted text with full insurance clauses.");

        List<TextChunk> sampleChunks = List.of(
                TextChunk.builder()
                        .documentId(100L)
                        .chunkIndex(0)
                        .chunkText("Extracted text with")
                        .metadata("{\"charStart\": 0, \"charEnd\": 19}")
                        .build(),
                TextChunk.builder()
                        .documentId(100L)
                        .chunkIndex(1)
                        .chunkText("full insurance clauses.")
                        .metadata("{\"charStart\": 15, \"charEnd\": 38}")
                        .build()
        );

        when(textChunker.chunkText(100L, "Extracted text with full insurance clauses."))
                .thenReturn(sampleChunks);

        List<Double> dummyVector = new ArrayList<>();
        for (int i = 0; i < 384; i++) {
            dummyVector.add(0.05);
        }
        when(embeddingService.generateEmbedding(anyString())).thenReturn(dummyVector);

        DocumentIngestResponseDto response = policyDocumentService.ingestDocument(100L);

        assertThat(response.getDocumentId()).isEqualTo(100L);
        assertThat(response.getTotalChunksCreated()).isEqualTo(2);
        assertThat(response.getExtractedCharacterCount()).isEqualTo("Extracted text with full insurance clauses.".length());

        verify(documentChunkRepository).deleteByDocumentId(100L);
        verify(documentChunkRepository, org.mockito.Mockito.times(2)).insertChunk(
                eq(100L),
                org.mockito.ArgumentMatchers.anyInt(),
                anyString(),
                anyString(),
                anyString()
        );
        verify(policyDocumentRepository).save(sampleDocument);
    }
}
