package com.claimwise.dto;

import com.claimwise.model.ClaimStatus;
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
public class ClaimRequestDto {

    @NotBlank(message = "Claim number is required")
    @Size(max = 50, message = "Claim number cannot exceed 50 characters")
    private String claimNumber;

    @NotNull(message = "Policy ID is required")
    private Long policyId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Incident date is required")
    private LocalDate incidentDate;

    @NotNull(message = "Claim status is required")
    private ClaimStatus status;
}
