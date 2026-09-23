package com.claimwise.repository;

import com.claimwise.model.Claim;
import com.claimwise.model.ClaimStatus;
import com.claimwise.model.Customer;
import com.claimwise.model.Policy;
import com.claimwise.model.PolicyDocument;
import com.claimwise.model.PolicyStatus;
import com.claimwise.model.PolicyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PolicyDocumentRepository policyDocumentRepository;

    @Test
    @DisplayName("Should persist and retrieve customer with unique constraints")
    void shouldPersistAndRetrieveCustomer() {
        Customer customer = Customer.builder()
                .customerNumber("CUST-IT-001")
                .fullName("Integration Customer")
                .email("it-customer@example.com")
                .build();

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(customerRepository.existsByCustomerNumber("CUST-IT-001")).isTrue();
        assertThat(customerRepository.existsByEmail("it-customer@example.com")).isTrue();

        Optional<Customer> retrieved = customerRepository.findByCustomerNumber("CUST-IT-001");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getFullName()).isEqualTo("Integration Customer");
    }

    @Test
    @DisplayName("Should enforce uniqueness constraint on customer number")
    void shouldEnforceCustomerNumberUniqueness() {
        Customer cust1 = Customer.builder()
                .customerNumber("CUST-DUP-01")
                .fullName("Cust One")
                .email("one@example.com")
                .build();
        customerRepository.saveAndFlush(cust1);

        Customer cust2 = Customer.builder()
                .customerNumber("CUST-DUP-01")
                .fullName("Cust Two")
                .email("two@example.com")
                .build();

        assertThatThrownBy(() -> customerRepository.saveAndFlush(cust2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should persist policy linked to customer and retrieve by policy number")
    void shouldPersistAndRetrievePolicy() {
        Customer customer = customerRepository.save(Customer.builder()
                .customerNumber("CUST-POL-01")
                .fullName("Policy Holder")
                .email("holder@example.com")
                .build());

        Policy policy = Policy.builder()
                .policyNumber("POL-IT-001")
                .customer(customer)
                .policyType(PolicyType.HOME)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .status(PolicyStatus.ACTIVE)
                .build();

        Policy savedPolicy = policyRepository.save(policy);

        assertThat(savedPolicy.getId()).isNotNull();
        assertThat(savedPolicy.getCustomer().getId()).isEqualTo(customer.getId());

        Optional<Policy> found = policyRepository.findByPolicyNumber("POL-IT-001");
        assertThat(found).isPresent();
        assertThat(found.get().getPolicyType()).isEqualTo(PolicyType.HOME);
    }

    @Test
    @DisplayName("Should persist claim linked to policy and retrieve by policy ID")
    void shouldPersistAndRetrieveClaim() {
        Customer customer = customerRepository.save(Customer.builder()
                .customerNumber("CUST-CLM-01")
                .fullName("Claimant Name")
                .email("claimant@example.com")
                .build());

        Policy policy = policyRepository.save(Policy.builder()
                .policyNumber("POL-CLM-001")
                .customer(customer)
                .policyType(PolicyType.AUTO)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .status(PolicyStatus.ACTIVE)
                .build());

        Claim claim = Claim.builder()
                .claimNumber("CLM-IT-001")
                .policy(policy)
                .description("Hail storm damage to roof")
                .incidentDate(LocalDate.now().minusDays(3))
                .status(ClaimStatus.SUBMITTED)
                .build();

        Claim savedClaim = claimRepository.save(claim);

        assertThat(savedClaim.getId()).isNotNull();
        assertThat(savedClaim.getClaimNumber()).isEqualTo("CLM-IT-001");

        List<Claim> claims = claimRepository.findByPolicyId(policy.getId());
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getClaimNumber()).isEqualTo("CLM-IT-001");
        assertThat(claims.get(0).getPolicy().getId()).isEqualTo(policy.getId());
    }

    @Test
    @DisplayName("Should persist policy document linked to policy and retrieve by policy ID")
    void shouldPersistAndRetrievePolicyDocument() {
        Customer customer = customerRepository.save(Customer.builder()
                .customerNumber("CUST-DOC-01")
                .fullName("Doc Customer")
                .email("doc-customer@example.com")
                .build());

        Policy policy = policyRepository.save(Policy.builder()
                .policyNumber("POL-DOC-001")
                .customer(customer)
                .policyType(PolicyType.HEALTH)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .status(PolicyStatus.ACTIVE)
                .build());

        PolicyDocument document = PolicyDocument.builder()
                .policy(policy)
                .documentName("health_schedule.pdf")
                .documentType("application/pdf")
                .storageReference("/docs/health_schedule.pdf")
                .extractedText("Health insurance benefits and schedule.")
                .build();

        PolicyDocument savedDoc = policyDocumentRepository.save(document);

        assertThat(savedDoc.getId()).isNotNull();
        assertThat(savedDoc.getDocumentName()).isEqualTo("health_schedule.pdf");
        assertThat(savedDoc.getPolicy().getId()).isEqualTo(policy.getId());

        List<PolicyDocument> docs = policyDocumentRepository.findByPolicyId(policy.getId());
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getDocumentName()).isEqualTo("health_schedule.pdf");
        assertThat(policyDocumentRepository.existsByPolicyIdAndDocumentName(policy.getId(), "health_schedule.pdf")).isTrue();
        assertThat(policyDocumentRepository.existsByPolicyIdAndDocumentName(policy.getId(), "other.pdf")).isFalse();
    }
}
