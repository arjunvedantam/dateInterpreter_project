package com.nlp.dateInterpreter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DateInterpreterService {
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public DateInterpreterService(WebClient.Builder builder,
                                  @Value("${nl.model.endpoint}") String endpoint,
                                  @Value("${nl.model.apiKey}") String apiKey) {

        this.webClient = builder
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<JsonNode> interpretText(String text, String timezone) {

        String prompt = buildPrompt(text, timezone);

        ObjectNode body = mapper.createObjectNode();
        body.put("model", "gpt-4o-mini");

        ArrayNode messages = mapper.createArrayNode();
        messages.add(
                mapper.createObjectNode()
                        .put("role", "system")
                        .put("content", "You are a natural language date interpreter. Always respond with valid JSON only — no markdown, no code blocks, just raw JSON.")
        );
        messages.add(
                mapper.createObjectNode()
                        .put("role", "user")
                        .put("content", prompt)
        );
        body.set("messages", messages);

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::extractJson);
    }

    private String buildPrompt(String text, String timezone) {
        return """
            Interpret the following natural language date expression into strict JSON.
            Respond ONLY with JSON in this format:
            {
              "date": "YYYY-MM-DD",
              "startDate": "YYYY-MM-DD",
              "endDate": "YYYY-MM-DD",
              "description": "explanation",
              "original": "<original>"
            }
            Timezone: %s
            Original: %s
        """.formatted(timezone == null ? "UTC" : timezone, text);
    }

    private JsonNode extractJson(JsonNode response) {
        try {
            String content = response.path("choices").get(0)
                    .path("message").path("content").asText();

            return mapper.readTree(content);
        } catch (Exception e) {
            ObjectNode err = mapper.createObjectNode();
            err.put("error", "PARSE_ERROR");
            err.put("raw", response.toString());
            return err;
        }
    }
}
