package com.claimwise.service;

import com.claimwise.dto.CustomerRequestDto;
import com.claimwise.dto.CustomerResponseDto;
import com.claimwise.exception.DuplicateResourceException;
import com.claimwise.exception.ResourceNotFoundException;
import com.claimwise.model.Customer;
import com.claimwise.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponseDto createCustomer(CustomerRequestDto requestDto) {
        if (customerRepository.existsByCustomerNumber(requestDto.getCustomerNumber())) {
            throw new DuplicateResourceException(
                    "Customer with customer number " + requestDto.getCustomerNumber() + " already exists");
        }
        if (customerRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException(
                    "Customer with email " + requestDto.getEmail() + " already exists");
        }

        Customer customer = Customer.builder()
                .customerNumber(requestDto.getCustomerNumber())
                .fullName(requestDto.getFullName())
                .email(requestDto.getEmail())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponseDto(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToResponseDto(customer);
    }

    private CustomerResponseDto mapToResponseDto(Customer customer) {
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
