package com.claimwise.dto;

import com.claimwise.model.PolicyStatus;
import com.claimwise.model.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyResponseDto {

    private Long id;
    private String policyNumber;
    private Long customerId;
    private String customerNumber;
    private PolicyType policyType;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private PolicyStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
