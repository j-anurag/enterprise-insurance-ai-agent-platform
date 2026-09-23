package com.claimwise.service;

import java.util.List;

public interface EmbeddingService {

    List<Double> generateEmbedding(String text);

    int getDimensions();

    String getModelName();
}
