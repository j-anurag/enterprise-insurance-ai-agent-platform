package com.claimwise.service;

import com.claimwise.dto.PolicyRequestDto;
import com.claimwise.dto.PolicyResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.InvalidRequestException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.Customer;
import com.claimwise.model.Policy;
import com.claimwise.model.PolicyStatus;
import com.claimwise.model.PolicyType;
import com.claimwise.repository.CustomerRepository;
import com.claimwise.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private PolicyServiceImpl policyService;

    private Customer sampleCustomer;
    private Policy samplePolicy;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id(1L)
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        samplePolicy = Policy.builder()
                .id(10L)
                .policyNumber("POL-1001")
                .customer(sampleCustomer)
                .policyType(PolicyType.HEALTH)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .status(PolicyStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a policy")
    void shouldCreatePolicy() {
        PolicyRequestDto request = PolicyRequestDto.builder()
                .policyNumber("POL-1001")
                .customerId(1L)
                .policyType(PolicyType.HEALTH)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .status(PolicyStatus.ACTIVE)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(policyRepository.existsByPolicyNumber("POL-1001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenReturn(samplePolicy);

        PolicyResponseDto response = policyService.createPolicy(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getPolicyNumber()).isEqualTo("POL-1001");
        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getCustomerNumber()).isEqualTo("CUST-1001");
        assertThat(response.getPolicyType()).isEqualTo(PolicyType.HEALTH);
        assertThat(response.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        verify(policyRepository).save(any(Policy.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found during policy creation")
    void shouldThrowWhenCustomerNotFound() {
        PolicyRequestDto request = PolicyRequestDto.builder()
                .policyNumber("POL-1001")
                .customerId(999L)
                .policyType(PolicyType.HEALTH)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .status(PolicyStatus.ACTIVE)
                .build();

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.createPolicy(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");

        verify(policyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when policyNumber already exists")
    void shouldThrowWhenPolicyNumberExists() {
        PolicyRequestDto request = PolicyRequestDto.builder()
                .policyNumber("POL-1001")
                .customerId(1L)
                .policyType(PolicyType.HEALTH)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .status(PolicyStatus.ACTIVE)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(policyRepository.existsByPolicyNumber("POL-1001")).thenReturn(true);

        assertThatThrownBy(() -> policyService.createPolicy(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(policyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when expiry date is before start date")
    void shouldThrowWhenExpiryBeforeStartDate() {
        PolicyRequestDto request = PolicyRequestDto.builder()
                .policyNumber("POL-1001")
                .customerId(1L)
                .policyType(PolicyType.HEALTH)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().minusDays(1))
                .status(PolicyStatus.ACTIVE)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(policyRepository.existsByPolicyNumber("POL-1001")).thenReturn(false);

        assertThatThrownBy(() -> policyService.createPolicy(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("expiry date must be on or after start date");

        verify(policyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return all policies")
    void shouldReturnAllPolicies() {
        when(policyRepository.findAll()).thenReturn(List.of(samplePolicy));

        List<PolicyResponseDto> policies = policyService.getAllPolicies();

        assertThat(policies).hasSize(1);
        assertThat(policies.get(0).getPolicyNumber()).isEqualTo("POL-1001");
    }

    @Test
    @DisplayName("Should get policy by policy number")
    void shouldGetPolicyByPolicyNumber() {
        when(policyRepository.findByPolicyNumber("POL-1001")).thenReturn(Optional.of(samplePolicy));

        PolicyResponseDto response = policyService.getPolicyByPolicyNumber("POL-1001");

        assertThat(response).isNotNull();
        assertThat(response.getPolicyNumber()).isEqualTo("POL-1001");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when policy number does not exist")
    void shouldThrowWhenPolicyNumberNotFound() {
        when(policyRepository.findByPolicyNumber("POL-UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicyByPolicyNumber("POL-UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Policy not found");
    }
}
