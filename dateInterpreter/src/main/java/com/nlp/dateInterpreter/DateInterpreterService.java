package com.nlp.dateInterpreter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nlp.dateInterpreter.exception.DateInterpretationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class DateInterpreterService {
    private static final DateTimeFormatter REFERENCE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String modelName;

    public DateInterpreterService(WebClient.Builder builder,
                                  @Value("${nl.model.endpoint}") String endpoint,
                                  @Value("${nl.model.api-key}") String apiKey,
                                  @Value("${nl.model.name}") String modelName,
                                  ObjectMapper mapper) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new DateInterpretationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Model API key is not configured",
                    "Set OPENAI_API_KEY, NL_MODEL_API_KEY, or GITHUB_TOKEN before starting the backend."
            );
        }

        this.mapper = mapper;
        this.modelName = modelName;
        this.webClient = builder
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public JsonNode interpretText(String text, String timezone) {
        String prompt = buildPrompt(text, timezone);

        ObjectNode body = mapper.createObjectNode();
        body.put("model", modelName);
        body.set("response_format", mapper.createObjectNode().put("type", "json_object"));

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

        JsonNode response = webClient.post()
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(bodyText -> new DateInterpretationException(
                                HttpStatus.BAD_GATEWAY,
                                "Model API request failed",
                                bodyText
                        )))
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new DateInterpretationException(
                    HttpStatus.BAD_GATEWAY,
                    "Model API returned an empty response"
            );
        }

        return extractJson(response);
    }

    String buildPrompt(String text, String timezone) {
        ZoneId zoneId = resolveZoneId(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        return """
            Interpret the following natural language date expression into strict JSON.
            Use the provided current date/time as the reference point for any relative expression like "today", "tomorrow", "next Friday", "last week", or "2 months from now".
            Respond ONLY with JSON in this format:
            {
              "date": "YYYY-MM-DD",
              "startDate": "YYYY-MM-DD",
              "endDate": "YYYY-MM-DD",
              "description": "explanation",
              "original": "<original>"
            }
            Current date/time: %s
            Current date: %s
            Current day of week: %s
            Timezone: %s
            Original: %s
        """.formatted(
                now.format(REFERENCE_TIMESTAMP_FORMATTER),
                now.toLocalDate(),
                now.getDayOfWeek(),
                zoneId.getId(),
                text
        );
    }

    private ZoneId resolveZoneId(String timezone) {
        String normalizedTimezone = timezone == null || timezone.isBlank() ? "UTC" : timezone.trim();
        try {
            return ZoneId.of(normalizedTimezone);
        } catch (DateTimeException ex) {
            throw new DateInterpretationException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid timezone",
                    "Unsupported timezone: " + normalizedTimezone,
                    ex
            );
        }
    }

    private JsonNode extractJson(JsonNode response) {
        try {
            JsonNode contentNode = response.at("/choices/0/message/content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new DateInterpretationException(
                        HttpStatus.BAD_GATEWAY,
                        "Model API returned an empty completion",
                        response.toString()
                );
            }

            return mapper.readTree(contentNode.asText());
        } catch (JsonProcessingException e) {
            throw new DateInterpretationException(
                    HttpStatus.BAD_GATEWAY,
                    "Model API returned invalid JSON",
                    response.toString(),
                    e
            );
        }
    }
}
