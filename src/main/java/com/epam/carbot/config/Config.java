package com.epam.carbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class Config {

    @Value("${llm.url}")
    private String llmUrl;

    @Bean
    public RestClient llmRestClient() {
        return RestClient.builder()
                .baseUrl(llmUrl)
                .build();
    }
}
