package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmClientService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient streamingClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

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

    /**
     * Görüntü (base64) + metin ile çok modlu (vision) sorgu gönderir.
     * OCR/fatura-fiş okuma gibi görüntü analizi için kullanılır.
     */
    public String sendVisionQuery(String provider, String model, String apiKey, String systemPrompt, String userPrompt, String base64Image, String mimeType) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider is null");
        }
        try {
            switch (provider.toUpperCase()) {
                case "OPENAI":
                    return sendOpenAIVisionRequest(model, apiKey, systemPrompt, userPrompt, base64Image, mimeType);
                case "GOOGLE":
                    return sendGoogleVisionRequest(model, apiKey, systemPrompt, userPrompt, base64Image, mimeType);
                case "ANTHROPIC":
                    return sendAnthropicVisionRequest(model, apiKey, systemPrompt, userPrompt, base64Image, mimeType);
                default:
                    throw new IllegalArgumentException("Unsupported AI provider: " + provider);
            }
        } catch (Exception e) {
            log.error("LLM vision call failed for provider {}", provider, e);
            throw new RuntimeException("LLM görüntü yaniti alinamadi: " + e.getMessage(), e);
        }
    }

    private String sendOpenAIVisionRequest(String model, String apiKey, String systemPrompt, String userPrompt, String base64Image, String mimeType) {
        String url = "https://api.openai.com/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> content = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            content.add(Map.of("type", "text", "text", systemPrompt));
        }
        content.add(Map.of("type", "text", "text", userPrompt));
        content.add(Map.of("type", "image_url", "image_url", Map.of(
                "url", "data:" + mimeType + ";base64," + base64Image
        )));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model != null ? model : "gpt-4o");
        body.put("messages", List.of(Map.of("role", "user", "content", content)));

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

    private String sendGoogleVisionRequest(String model, String apiKey, String systemPrompt, String userPrompt, String base64Image, String mimeType) {
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model != null ? model : "gemini-2.5-flash", apiKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> parts = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            parts.add(Map.of("text", systemPrompt));
        }
        parts.add(Map.of("text", userPrompt));
        parts.add(Map.of("inline_data", Map.of(
                "mime_type", mimeType,
                "data", base64Image
        )));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(Map.of("parts", parts)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");
                if (!partsList.isEmpty()) {
                    return (String) partsList.get(0).get("text");
                }
            }
        }
        return "Bos yanit alindi.";
    }

    private String sendAnthropicVisionRequest(String model, String apiKey, String systemPrompt, String userPrompt, String base64Image, String mimeType) {
        String url = "https://api.anthropic.com/v1/messages";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", userPrompt));
        content.add(Map.of("type", "image", "source", Map.of(
                "type", "base64",
                "media_type", mimeType,
                "data", base64Image
        )));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model != null ? model : "claude-3-sonnet-20240229");
        body.put("max_tokens", 1024);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }
        body.put("messages", List.of(Map.of("role", "user", "content", content)));

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

    /**
     * LLM yanıtını akış (streaming) olarak okur ve her token parçasını onToken
     * geri çağrısına iletir. SSE (Server-Sent Events) tabanlı akış kullanılır.
     */
    public void streamQuery(String provider, String model, String apiKey, String systemPrompt, String userPrompt, Consumer<String> onToken) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider is null");
        }
        switch (provider.toUpperCase()) {
            case "OPENAI":
                streamOpenAI(model, apiKey, systemPrompt, userPrompt, onToken);
                return;
            case "GOOGLE":
                streamGoogle(model, apiKey, systemPrompt, userPrompt, onToken);
                return;
            case "ANTHROPIC":
                streamAnthropic(model, apiKey, systemPrompt, userPrompt, onToken);
                return;
            default:
                throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        }
    }

    private void streamOpenAI(String model, String apiKey, String systemPrompt, String userPrompt, Consumer<String> onToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model != null ? model : "gpt-4o");
        body.put("stream", true);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body)))
                .build();

        streamSse(request, (data, consumer) -> {
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                if (delta.isTextual() && !delta.asText().isEmpty()) {
                    consumer.accept(delta.asText());
                }
            } catch (Exception ignored) {
                // Tek parçalık JSON hatası akışı durdurmaz
            }
        }, onToken);
    }

    private void streamGoogle(String model, String apiKey, String systemPrompt, String userPrompt, Consumer<String> onToken) {
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?alt=sse&key=%s",
                model != null ? model : "gemini-2.5-flash", apiKey);

        Map<String, Object> body = new HashMap<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body)))
                .build();

        streamSse(request, (data, consumer) -> {
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode text = node.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                if (text.isTextual() && !text.asText().isEmpty()) {
                    consumer.accept(text.asText());
                }
            } catch (Exception ignored) {
            }
        }, onToken);
    }

    private void streamAnthropic(String model, String apiKey, String systemPrompt, String userPrompt, Consumer<String> onToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model != null ? model : "claude-3-sonnet-20240229");
        body.put("max_tokens", 1024);
        body.put("stream", true);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body)))
                .build();

        streamSse(request, (data, consumer) -> {
            try {
                JsonNode node = objectMapper.readTree(data);
                String type = node.path("type").asText();
                if ("content_block_delta".equals(type)) {
                    JsonNode text = node.path("delta").path("text");
                    if (text.isTextual() && !text.asText().isEmpty()) {
                        consumer.accept(text.asText());
                    }
                }
            } catch (Exception ignored) {
            }
        }, onToken);
    }

    private void streamSse(HttpRequest request, java.util.function.BiConsumer<String, Consumer<String>> lineParser, Consumer<String> onToken) {
        try {
            HttpResponse<Stream<String>> response = streamingClient.send(request, HttpResponse.BodyHandlers.ofLines());
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> {
                    if (line == null) return;
                    String trimmed = line.trim();
                    if (trimmed.startsWith("data:")) {
                        String data = trimmed.substring(5).trim();
                        if ("[DONE]".equals(data)) return;
                        lineParser.accept(data, onToken);
                    }
                });
            }
        } catch (Exception e) {
            log.error("LLM akış okunamadı: {}", e.getMessage(), e);
            throw new RuntimeException("LLM akış okunamadı: " + e.getMessage(), e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("JSON yazılamadı: " + e.getMessage(), e);
        }
    }
}
