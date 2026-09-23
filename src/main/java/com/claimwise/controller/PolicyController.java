package com.claimwise.controller;

import com.claimwise.dto.PolicyRequestDto;
import com.claimwise.dto.PolicyResponseDto;
import com.claimwise.service.PolicyService;
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
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Management", description = "APIs for managing insurance policies")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Create policy", description = "Underwrites and creates a new policy for a customer")
    public ResponseEntity<PolicyResponseDto> createPolicy(
            @Valid @RequestBody PolicyRequestDto requestDto) {
        PolicyResponseDto response = policyService.createPolicy(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List policies", description = "Retrieves all policies in the platform")
    public ResponseEntity<List<PolicyResponseDto>> getAllPolicies() {
        List<PolicyResponseDto> response = policyService.getAllPolicies();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{policyNumber}")
    @Operation(summary = "Get policy by number", description = "Retrieves a specific policy by its unique policy number")
    public ResponseEntity<PolicyResponseDto> getPolicyByPolicyNumber(
            @PathVariable String policyNumber) {
        PolicyResponseDto response = policyService.getPolicyByPolicyNumber(policyNumber);
        return ResponseEntity.ok(response);
    }
}
