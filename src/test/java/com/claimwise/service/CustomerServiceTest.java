package com.claimwise.service;

import com.claimwise.dto.CustomerRequestDto;
import com.claimwise.dto.CustomerResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.Customer;
import com.claimwise.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("Should successfully create a customer")
    void shouldCreateCustomer() {
        CustomerRequestDto request = CustomerRequestDto.builder()
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(customerRepository.existsByCustomerNumber("CUST-1001")).thenReturn(false);
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponseDto response = customerService.createCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCustomerNumber()).isEqualTo("CUST-1001");
        assertThat(response.getFullName()).isEqualTo("Alice Smith");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when customerNumber already exists")
    void shouldThrowWhenCustomerNumberExists() {
        CustomerRequestDto request = CustomerRequestDto.builder()
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        when(customerRepository.existsByCustomerNumber("CUST-1001")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void shouldThrowWhenEmailExists() {
        CustomerRequestDto request = CustomerRequestDto.builder()
                .customerNumber("CUST-1001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        when(customerRepository.existsByCustomerNumber("CUST-1001")).thenReturn(false);
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve all customers")
    void shouldGetAllCustomers() {
        Customer customer1 = Customer.builder()
                .id(1L)
                .customerNumber("CUST-1")
                .fullName("Alice")
                .email("alice@example.com")
                .build();
        Customer customer2 = Customer.builder()
                .id(2L)
                .customerNumber("CUST-2")
                .fullName("Bob")
                .email("bob@example.com")
                .build();

        when(customerRepository.findAll()).thenReturn(List.of(customer1, customer2));

        List<CustomerResponseDto> list = customerService.getAllCustomers();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getCustomerNumber()).isEqualTo("CUST-1");
        assertThat(list.get(1).getCustomerNumber()).isEqualTo("CUST-2");
    }

    @Test
    @DisplayName("Should find customer by ID")
    void shouldFindCustomerById() {
        Customer customer = Customer.builder()
                .id(1L)
                .customerNumber("CUST-1")
                .fullName("Alice")
                .email("alice@example.com")
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponseDto response = customerService.getCustomerById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer ID does not exist")
    void shouldThrowWhenCustomerIdNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
