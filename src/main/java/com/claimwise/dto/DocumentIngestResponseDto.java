package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentIngestResponseDto {

    private Long documentId;
    private Integer extractedCharacterCount;
    private Integer totalChunksCreated;
}
