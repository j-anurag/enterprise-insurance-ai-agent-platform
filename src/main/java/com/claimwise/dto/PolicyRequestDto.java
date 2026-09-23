package com.claimwise.dto;

import com.claimwise.model.PolicyStatus;
import com.claimwise.model.PolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRequestDto {

    @NotBlank(message = "Policy number is required")
    @Size(max = 50, message = "Policy number cannot exceed 50 characters")
    private String policyNumber;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Policy type is required")
    private PolicyType policyType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotNull(message = "Policy status is required")
    private PolicyStatus status;
}
