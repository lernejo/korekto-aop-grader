package com.github.lernejo.korekto.grader.api;

public class NoRetryAnnotation extends RuntimeException {
    public NoRetryAnnotation(ClassNotFoundException cause) {
        super(cause);
    }
}
