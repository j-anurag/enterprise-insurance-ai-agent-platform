package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDocumentResponseDto {

    private Long id;
    private Long policyId;
    private String documentName;
    private String documentType;
    private String storageReference;
    private Integer extractedTextLength;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
