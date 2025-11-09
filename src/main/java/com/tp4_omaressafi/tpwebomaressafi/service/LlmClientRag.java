package com.tp4_omaressafi.tpwebomaressafi.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class LlmClientRag {

    private Assistant assistant;

    @Inject
    private MagasinEmbeddings magasinEmbeddings;

    public LlmClientRag() {

    }

    @jakarta.annotation.PostConstruct
    public void init() {
        String GEMINI_KEY = System.getenv("GEMINI_KEY");

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(GEMINI_KEY)
                .modelName("gemini-1.5-flash")
                .temperature(0.3)
                .logRequestsAndResponses(true) // Fonctionnalité Logging (Test 2)
                .build();

        // Récupérer le magasin et le modèle depuis le service injecté
        EmbeddingStore<TextSegment> store = magasinEmbeddings.getEmbeddingStore();
        EmbeddingModel embeddingModel = magasinEmbeddings.getEmbeddingModel();

        // 1. Créer le retriever pour le RAG local (PDFs initiaux + uploadés)
        ContentRetriever retrieverLocal = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(3) // 3 morceaux de contexte
                .minScore(0.5)
                .build();



        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                // On utilise .contentRetriever (RAG simple)
                .contentRetriever(retrieverLocal)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    public String ask(String question) {
        return assistant.chat(question);
    }
}