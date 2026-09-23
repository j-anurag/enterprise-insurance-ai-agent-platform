package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAnswerResponseDto {

    private String question;
    private String answer;
    private Long policyId;

    @Builder.Default
    private List<PolicyAnswerSourceDto> sources = new ArrayList<>();
}
