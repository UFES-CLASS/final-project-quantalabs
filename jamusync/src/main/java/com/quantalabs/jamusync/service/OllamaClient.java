package com.quantalabs.jamusync.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Talks to the Google Gemini API to give jamu health advice.
public class OllamaClient {

    // Gemini API key.
    private static final String API_KEY = System.getenv("GCP_API_KEY");
    // Gemini generateContent endpoint. The key is added on the end.
    // (gemini-2.5-flash is what this API key has access + quota for.)
    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public String chat(String systemPrompt, String userMessage) {
        try {
            // Gemini wants a "contents" array of parts. We put the system
            // prompt and the user's question together in one text part.
            JsonObject part = new JsonObject();
            part.addProperty("text", "SYSTEM: " + systemPrompt + "\n\nUSER: " + userMessage);

            JsonArray parts = new JsonArray();
            parts.add(part);

            JsonObject content = new JsonObject();
            content.add("parts", parts);

            JsonArray contents = new JsonArray();
            contents.add(content);

            JsonObject requestBody = new JsonObject();
            requestBody.add("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + API_KEY))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Print the full response body for debugging.
            System.out.println("Gemini response (HTTP " + response.statusCode() + "): " + response.body());

            if (response.statusCode() != 200) {
                return "Gemini service unavailable (HTTP " + response.statusCode() + "). " +
                       "Please check the API key and try again.";
            }

            // Read candidates[0].content.parts[0].text from the response.
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.has("candidates")) {
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("content")) {
                        JsonObject contentObj = firstCandidate.getAsJsonObject("content");
                        if (contentObj.has("parts")) {
                            JsonArray responseParts = contentObj.getAsJsonArray("parts");
                            if (responseParts.size() > 0) {
                                JsonObject firstPart = responseParts.get(0).getAsJsonObject();
                                if (firstPart.has("text")) {
                                    return firstPart.get("text").getAsString().trim();
                                }
                            }
                        }
                    }
                }
            }
            return "No response from Gemini.";
        } catch (Exception e) {
            return "Could not connect to Gemini. " +
                   "Please check your internet connection and API key. (" + e.getMessage() + ")";
        }
    }

    public String getProductRecommendation(String productContext, String userQuery) {
        String systemPrompt = "You are a helpful Jamu (traditional Indonesian herbal drink) advisor for JamuSync. " +
            "Recommend products from the catalog based on the user's health needs. " +
            "Be concise, friendly, and mention specific product names from the catalog.\n\n" +
            productContext;
        return chat(systemPrompt, userQuery);
    }

    public String getHealthAdvice(String productContext, String userQuery) {
        String systemPrompt = "You are a Jamu health advisor for JamuSync. " +
            "Provide general wellness advice about traditional Indonesian herbal drinks. " +
            "Do not provide medical diagnoses. Reference products from this catalog when relevant:\n\n" +
            productContext;
        return chat(systemPrompt, userQuery);
    }
}
