package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.service.OllamaClient;
import com.quantalabs.jamusync.service.RecommendationService;

public class ChatbotController {

    private final OllamaClient ollamaClient = new OllamaClient();
    private final RecommendationService recommendationService = new RecommendationService();

    public String sendChatMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Please enter a question.";
        }
        String context = recommendationService.buildProductContext();
        return ollamaClient.getHealthAdvice(context, userMessage.trim());
    }

    public String getRecommendations(String userQuery) {
        String query = (userQuery == null || userQuery.trim().isEmpty())
            ? "Recommend jamu products for general wellness and immunity"
            : userQuery.trim();
        String context = recommendationService.buildProductContext();
        return ollamaClient.getProductRecommendation(context, query);
    }
}
