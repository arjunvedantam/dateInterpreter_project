package com.nlp.dateInterpreter;

import com.nlp.dateInterpreter.dto.InterpretRequest;
import com.nlp.dateInterpreter.model.DateInterpreter;
import com.nlp.dateInterpreter.repository.DateInterpreterRepository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/date")
public class DateInterpreterController {
    private final DateInterpreterService service;
    private final DateInterpreterRepository repo;

    @PostMapping("/interpret")
    public Mono<JsonNode> interpret(@RequestBody InterpretRequest request) {
        return service.interpretText(request.text(), request.timezone())
                .flatMap(json -> {
                    DateInterpreter q = DateInterpreter.builder()
                            .userInput(request.text())
                            .jsonResponse(json.toString())
                            .build();
                    repo.save(q);
                    return Mono.just(json);
                });
    }

    @GetMapping("/history")
    public List<DateInterpreter> history() {
        return repo.findAllByOrderByCreatedAtDesc();
    }
}
