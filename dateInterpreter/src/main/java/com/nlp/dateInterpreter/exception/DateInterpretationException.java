package com.nlp.dateInterpreter.exception;

import org.springframework.http.HttpStatus;

public class DateInterpretationException extends RuntimeException {
    private final HttpStatus status;
    private final String detail;

    public DateInterpretationException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public DateInterpretationException(HttpStatus status, String message, String detail) {
        this(status, message, detail, null);
    }

    public DateInterpretationException(HttpStatus status, String message, String detail, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }
}
