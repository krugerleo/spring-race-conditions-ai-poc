package com.gateway.ai.infrastructure.adapters.in;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.PromptQuery;
import com.gateway.ai.infrastructure.adapters.in.dto.PromptRequestDTO;
import com.gateway.ai.infrastructure.adapters.in.dto.PromptResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class PromptController {

    private final ProcessPromptUseCase processPromptUseCase;

    public PromptController(ProcessPromptUseCase processPromptUseCase) {
        this.processPromptUseCase = processPromptUseCase;
    }

    @PostMapping("/generate")
    public ResponseEntity<PromptResponseDTO> generate(@RequestBody PromptRequestDTO promptRequestDTO) {
        if (promptRequestDTO == null || promptRequestDTO.prompt() == null) {
            throw new IllegalArgumentException("O request ou o prompt não podem ser nulos.");
        }

        PromptQuery promptQuery = new PromptQuery(promptRequestDTO.prompt());
        AiAnswer response = processPromptUseCase.process(promptQuery);

        return ResponseEntity.ok()
                .header("X-Cache-Type", response.cacheType().name())
                .body(new PromptResponseDTO(response.answer()));
    }
}
