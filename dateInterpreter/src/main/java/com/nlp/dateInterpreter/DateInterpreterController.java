package com.nlp.dateInterpreter;

import com.nlp.dateInterpreter.dto.InterpretRequest;
import com.nlp.dateInterpreter.model.DateInterpreter;
import com.nlp.dateInterpreter.repository.DateInterpreterRepository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/date")
public class DateInterpreterController {
    private final DateInterpreterService service;
    private final DateInterpreterRepository repo;

    @PostMapping("/interpret")
    public JsonNode interpret(@Valid @RequestBody InterpretRequest request) {
        JsonNode json = service.interpretText(request.text(), request.timezone());

        DateInterpreter q = DateInterpreter.builder()
                .userInput(request.text())
                .jsonResponse(json)
                .build();
        repo.save(q);

        return json;
    }

    @GetMapping("/history")
    public List<DateInterpreter> history() {
        return repo.findAllByOrderByCreatedAtDesc();
    }
}
