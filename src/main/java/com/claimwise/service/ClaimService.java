package com.claimwise.service;

import com.claimwise.dto.ClaimRequestDto;
import com.claimwise.dto.ClaimResponseDto;

import java.util.List;

public interface ClaimService {

    ClaimResponseDto createClaim(ClaimRequestDto requestDto);

    List<ClaimResponseDto> getAllClaims();

    List<ClaimResponseDto> getClaimsByPolicyId(Long policyId);
}
