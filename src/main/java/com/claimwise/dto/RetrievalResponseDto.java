package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrievalResponseDto {

    private String query;
    private Integer totalResults;
    private List<RetrievedChunkDto> results;
}
