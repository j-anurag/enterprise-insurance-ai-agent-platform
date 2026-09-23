package com.claimwise.dto;

import com.claimwise.model.ClaimStatus;
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
public class ClaimResponseDto {

    private Long id;
    private String claimNumber;
    private Long policyId;
    private String policyNumber;
    private String description;
    private LocalDate incidentDate;
    private ClaimStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
