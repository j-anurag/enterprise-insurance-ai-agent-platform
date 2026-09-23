package com.claimwise.service;

import com.claimwise.dto.DocumentIngestResponseDto;
import com.claimwise.dto.PolicyDocumentRequestDto;
import com.claimwise.dto.PolicyDocumentResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.DocumentChunk;
import com.claimwise.model.Policy;
import com.claimwise.model.PolicyDocument;
import com.claimwise.repository.DocumentChunkRepository;
import com.claimwise.repository.PolicyDocumentRepository;
import com.claimwise.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PolicyDocumentServiceImpl implements PolicyDocumentService {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyRepository policyRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentIngestionService documentIngestionService;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;

    @Override
    public PolicyDocumentResponseDto createPolicyDocument(Long policyId, PolicyDocumentRequestDto requestDto) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        if (policyDocumentRepository.existsByPolicyIdAndDocumentName(policyId, requestDto.getDocumentName())) {
            throw new DuplicateResourceException(String.format(
                    "Document with name '%s' already exists for policy id %d",
                    requestDto.getDocumentName(), policyId));
        }

        PolicyDocument document = PolicyDocument.builder()
                .policy(policy)
                .documentName(requestDto.getDocumentName())
                .documentType(requestDto.getDocumentType())
                .storageReference(requestDto.getStorageReference())
                .build();

        PolicyDocument saved = policyDocumentRepository.save(document);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyDocumentResponseDto> getDocumentsByPolicyId(Long policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new ResourceNotFoundException("Policy not found with id: " + policyId);
        }

        return policyDocumentRepository.findByPolicyId(policyId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyDocumentResponseDto getDocumentById(Long documentId) {
        PolicyDocument document = policyDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy document not found with id: " + documentId));
        return mapToResponseDto(document);
    }

    @Override
    public DocumentIngestResponseDto ingestDocument(Long documentId) {
        PolicyDocument document = policyDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy document not found with id: " + documentId));

        log.info("Starting ingestion for document id: {}, reference: {}", documentId, document.getStorageReference());

        String extractedText = documentIngestionService.extractText(document.getStorageReference());
        document.setExtractedText(extractedText);
        policyDocumentRepository.save(document);

        List<TextChunk> textChunks = textChunker.chunkText(documentId, extractedText);
        log.info("Created {} chunks for document id: {}", textChunks.size(), documentId);

        documentChunkRepository.deleteByDocumentId(documentId);

        for (TextChunk chunk : textChunks) {
            List<Double> vector = embeddingService.generateEmbedding(chunk.getChunkText());
            String vectorString = formatVector(vector);

            documentChunkRepository.insertChunk(
                    documentId,
                    chunk.getChunkIndex(),
                    chunk.getChunkText(),
                    vectorString,
                    chunk.getMetadata()
            );
        }
        log.info("Saved {} chunks with embeddings to pgvector for document id: {}", textChunks.size(), documentId);

        return DocumentIngestResponseDto.builder()
                .documentId(documentId)
                .extractedCharacterCount(extractedText.length())
                .totalChunksCreated(textChunks.size())
                .build();
    }

    public static String formatVector(List<Double> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private PolicyDocumentResponseDto mapToResponseDto(PolicyDocument document) {
        return PolicyDocumentResponseDto.builder()
                .id(document.getId())
                .policyId(document.getPolicy().getId())
                .documentName(document.getDocumentName())
                .documentType(document.getDocumentType())
                .storageReference(document.getStorageReference())
                .extractedTextLength(document.getExtractedText() != null ? document.getExtractedText().length() : 0)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
