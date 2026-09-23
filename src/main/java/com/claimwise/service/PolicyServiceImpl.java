package com.claimwise.service;

import com.claimwise.dto.PolicyRequestDto;
import com.claimwise.dto.PolicyResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.InvalidRequestException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.Customer;
import com.claimwise.model.Policy;
import com.claimwise.repository.CustomerRepository;
import com.claimwise.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;

    @Override
    public PolicyResponseDto createPolicy(PolicyRequestDto requestDto) {
        Customer customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + requestDto.getCustomerId()));

        if (policyRepository.existsByPolicyNumber(requestDto.getPolicyNumber())) {
            throw new DuplicateResourceException(
                    "Policy with policy number " + requestDto.getPolicyNumber() + " already exists");
        }

        if (requestDto.getExpiryDate().isBefore(requestDto.getStartDate())) {
            throw new InvalidRequestException("Policy expiry date must be on or after start date");
        }

        Policy policy = Policy.builder()
                .policyNumber(requestDto.getPolicyNumber())
                .customer(customer)
                .policyType(requestDto.getPolicyType())
                .startDate(requestDto.getStartDate())
                .expiryDate(requestDto.getExpiryDate())
                .status(requestDto.getStatus())
                .build();

        Policy savedPolicy = policyRepository.save(policy);
        return mapToResponseDto(savedPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponseDto> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponseDto getPolicyByPolicyNumber(String policyNumber) {
        Policy policy = policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found with policy number: " + policyNumber));
        return mapToResponseDto(policy);
    }

    private PolicyResponseDto mapToResponseDto(Policy policy) {
        return PolicyResponseDto.builder()
                .id(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .customerId(policy.getCustomer().getId())
                .customerNumber(policy.getCustomer().getCustomerNumber())
                .policyType(policy.getPolicyType())
                .startDate(policy.getStartDate())
                .expiryDate(policy.getExpiryDate())
                .status(policy.getStatus())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
