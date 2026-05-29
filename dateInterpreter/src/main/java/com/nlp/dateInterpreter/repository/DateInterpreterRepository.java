package com.nlp.dateInterpreter.repository;

import com.nlp.dateInterpreter.model.DateInterpreter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DateInterpreterRepository extends JpaRepository<DateInterpreter, Long> {
    List<DateInterpreter> findAllByOrderByCreatedAtDesc();
}