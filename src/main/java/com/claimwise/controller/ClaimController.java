package com.claimwise.controller;

import com.claimwise.dto.ClaimRequestDto;
import com.claimwise.dto.ClaimResponseDto;
import com.claimwise.service.ClaimService;
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
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Tag(name = "Claim Management", description = "APIs for filing and retrieving insurance claims")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @Operation(summary = "File claim", description = "Submits a new claim against an active insurance policy")
    public ResponseEntity<ClaimResponseDto> createClaim(
            @Valid @RequestBody ClaimRequestDto requestDto) {
        ClaimResponseDto response = claimService.createClaim(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List claims", description = "Retrieves all filed claims")
    public ResponseEntity<List<ClaimResponseDto>> getAllClaims() {
        List<ClaimResponseDto> response = claimService.getAllClaims();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/policy/{policyId}")
    @Operation(summary = "Get claims by policy ID", description = "Retrieves all claims associated with a specific policy ID")
    public ResponseEntity<List<ClaimResponseDto>> getClaimsByPolicyId(
            @PathVariable Long policyId) {
        List<ClaimResponseDto> response = claimService.getClaimsByPolicyId(policyId);
        return ResponseEntity.ok(response);
    }
}
