package com.tp4_omaressafi.tpwebomaressafi.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LlmClient {

    private String systemRole;
    private Assistant assistant;
    private ChatMemory chatMemory;

    public LlmClient() {
        String apiKey = System.getenv("GEMINI_KEY");

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-flash")
                .logRequestsAndResponses(true)
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();
    }

    public void setSystemRole(String role) {
        this.systemRole = role;
        chatMemory.clear();
        chatMemory.add(new SystemMessage(role));
    }

    public String ask(String prompt) {
        return assistant.chat(prompt);
    }
}