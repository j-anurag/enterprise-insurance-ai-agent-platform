package com.claimwise.controller;

import com.claimwise.dto.DocumentIngestResponseDto;
import com.claimwise.dto.PolicyDocumentRequestDto;
import com.claimwise.dto.PolicyDocumentResponseDto;
import com.claimwise.service.PolicyDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Policy Document Management", description = "APIs for registering and ingesting policy documents")
public class PolicyDocumentController {

    private final PolicyDocumentService policyDocumentService;

    @PostMapping("/api/v1/policies/{policyId}/documents")
    @Operation(summary = "Register policy document", description = "Associates a new document reference with an existing policy")
    public ResponseEntity<PolicyDocumentResponseDto> createPolicyDocument(
            @PathVariable Long policyId,
            @Valid @RequestBody PolicyDocumentRequestDto requestDto) {
        PolicyDocumentResponseDto response = policyDocumentService.createPolicyDocument(policyId, requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/api/v1/policies/{policyId}/documents")
    @Operation(summary = "List documents for policy", description = "Retrieves all documents associated with a specific policy")
    public ResponseEntity<List<PolicyDocumentResponseDto>> getDocumentsByPolicyId(
            @PathVariable Long policyId) {
        List<PolicyDocumentResponseDto> response = policyDocumentService.getDocumentsByPolicyId(policyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/documents/{documentId}")
    @Operation(summary = "Get document by ID", description = "Retrieves a specific policy document by its ID")
    public ResponseEntity<PolicyDocumentResponseDto> getDocumentById(
            @PathVariable Long documentId) {
        PolicyDocumentResponseDto response = policyDocumentService.getDocumentById(documentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/documents/{documentId}/ingest")
    @Operation(summary = "Ingest document", description = "Extracts PDF text, chunks content, generates embeddings, and stores them in pgvector")
    public ResponseEntity<DocumentIngestResponseDto> ingestDocument(
            @PathVariable Long documentId) {
        DocumentIngestResponseDto response = policyDocumentService.ingestDocument(documentId);
        return ResponseEntity.ok(response);
    }
}
