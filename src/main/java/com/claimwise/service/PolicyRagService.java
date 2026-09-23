package com.claimwise.service;

import com.claimwise.dto.PolicyAnswerResponseDto;
import com.claimwise.dto.PolicyQuestionRequestDto;

public interface PolicyRagService {

    PolicyAnswerResponseDto askQuestion(Long policyId, PolicyQuestionRequestDto requestDto);
}
