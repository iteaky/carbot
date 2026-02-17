package com.epam.carbot.service.llm;

import com.epam.carbot.dto.generate.GenerateRequest;
import com.epam.carbot.dto.generate.GenerateResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PlaywrightLlmClient implements LlmClient {

    private final RestClient restClient;

    public PlaywrightLlmClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public GenerateResponse generate(GenerateRequest request) {
        try {
            return restClient.post()
                    .uri("/generate")
                    .body(request)
                    .retrieve()
                    .body(GenerateResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                throw new LlmBusyException("LLM API busy");
            }
            if (e.getStatusCode().value() == 422) {
                throw new LlmInvalidRequestException("Invalid LLM request");
            }
            throw new LlmServiceException("LLM API error", e);
        } catch (Exception e) {
            throw new LlmServiceException("LLM API error", e);
        }
    }
}
