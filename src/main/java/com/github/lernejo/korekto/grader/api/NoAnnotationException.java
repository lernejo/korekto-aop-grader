package com.github.lernejo.korekto.grader.api;

public class NoAnnotationException extends RuntimeException {
    public NoAnnotationException(ClassNotFoundException cause) {
        super(cause);
    }
}
