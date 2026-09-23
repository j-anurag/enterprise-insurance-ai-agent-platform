package com.claimwise.service;

import com.claimwise.dto.ClaimRequestDto;
import com.claimwise.dto.ClaimResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.InvalidRequestException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.Claim;
import com.claimwise.model.ClaimStatus;
import com.claimwise.model.Customer;
import com.claimwise.model.Policy;
import com.claimwise.model.PolicyStatus;
import com.claimwise.model.PolicyType;
import com.claimwise.repository.ClaimRepository;
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
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private ClaimServiceImpl claimService;

    private Policy samplePolicy;
    private Claim sampleClaim;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
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
                .customer(customer)
                .policyType(PolicyType.HEALTH)
                .startDate(LocalDate.now().minusMonths(3))
                .expiryDate(LocalDate.now().plusMonths(9))
                .status(PolicyStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        sampleClaim = Claim.builder()
                .id(100L)
                .claimNumber("CLM-1001")
                .policy(samplePolicy)
                .description("Emergency medical treatment")
                .incidentDate(LocalDate.now().minusDays(5))
                .status(ClaimStatus.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a claim")
    void shouldCreateClaim() {
        ClaimRequestDto request = ClaimRequestDto.builder()
                .claimNumber("CLM-1001")
                .policyId(10L)
                .description("Emergency medical treatment")
                .incidentDate(LocalDate.now().minusDays(5))
                .status(ClaimStatus.SUBMITTED)
                .build();

        when(policyRepository.findById(10L)).thenReturn(Optional.of(samplePolicy));
        when(claimRepository.existsByClaimNumber("CLM-1001")).thenReturn(false);
        when(claimRepository.save(any(Claim.class))).thenReturn(sampleClaim);

        ClaimResponseDto response = claimService.createClaim(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getClaimNumber()).isEqualTo("CLM-1001");
        assertThat(response.getPolicyId()).isEqualTo(10L);
        assertThat(response.getPolicyNumber()).isEqualTo("POL-1001");
        assertThat(response.getDescription()).isEqualTo("Emergency medical treatment");
        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when policy not found during claim creation")
    void shouldThrowWhenPolicyNotFound() {
        ClaimRequestDto request = ClaimRequestDto.builder()
                .claimNumber("CLM-1001")
                .policyId(999L)
                .description("Emergency medical treatment")
                .incidentDate(LocalDate.now().minusDays(5))
                .status(ClaimStatus.SUBMITTED)
                .build();

        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.createClaim(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Policy not found");

        verify(claimRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when claimNumber already exists")
    void shouldThrowWhenClaimNumberExists() {
        ClaimRequestDto request = ClaimRequestDto.builder()
                .claimNumber("CLM-1001")
                .policyId(10L)
                .description("Emergency medical treatment")
                .incidentDate(LocalDate.now().minusDays(5))
                .status(ClaimStatus.SUBMITTED)
                .build();

        when(policyRepository.findById(10L)).thenReturn(Optional.of(samplePolicy));
        when(claimRepository.existsByClaimNumber("CLM-1001")).thenReturn(true);

        assertThatThrownBy(() -> claimService.createClaim(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(claimRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when incidentDate is in the future")
    void shouldThrowWhenIncidentDateInFuture() {
        ClaimRequestDto request = ClaimRequestDto.builder()
                .claimNumber("CLM-1001")
                .policyId(10L)
                .description("Emergency medical treatment")
                .incidentDate(LocalDate.now().plusDays(2))
                .status(ClaimStatus.SUBMITTED)
                .build();

        when(policyRepository.findById(10L)).thenReturn(Optional.of(samplePolicy));
        when(claimRepository.existsByClaimNumber("CLM-1001")).thenReturn(false);

        assertThatThrownBy(() -> claimService.createClaim(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Incident date cannot be in the future");

        verify(claimRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return all claims")
    void shouldReturnAllClaims() {
        when(claimRepository.findAll()).thenReturn(List.of(sampleClaim));

        List<ClaimResponseDto> claims = claimService.getAllClaims();

        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getClaimNumber()).isEqualTo("CLM-1001");
    }

    @Test
    @DisplayName("Should return claims for a specific policy ID")
    void shouldReturnClaimsByPolicyId() {
        when(policyRepository.existsById(10L)).thenReturn(true);
        when(claimRepository.findByPolicyId(10L)).thenReturn(List.of(sampleClaim));

        List<ClaimResponseDto> claims = claimService.getClaimsByPolicyId(10L);

        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getClaimNumber()).isEqualTo("CLM-1001");
        assertThat(claims.get(0).getPolicyId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when policy does not exist for policy ID query")
    void shouldThrowWhenQueryingClaimsForNonExistentPolicy() {
        when(policyRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> claimService.getClaimsByPolicyId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Policy not found with id: 999");
    }
}
