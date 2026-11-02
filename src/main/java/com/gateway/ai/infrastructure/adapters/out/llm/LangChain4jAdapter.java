
package com.gateway.ai.infrastructure.adapters.out.llm;

import com.gateway.ai.application.ports.out.LlmServicePort;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LangChain4jAdapter implements LlmServicePort {

    private ChatModel geminiChatModel;

    public LangChain4jAdapter(@Value("${LLM_API_KEY}") String apiKey) {
        this.geminiChatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-3.1-flash-lite")
                .build();
    }

    @Override
    public String generate(String prompt) {
        return geminiChatModel.chat(prompt);
    }

}
