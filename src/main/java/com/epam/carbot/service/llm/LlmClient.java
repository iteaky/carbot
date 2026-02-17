package com.epam.carbot.service.llm;

import com.epam.carbot.dto.generate.GenerateRequest;
import com.epam.carbot.dto.generate.GenerateResponse;

public interface LlmClient {
    GenerateResponse generate(GenerateRequest request);
}
