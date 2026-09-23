package com.claimwise.controller;

import com.claimwise.dto.PolicyAnswerResponseDto;
import com.claimwise.dto.PolicyQuestionRequestDto;
import com.claimwise.service.PolicyRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Intelligence & RAG", description = "APIs for grounded LLM question answering over policy documents")
public class PolicyQaController {

    private final PolicyRagService policyRagService;

    @PostMapping("/{policyId}/ask")
    @Operation(summary = "Ask question about policy", description = "Retrieves relevant policy chunks via pgvector and generates a grounded answer via Groq LLM")
    public ResponseEntity<PolicyAnswerResponseDto> askQuestion(
            @PathVariable Long policyId,
            @Valid @RequestBody PolicyQuestionRequestDto requestDto) {
        PolicyAnswerResponseDto response = policyRagService.askQuestion(policyId, requestDto);
        return ResponseEntity.ok(response);
    }
}
