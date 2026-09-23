package com.claimwise.service;

import com.claimwise.dto.DocumentIngestResponseDto;
import com.claimwise.dto.PolicyDocumentRequestDto;
import com.claimwise.dto.PolicyDocumentResponseDto;

import java.util.List;

public interface PolicyDocumentService {

    PolicyDocumentResponseDto createPolicyDocument(Long policyId, PolicyDocumentRequestDto requestDto);

    List<PolicyDocumentResponseDto> getDocumentsByPolicyId(Long policyId);

    PolicyDocumentResponseDto getDocumentById(Long documentId);

    DocumentIngestResponseDto ingestDocument(Long documentId);
}
