package com.github.lernejo.korekto.grader.api;

public class ErrorTester {

    private int counter = 0;

    public void always_fails() {
        throw new IllegalArgumentException();
    }

    public void never_fails() {
    }

    public void first_call_fails_second_succeed() {
        if (counter == 0) {
            counter++;
            throw new IllegalArgumentException();
        }
    }

}
