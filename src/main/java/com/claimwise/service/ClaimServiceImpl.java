package com.claimwise.service;

import com.claimwise.dto.ClaimRequestDto;
import com.claimwise.dto.ClaimResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.InvalidRequestException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.Claim;
import com.claimwise.model.Policy;
import com.claimwise.repository.ClaimRepository;
import com.claimwise.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;

    @Override
    public ClaimResponseDto createClaim(ClaimRequestDto requestDto) {
        Policy policy = policyRepository.findById(requestDto.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found with id: " + requestDto.getPolicyId()));

        if (claimRepository.existsByClaimNumber(requestDto.getClaimNumber())) {
            throw new DuplicateResourceException(
                    "Claim with claim number " + requestDto.getClaimNumber() + " already exists");
        }

        if (requestDto.getIncidentDate().isAfter(LocalDate.now())) {
            throw new InvalidRequestException("Incident date cannot be in the future");
        }

        Claim claim = Claim.builder()
                .claimNumber(requestDto.getClaimNumber())
                .policy(policy)
                .description(requestDto.getDescription())
                .incidentDate(requestDto.getIncidentDate())
                .status(requestDto.getStatus())
                .build();

        Claim savedClaim = claimRepository.save(claim);
        return mapToResponseDto(savedClaim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getClaimsByPolicyId(Long policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new ResourceNotFoundException("Policy not found with id: " + policyId);
        }
        return claimRepository.findByPolicyId(policyId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private ClaimResponseDto mapToResponseDto(Claim claim) {
        return ClaimResponseDto.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber())
                .policyId(claim.getPolicy().getId())
                .policyNumber(claim.getPolicy().getPolicyNumber())
                .description(claim.getDescription())
                .incidentDate(claim.getIncidentDate())
                .status(claim.getStatus())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
