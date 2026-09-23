package com.claimwise.service;

import com.claimwise.exception.InvalidRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "claimwise.ai.embedding.provider", havingValue = "huggingface", matchIfMissing = true)
public class HuggingFaceEmbeddingService implements EmbeddingService {

    private final String model;
    private final String apiUrl;
    private final String apiKey;
    private final int dimensions;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceEmbeddingService(
            @Value("${claimwise.ai.embedding.model:BAAI/bge-small-en-v1.5}") String model,
            @Value("${claimwise.ai.embedding.api-url:https://api-inference.huggingface.co/pipeline/feature-extraction/BAAI/bge-small-en-v1.5}") String apiUrl,
            @Value("${claimwise.ai.embedding.api-key:}") String apiKey,
            @Value("${claimwise.ai.embedding.dimensions:384}") int dimensions) {
        this.model = model;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.dimensions = dimensions;
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidRequestException("Text for embedding generation cannot be empty");
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "inputs", text.trim(),
                    "options", Map.of("wait_for_model", true)
            );

            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
            }

            String responseBody = requestSpec
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseEmbeddingResponse(responseBody);

        } catch (RestClientResponseException ex) {
            log.error("Hugging Face API error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode().value() == 401) {
                throw new InvalidRequestException(
                        "Hugging Face API authentication failed. Please configure a valid HUGGINGFACE_API_KEY.");
            }
            throw new InvalidRequestException(
                    "Hugging Face embedding request failed (" + ex.getStatusCode() + "): " + ex.getStatusText());
        } catch (Exception ex) {
            log.error("Failed to generate embedding from Hugging Face", ex);
            throw new InvalidRequestException("Failed to generate embedding: " + ex.getMessage());
        }
    }

    private List<Double> parseEmbeddingResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new InvalidRequestException("Empty response received from Hugging Face embedding API");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.isArray()) {
                if (!rootNode.isEmpty() && rootNode.get(0).isArray()) {
                    rootNode = rootNode.get(0);
                }

                List<Double> vector = new ArrayList<>(rootNode.size());
                for (JsonNode element : rootNode) {
                    vector.add(element.asDouble());
                }

                if (vector.size() != dimensions) {
                    throw new InvalidRequestException(String.format(
                            "Embedding dimension mismatch: expected %d from model %s, but received %d",
                            dimensions, model, vector.size()));
                }

                return vector;
            } else if (rootNode.has("error")) {
                throw new InvalidRequestException("Hugging Face error: " + rootNode.get("error").asText());
            } else {
                throw new InvalidRequestException("Unexpected response format from Hugging Face embedding API");
            }
        } catch (InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidRequestException("Failed to parse embedding JSON response: " + ex.getMessage());
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getModelName() {
        return model;
    }
}
