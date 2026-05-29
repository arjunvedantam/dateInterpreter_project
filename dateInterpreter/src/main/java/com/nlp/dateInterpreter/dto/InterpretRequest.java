package com.nlp.dateInterpreter.dto;

import jakarta.validation.constraints.NotBlank;

public record InterpretRequest(
        @NotBlank(message = "text is required") String text,
        String timezone
) {}