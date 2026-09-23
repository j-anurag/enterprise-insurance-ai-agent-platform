package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAnswerSourceDto {

    private Long chunkId;
    private Long documentId;
    private Integer chunkIndex;
    private Double similarityScore;
    private String snippet;
}
