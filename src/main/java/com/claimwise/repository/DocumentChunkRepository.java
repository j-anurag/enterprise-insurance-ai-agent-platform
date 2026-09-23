package com.claimwise.repository;

import com.claimwise.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
        INSERT INTO document_chunks (document_id, chunk_index, chunk_text, embedding, metadata, created_at)
        VALUES (:documentId, :chunkIndex, :chunkText, cast(:embedding as vector), :metadata, CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    void insertChunk(
            @Param("documentId") Long documentId,
            @Param("chunkIndex") Integer chunkIndex,
            @Param("chunkText") String chunkText,
            @Param("embedding") String embedding,
            @Param("metadata") String metadata
    );

    @Query(value = """
        SELECT c.id AS id,
               c.document_id AS documentId,
               c.chunk_index AS chunkIndex,
               c.chunk_text AS chunkText,
               c.metadata AS metadata,
               (1.0 - (c.embedding <=> cast(:queryVector as vector))) AS similarity
        FROM document_chunks c
        WHERE (:policyId IS NULL OR c.document_id IN (
            SELECT d.id FROM policy_documents d WHERE d.policy_id = :policyId
        ))
        ORDER BY c.embedding <=> cast(:queryVector as vector) ASC
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSearchResult> findSimilarChunks(
            @Param("queryVector") String queryVector,
            @Param("policyId") Long policyId,
            @Param("topK") int topK
    );

    interface ChunkSearchResult {
        Long getId();
        Long getDocumentId();
        Integer getChunkIndex();
        String getChunkText();
        String getMetadata();
        Double getSimilarity();
    }
}
