package com.claimwise.repository;

import com.claimwise.model.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {
    List<PolicyDocument> findByPolicyId(Long policyId);
    boolean existsByPolicyIdAndDocumentName(Long policyId, String documentName);
}
