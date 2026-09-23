package com.claimwise.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextChunk {
    private Long documentId;
    private int chunkIndex;
    private String chunkText;
    private String metadata;
}
