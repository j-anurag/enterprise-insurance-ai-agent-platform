package com.claimwise.service;

import com.claimwise.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service("deterministicEmbeddingService")
@ConditionalOnProperty(name = "claimwise.ai.embedding.provider", havingValue = "deterministic")
public class DeterministicEmbeddingService implements EmbeddingService {

    private final int dimensions;

    public DeterministicEmbeddingService(
            @Value("${claimwise.ai.embedding.dimensions:384}") int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidRequestException("Text for embedding generation cannot be empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.trim().getBytes(StandardCharsets.UTF_8));

            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xff);
            }

            Random random = new Random(seed);
            List<Double> vector = new ArrayList<>(dimensions);
            double sumSquares = 0.0;

            for (int i = 0; i < dimensions; i++) {
                double val = (random.nextDouble() * 2.0) - 1.0;
                vector.add(val);
                sumSquares += val * val;
            }

            double norm = Math.sqrt(sumSquares);
            List<Double> normalizedVector = new ArrayList<>(dimensions);
            for (Double val : vector) {
                normalizedVector.add(val / norm);
            }

            return normalizedVector;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getModelName() {
        return "deterministic-test-model";
    }
}
