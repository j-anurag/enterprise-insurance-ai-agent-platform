package com.claimwise.service;

import com.claimwise.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class EmbeddingServiceTest {

    private DeterministicEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new DeterministicEmbeddingService(384);
    }

    @Test
    @DisplayName("Should generate exactly 384 dimensions for deterministic service")
    void shouldGenerate384Dimensions() {
        assertThat(embeddingService.getDimensions()).isEqualTo(384);

        List<Double> vector = embeddingService.generateEmbedding("Test insurance policy clause");
        assertThat(vector).hasSize(384);
    }

    @Test
    @DisplayName("Should produce a unit-normalized vector with norm approximately 1.0")
    void shouldProduceNormalizedVector() {
        List<Double> vector = embeddingService.generateEmbedding("Insurance claim coverage");

        double sumSquares = vector.stream().mapToDouble(v -> v * v).sum();
        double norm = Math.sqrt(sumSquares);

        assertThat(norm).isCloseTo(1.0, offset(1e-5));
    }

    @Test
    @DisplayName("Should be deterministic: identical input generates identical vector")
    void shouldBeDeterministicForIdenticalInput() {
        String input = "Water damage due to burst pipes";
        List<Double> vector1 = embeddingService.generateEmbedding(input);
        List<Double> vector2 = embeddingService.generateEmbedding(input);

        assertThat(vector1).isEqualTo(vector2);
    }

    @Test
    @DisplayName("Should produce different vectors for different inputs")
    void shouldProduceDifferentVectorsForDifferentInputs() {
        List<Double> vector1 = embeddingService.generateEmbedding("Auto collision coverage");
        List<Double> vector2 = embeddingService.generateEmbedding("Commercial property liability");

        assertThat(vector1).isNotEqualTo(vector2);
    }

    @Test
    @DisplayName("Should reject null or blank text")
    void shouldRejectBlankText() {
        assertThatThrownBy(() -> embeddingService.generateEmbedding(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot be empty");

        assertThatThrownBy(() -> embeddingService.generateEmbedding("   "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("Should format vector correctly for pgvector syntax")
    void shouldFormatVectorAsPgvectorString() {
        List<Double> vector = List.of(0.123, -0.456, 0.789);
        String formatted = PolicyDocumentServiceImpl.formatVector(vector);

        assertThat(formatted).isEqualTo("[0.123,-0.456,0.789]");
    }
}
