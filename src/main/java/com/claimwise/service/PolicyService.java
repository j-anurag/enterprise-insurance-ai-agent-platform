package com.claimwise.service;

import com.claimwise.dto.PolicyRequestDto;
import com.claimwise.dto.PolicyResponseDto;

import java.util.List;

public interface PolicyService {

    PolicyResponseDto createPolicy(PolicyRequestDto requestDto);

    List<PolicyResponseDto> getAllPolicies();

    PolicyResponseDto getPolicyByPolicyNumber(String policyNumber);
}
