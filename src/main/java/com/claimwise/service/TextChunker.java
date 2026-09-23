package com.claimwise.service;

import java.util.List;

public interface TextChunker {

    List<TextChunk> chunkText(Long documentId, String text);

    List<TextChunk> chunkText(Long documentId, String text, int chunkSize, int chunkOverlap);
}
