package com.claimwise.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TextChunkerImpl implements TextChunker {

    private final int defaultChunkSize;
    private final int defaultChunkOverlap;

    public TextChunkerImpl(
            @Value("${claimwise.ai.chunking.chunk-size:500}") int defaultChunkSize,
            @Value("${claimwise.ai.chunking.chunk-overlap:50}") int defaultChunkOverlap) {
        this.defaultChunkSize = defaultChunkSize;
        this.defaultChunkOverlap = defaultChunkOverlap;
    }

    @Override
    public List<TextChunk> chunkText(Long documentId, String text) {
        return chunkText(documentId, text, defaultChunkSize, defaultChunkOverlap);
    }

    @Override
    public List<TextChunk> chunkText(Long documentId, String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }

        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be non-negative and less than chunkSize");
        }

        String cleanedText = text.trim();
        List<TextChunk> chunks = new ArrayList<>();
        int stepSize = chunkSize - chunkOverlap;
        int textLength = cleanedText.length();
        int chunkIndex = 0;

        for (int start = 0; start < textLength; start += stepSize) {
            int end = Math.min(start + chunkSize, textLength);
            String chunkContent = cleanedText.substring(start, end).trim();

            if (!chunkContent.isEmpty()) {
                String metadata = String.format("{\"charStart\": %d, \"charEnd\": %d}", start, end);
                chunks.add(TextChunk.builder()
                        .documentId(documentId)
                        .chunkIndex(chunkIndex++)
                        .chunkText(chunkContent)
                        .metadata(metadata)
                        .build());
            }

            if (end == textLength) {
                break;
            }
        }

        return chunks;
    }
}
