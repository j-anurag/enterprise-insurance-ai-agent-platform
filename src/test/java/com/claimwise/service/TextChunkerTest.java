package com.claimwise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkerTest {

    private TextChunker textChunker;

    @BeforeEach
    void setUp() {
        textChunker = new TextChunkerImpl(500, 50);
    }

    @Test
    @DisplayName("Should return empty list for null or blank text")
    void shouldReturnEmptyListForNullOrBlankText() {
        assertThat(textChunker.chunkText(1L, null)).isEmpty();
        assertThat(textChunker.chunkText(1L, "")).isEmpty();
        assertThat(textChunker.chunkText(1L, "   ")).isEmpty();
    }

    @Test
    @DisplayName("Should produce a single chunk when text is shorter than chunk size")
    void shouldProduceSingleChunkWhenTextIsShort() {
        String shortText = "Comprehensive Home Insurance Policy covers fire and theft.";
        List<TextChunk> chunks = textChunker.chunkText(1L, shortText);

        assertThat(chunks).hasSize(1);
        TextChunk chunk = chunks.get(0);
        assertThat(chunk.getDocumentId()).isEqualTo(1L);
        assertThat(chunk.getChunkIndex()).isEqualTo(0);
        assertThat(chunk.getChunkText()).isEqualTo(shortText);
        assertThat(chunk.getMetadata()).contains("\"charStart\": 0");
    }

    @Test
    @DisplayName("Should produce multiple overlapping chunks when text exceeds chunk size")
    void shouldProduceMultipleOverlappingChunks() {
        // Construct 1200 characters of text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            sb.append(String.format("Sentence %02d about insurance terms. ", i));
        }
        String text = sb.toString();

        List<TextChunk> chunks = textChunker.chunkText(1L, text, 200, 40);

        assertThat(chunks).hasSizeGreaterThan(1);
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            assertThat(chunk.getDocumentId()).isEqualTo(1L);
            assertThat(chunk.getChunkIndex()).isEqualTo(i);
            assertThat(chunk.getChunkText()).isNotEmpty();
            assertThat(chunk.getMetadata()).isNotBlank();
        }

        // Verify sequential index numbering
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i).getChunkIndex()).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("Should reject invalid chunk size or overlap parameters")
    void shouldRejectInvalidParameters() {
        assertThatThrownBy(() -> textChunker.chunkText(1L, "Some text", 0, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize must be greater than 0");

        assertThatThrownBy(() -> textChunker.chunkText(1L, "Some text", 100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkOverlap must be non-negative and less than chunkSize");

        assertThatThrownBy(() -> textChunker.chunkText(1L, "Some text", 100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkOverlap must be non-negative and less than chunkSize");
    }
}
