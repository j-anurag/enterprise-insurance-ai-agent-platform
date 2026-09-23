package com.claimwise.service;

import com.claimwise.exception.ExternalServiceException;
import com.claimwise.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqLlmServiceTest {

        private MockRestServiceServer mockServer;
        private GroqLlmService groqLlmService;

        @BeforeEach
        void setUp() {
                RestClient.Builder builder = RestClient.builder();
                mockServer = MockRestServiceServer.bindTo(builder).build();
                RestClient restClient = builder.build();

                groqLlmService = new GroqLlmService(
                                "mock-groq-api-key",
                                "llama-3.3-70b-versatile",
                                "https://api.groq.com/openai/v1",
                                30,
                                0.0,
                                1024,
                                restClient);
        }

        @Test
        @DisplayName("Should successfully parse generated answer from Groq chat completions response")
        void testGenerateAnswer_Success() {
                String mockJsonResponse = """
                                {
                                  "id": "chatcmpl-12345",
                                  "object": "chat.completion",
                                  "created": 1700000000,
                                  "model": "llama-3.3-70b-versatile",
                                  "choices": [
                                    {
                                      "index": 0,
                                      "message": {
                                        "role": "assistant",
                                        "content": "Water damage is covered up to $25,000 under section 4."
                                      },
                                      "finish_reason": "stop"
                                    }
                                  ],
                                  "usage": {
                                    "prompt_tokens": 120,
                                    "completion_tokens": 35,
                                    "total_tokens": 155
                                  }
                                }
                                """;

                mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                                .andExpect(method(HttpMethod.POST))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer mock-groq-api-key"))
                                .andExpect(jsonPath("$.model").value("llama-3.3-70b-versatile"))
                                .andExpect(jsonPath("$.temperature").value(0.0))
                                .andExpect(jsonPath("$.max_completion_tokens").value(1024))
                                .andExpect(jsonPath("$.messages[0].role").value("system"))
                                .andExpect(jsonPath("$.messages[0].content").value("You are an assistant."))
                                .andExpect(jsonPath("$.messages[1].role").value("user"))
                                .andExpect(jsonPath("$.messages[1].content").value("Is water covered?"))
                                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

                String answer = groqLlmService.generateAnswer("You are an assistant.", "Is water covered?");

                assertThat(answer).isEqualTo("Water damage is covered up to $25,000 under section 4.");
                mockServer.verify();
        }

        @Test
        @DisplayName("Should throw InvalidRequestException if Groq API key is missing or blank")
        void testGenerateAnswer_MissingApiKey() {
                GroqLlmService serviceWithoutKey = new GroqLlmService(
                                "",
                                "llama-3.3-70b-versatile",
                                "https://api.groq.com/openai/v1",
                                30,
                                0.0,
                                1024,
                                RestClient.builder().build());

                assertThatThrownBy(() -> serviceWithoutKey.generateAnswer("System", "User"))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Groq API key is not configured");
        }

        @Test
        @DisplayName("Should map HTTP 401 Unauthorized to ExternalServiceException with 502 status")
        void testGenerateAnswer_401Unauthorized() {
                mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                                                .body("{\"error\":\"Invalid API key\"}"));

                assertThatThrownBy(() -> groqLlmService.generateAnswer("System", "User"))
                                .isInstanceOf(ExternalServiceException.class)
                                .satisfies(ex -> {
                                        ExternalServiceException ese = (ExternalServiceException) ex;
                                        assertThat(ese.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                                        assertThat(ese.getMessage()).contains("Groq API authentication failed");
                                });

                mockServer.verify();
        }

        @Test
        @DisplayName("Should map HTTP 429 Rate Limit to ExternalServiceException with 429 status")
        void testGenerateAnswer_429RateLimit() {
                mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                                                .body("{\"error\":\"Rate limit reached\"}"));

                assertThatThrownBy(() -> groqLlmService.generateAnswer("System", "User"))
                                .isInstanceOf(ExternalServiceException.class)
                                .satisfies(ex -> {
                                        ExternalServiceException ese = (ExternalServiceException) ex;
                                        assertThat(ese.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                                        assertThat(ese.getMessage()).contains("rate limit exceeded");
                                });

                mockServer.verify();
        }

        @Test
        @DisplayName("Should map HTTP 500 Server Error to ExternalServiceException with 502 status")
        void testGenerateAnswer_500ServerError() {
                mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withServerError());

                assertThatThrownBy(() -> groqLlmService.generateAnswer("System", "User"))
                                .isInstanceOf(ExternalServiceException.class)
                                .satisfies(ex -> {
                                        ExternalServiceException ese = (ExternalServiceException) ex;
                                        assertThat(ese.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                                        assertThat(ese.getMessage()).contains("upstream service error");
                                });

                mockServer.verify();
        }

        @Test
        @DisplayName("Should map empty response body to ExternalServiceException with 502 status")
        void testGenerateAnswer_EmptyResponse() {
                mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

                assertThatThrownBy(() -> groqLlmService.generateAnswer("System", "User"))
                                .isInstanceOf(ExternalServiceException.class)
                                .satisfies(ex -> {
                                        ExternalServiceException ese = (ExternalServiceException) ex;
                                        assertThat(ese.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                                        assertThat(ese.getMessage()).contains("Empty response received from Groq API");
                                });

                mockServer.verify();
        }

        @Test
        @DisplayName("Should map malformed JSON without choices to ExternalServiceException")
        void testGenerateAnswer_MalformedJsonChoices() {
                mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess("{\"id\":\"123\",\"choices\":[]}", MediaType.APPLICATION_JSON));

                assertThatThrownBy(() -> groqLlmService.generateAnswer("System", "User"))
                                .isInstanceOf(ExternalServiceException.class)
                                .satisfies(ex -> {
                                        ExternalServiceException ese = (ExternalServiceException) ex;
                                        assertThat(ese.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                                        assertThat(ese.getMessage()).contains("missing choices array");
                                });

                mockServer.verify();
        }
}
