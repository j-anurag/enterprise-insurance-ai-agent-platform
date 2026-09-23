package com.claimwise.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrievalRequestDto {

    @NotBlank(message = "Search query is required")
    private String query;

    private Long policyId;

    @Min(value = 1, message = "topK must be at least 1")
    @Builder.Default
    private Integer topK = 3;
}
