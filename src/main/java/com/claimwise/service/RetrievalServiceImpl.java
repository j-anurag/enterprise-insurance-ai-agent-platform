package com.claimwise.service;

import com.claimwise.dto.RetrievalRequestDto;
import com.claimwise.dto.RetrievalResponseDto;
import com.claimwise.dto.RetrievedChunkDto;
import com.claimwise.exception.InvalidRequestException;
import com.claimwise.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetrievalServiceImpl implements RetrievalService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    @Override
    public RetrievalResponseDto search(RetrievalRequestDto requestDto) {
        if (requestDto.getQuery() == null || requestDto.getQuery().trim().isEmpty()) {
            throw new InvalidRequestException("Search query cannot be empty");
        }

        int topK = (requestDto.getTopK() != null && requestDto.getTopK() > 0) ? requestDto.getTopK() : 3;

        log.debug("Generating embedding for search query: '{}'", requestDto.getQuery());
        List<Double> queryVector = embeddingService.generateEmbedding(requestDto.getQuery());
        String vectorString = PolicyDocumentServiceImpl.formatVector(queryVector);

        log.debug("Executing native pgvector cosine similarity search with topK={}", topK);
        List<DocumentChunkRepository.ChunkSearchResult> results =
                documentChunkRepository.findSimilarChunks(vectorString, requestDto.getPolicyId(), topK);

        List<RetrievedChunkDto> chunkDtos = results.stream()
                .map(r -> RetrievedChunkDto.builder()
                        .chunkId(r.getId())
                        .documentId(r.getDocumentId())
                        .chunkIndex(r.getChunkIndex())
                        .chunkText(r.getChunkText())
                        .similarityScore(r.getSimilarity())
                        .metadata(r.getMetadata())
                        .build())
                .toList();

        return RetrievalResponseDto.builder()
                .query(requestDto.getQuery())
                .totalResults(chunkDtos.size())
                .results(chunkDtos)
                .build();
    }
}
