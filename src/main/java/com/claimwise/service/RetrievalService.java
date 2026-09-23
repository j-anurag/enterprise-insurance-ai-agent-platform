package com.claimwise.service;

import com.claimwise.dto.RetrievalRequestDto;
import com.claimwise.dto.RetrievalResponseDto;

public interface RetrievalService {

    RetrievalResponseDto search(RetrievalRequestDto requestDto);
}
