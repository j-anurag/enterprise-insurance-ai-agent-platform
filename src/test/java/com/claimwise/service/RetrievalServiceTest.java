package com.claimwise.service;

import com.claimwise.dto.RetrievalRequestDto;
import com.claimwise.dto.RetrievalResponseDto;
import com.claimwise.exception.InvalidRequestException;
import com.claimwise.repository.DocumentChunkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private RetrievalServiceImpl retrievalService;

    private static class TestChunkSearchResult implements DocumentChunkRepository.ChunkSearchResult {
        private final Long id;
        private final Long documentId;
        private final Integer chunkIndex;
        private final String chunkText;
        private final Double similarity;
        private final String metadata;

        public TestChunkSearchResult(Long id, Long documentId, Integer chunkIndex, String chunkText, Double similarity, String metadata) {
            this.id = id;
            this.documentId = documentId;
            this.chunkIndex = chunkIndex;
            this.chunkText = chunkText;
            this.similarity = similarity;
            this.metadata = metadata;
        }

        @Override public Long getId() { return id; }
        @Override public Long getDocumentId() { return documentId; }
        @Override public Integer getChunkIndex() { return chunkIndex; }
        @Override public String getChunkText() { return chunkText; }
        @Override public Double getSimilarity() { return similarity; }
        @Override public String getMetadata() { return metadata; }
    }

    @Test
    @DisplayName("Should execute similarity search and map results to response DTO")
    void shouldExecuteSimilaritySearch() {
        RetrievalRequestDto request = RetrievalRequestDto.builder()
                .query("Does policy cover water damage?")
                .policyId(10L)
                .topK(3)
                .build();

        List<Double> dummyVector = new ArrayList<>();
        for (int i = 0; i < 384; i++) {
            dummyVector.add(0.01);
        }
        when(embeddingService.generateEmbedding("Does policy cover water damage?")).thenReturn(dummyVector);

        List<DocumentChunkRepository.ChunkSearchResult> mockResults = List.of(
                new TestChunkSearchResult(1L, 100L, 0, "Water damage from burst pipe is covered up to $50,000.", 0.89, "{\"charStart\":0}"),
                new TestChunkSearchResult(2L, 100L, 1, "Flood damage requires a separate flood policy rider.", 0.72, "{\"charStart\":100}")
        );

        when(documentChunkRepository.findSimilarChunks(anyString(), eq(10L), eq(3)))
                .thenReturn(mockResults);

        RetrievalResponseDto response = retrievalService.search(request);

        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo("Does policy cover water damage?");
        assertThat(response.getTotalResults()).isEqualTo(2);
        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getChunkText()).contains("Water damage from burst pipe");
        assertThat(response.getResults().get(0).getSimilarityScore()).isEqualTo(0.89);
    }

    @Test
    @DisplayName("Should use default topK of 3 when topK is not specified")
    void shouldUseDefaultTopK() {
        RetrievalRequestDto request = RetrievalRequestDto.builder()
                .query("Roof leakage coverage")
                .build();

        List<Double> dummyVector = new ArrayList<>();
        for (int i = 0; i < 384; i++) {
            dummyVector.add(0.02);
        }
        when(embeddingService.generateEmbedding("Roof leakage coverage")).thenReturn(dummyVector);

        when(documentChunkRepository.findSimilarChunks(anyString(), eq(null), eq(3)))
                .thenReturn(List.of());

        RetrievalResponseDto response = retrievalService.search(request);

        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reject search when query is null or blank")
    void shouldRejectBlankQuery() {
        RetrievalRequestDto nullQuery = RetrievalRequestDto.builder().query(null).build();
        assertThatThrownBy(() -> retrievalService.search(nullQuery))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Search query cannot be empty");

        RetrievalRequestDto emptyQuery = RetrievalRequestDto.builder().query("   ").build();
        assertThatThrownBy(() -> retrievalService.search(emptyQuery))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Search query cannot be empty");
    }
}
