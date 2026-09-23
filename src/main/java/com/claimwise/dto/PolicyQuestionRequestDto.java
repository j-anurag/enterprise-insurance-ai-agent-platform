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
public class PolicyQuestionRequestDto {

    @NotBlank(message = "Question cannot be empty")
    @Size(min = 3, max = 1000, message = "Question must be between 3 and 1000 characters")
    private String question;
}
