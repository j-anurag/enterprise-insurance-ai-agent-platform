package com.claimwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDocumentRequestDto {

    @NotBlank(message = "Document name is required")
    @Size(max = 255, message = "Document name must not exceed 255 characters")
    private String documentName;

    @NotBlank(message = "Document type is required")
    @Size(max = 100, message = "Document type must not exceed 100 characters")
    private String documentType;

    @NotBlank(message = "Storage reference is required")
    @Size(max = 500, message = "Storage reference must not exceed 500 characters")
    private String storageReference;
}
