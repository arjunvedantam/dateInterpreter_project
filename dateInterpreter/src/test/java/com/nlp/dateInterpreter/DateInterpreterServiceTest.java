package com.nlp.dateInterpreter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nlp.dateInterpreter.exception.DateInterpretationException;

class DateInterpreterServiceTest {

    private final DateInterpreterService service = new DateInterpreterService(
            WebClient.builder(),
            "https://api.openai.com/v1/chat/completions",
            "test-key",
            "gpt-4o-mini",
            new ObjectMapper()
    );

    @Test
    void buildPromptIncludesCurrentDateContextForRequestedTimezone() {
        String prompt = service.buildPrompt("today", "Asia/Kolkata");

        assertTrue(prompt.contains("Use the provided current date/time as the reference point"));
        assertTrue(prompt.contains("Current date/time:"));
        assertTrue(prompt.contains("Current date:"));
        assertTrue(prompt.contains("Current day of week:"));
        assertTrue(prompt.contains("Timezone: Asia/Kolkata"));
        assertTrue(prompt.contains("Original: today"));
    }

    @Test
    void buildPromptDefaultsTimezoneToUtc() {
        String prompt = service.buildPrompt("tomorrow", null);

        assertTrue(prompt.contains("Timezone: UTC"));
    }

    @Test
    void buildPromptRejectsInvalidTimezone() {
        DateInterpretationException exception = assertThrows(
                DateInterpretationException.class,
                () -> service.buildPrompt("today", "Not/A-Timezone")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getDetail().contains("Unsupported timezone"));
    }
}
