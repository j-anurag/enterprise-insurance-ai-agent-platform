package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrievedChunkDto {

    private Long chunkId;
    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private Double similarityScore;
    private String metadata;
}
