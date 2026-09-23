package com.claimwise.controller;

import com.claimwise.dto.RetrievalRequestDto;
import com.claimwise.dto.RetrievalResponseDto;
import com.claimwise.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retrieval")
@RequiredArgsConstructor
@Tag(name = "Semantic Retrieval", description = "APIs for vector similarity search across policy documents")
public class RetrievalController {

    private final RetrievalService retrievalService;

    @PostMapping("/search")
    @Operation(summary = "Semantic vector search", description = "Executes native pgvector cosine similarity search to retrieve relevant document chunks")
    public ResponseEntity<RetrievalResponseDto> search(
            @Valid @RequestBody RetrievalRequestDto requestDto) {
        RetrievalResponseDto response = retrievalService.search(requestDto);
        return ResponseEntity.ok(response);
    }
}
