package com.raspel.erp.service.sistem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String sendQuery(String provider, String model, String apiKey, String systemPrompt, String userPrompt) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider is null");
        }
        
        try {
            switch (provider.toUpperCase()) {
                case "OPENAI":
                    return sendOpenAIRequest(model, apiKey, systemPrompt, userPrompt);
                case "GOOGLE":
                    return sendGoogleRequest(model, apiKey, systemPrompt, userPrompt);
                case "ANTHROPIC":
                    return sendAnthropicRequest(model, apiKey, systemPrompt, userPrompt);
                default:
                    throw new IllegalArgumentException("Unsupported AI provider: " + provider);
            }
        } catch (Exception e) {
            log.error("LLM Client call failed for provider {}", provider, e);
            throw new RuntimeException("LLM yanit alinamadi: " + e.getMessage(), e);
        }
    }

    private String sendOpenAIRequest(String model, String apiKey, String systemPrompt, String userPrompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model != null ? model : "gpt-4o");
        body.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        
        Map responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        return "Bos yanit alindi.";
    }

    private String sendGoogleRequest(String model, String apiKey, String systemPrompt, String userPrompt) {
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
            model != null ? model : "gemini-2.5-flash", apiKey);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
             body.put("systemInstruction", Map.of(
                 "parts", List.of(Map.of("text", systemPrompt))
             ));
        }
        body.put("contents", List.of(
            Map.of("parts", List.of(Map.of("text", userPrompt)))
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        
        Map responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (!parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        }
        return "Bos yanit alindi.";
    }

    private String sendAnthropicRequest(String model, String apiKey, String systemPrompt, String userPrompt) {
        String url = "https://api.anthropic.com/v1/messages";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model != null ? model : "claude-3-sonnet-20240229");
        body.put("max_tokens", 1024);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }
        body.put("messages", List.of(
            Map.of("role", "user", "content", userPrompt)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        
        Map responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("content")) {
            List<Map<String, Object>> contents = (List<Map<String, Object>>) responseBody.get("content");
            if (!contents.isEmpty()) {
                return (String) contents.get(0).get("text");
            }
        }
        return "Bos yanit alindi.";
    }
}
