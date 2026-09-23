package com.claimwise.service;

import com.claimwise.exception.ExternalServiceException;
import com.claimwise.exception.InvalidRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "claimwise.ai.llm.provider", havingValue = "groq", matchIfMissing = true)
public class GroqLlmService implements LlmService {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final double temperature;
    private final int maxCompletionTokens;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GroqLlmService(
            @Value("${claimwise.ai.llm.groq.api-key:}") String apiKey,
            @Value("${claimwise.ai.llm.groq.model:llama-3.3-70b-versatile}") String model,
            @Value("${claimwise.ai.llm.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${claimwise.ai.llm.groq.timeout-seconds:30}") int timeoutSeconds,
            @Value("${claimwise.ai.llm.groq.temperature:0.0}") double temperature,
            @Value("${claimwise.ai.llm.groq.max-completion-tokens:1024}") int maxCompletionTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.temperature = temperature;
        this.maxCompletionTokens = maxCompletionTokens;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public GroqLlmService(String apiKey, String model, String baseUrl, int timeoutSeconds,
                          double temperature, int maxCompletionTokens, RestClient restClient) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.temperature = temperature;
        this.maxCompletionTokens = maxCompletionTokens;
        this.objectMapper = new ObjectMapper();
        this.restClient = restClient;
    }

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidRequestException("Groq API key is not configured. Please set GROQ_API_KEY environment variable.");
        }

        String endpointUrl = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

        Map<String, Object> requestPayload = Map.of(
                "model", model,
                "temperature", temperature,
                "max_completion_tokens", maxCompletionTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        log.debug("Sending chat completion request to Groq: model={}, endpoint={}", model, endpointUrl);

        try {
            String responseBody = restClient.post()
                    .uri(endpointUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            return parseChatResponse(responseBody);

        } catch (RestClientResponseException ex) {
            log.error("Groq API HTTP error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode().value() == 401) {
                throw new ExternalServiceException("Groq API authentication failed. Please verify GROQ_API_KEY.", HttpStatus.BAD_GATEWAY);
            } else if (ex.getStatusCode().value() == 429) {
                throw new ExternalServiceException("Groq API rate limit exceeded. Please retry later.", HttpStatus.TOO_MANY_REQUESTS);
            } else if (ex.getStatusCode().is5xxServerError()) {
                throw new ExternalServiceException("Groq upstream service error: " + ex.getStatusCode(), HttpStatus.BAD_GATEWAY);
            } else {
                throw new ExternalServiceException("Groq API request failed with status: " + ex.getStatusCode(), HttpStatus.BAD_GATEWAY);
            }
        } catch (ResourceAccessException ex) {
            log.error("Groq API network or timeout error: {}", ex.getMessage());
            throw new ExternalServiceException("Groq API request timed out or network error", ex, HttpStatus.GATEWAY_TIMEOUT);
        } catch (InvalidRequestException | ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during Groq LLM generation: {}", ex.getMessage(), ex);
            throw new ExternalServiceException("Failed to generate answer from Groq: " + ex.getMessage(), ex, HttpStatus.BAD_GATEWAY);
        }
    }

    private String parseChatResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new ExternalServiceException("Empty response received from Groq API", HttpStatus.BAD_GATEWAY);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                String errorMsg = root.get("error").has("message")
                        ? root.get("error").get("message").asText()
                        : root.get("error").asText();
                throw new ExternalServiceException("Groq API error: " + errorMsg, HttpStatus.BAD_GATEWAY);
            }

            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new ExternalServiceException("Malformed response from Groq API: missing choices array", HttpStatus.BAD_GATEWAY);
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode messageNode = firstChoice.get("message");
            if (messageNode == null || !messageNode.has("content")) {
                throw new ExternalServiceException("Malformed response from Groq API: missing message content", HttpStatus.BAD_GATEWAY);
            }

            String content = messageNode.get("content").asText();

            if (root.has("usage")) {
                JsonNode usage = root.get("usage");
                log.debug("Groq token usage: prompt_tokens={}, completion_tokens={}, total_tokens={}",
                        usage.path("prompt_tokens").asInt(),
                        usage.path("completion_tokens").asInt(),
                        usage.path("total_tokens").asInt());
            }

            return content.trim();

        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse Groq response JSON", ex);
            throw new ExternalServiceException("Failed to parse Groq response JSON: " + ex.getMessage(), ex, HttpStatus.BAD_GATEWAY);
        }
    }
}
