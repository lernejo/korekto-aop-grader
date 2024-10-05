package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.toolkit.misc.RandomSupplier;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record IdentifierGenerator(RandomSupplier rs) {

    public String generateId(boolean upper) {
        return IntStream.range(0, 5 + rs.nextInt(15))
            .mapToObj(i -> (i == 0 && !upper) || (i != 0 && mostlyFalse()) ? generateLower() : generateUpper())
            .collect(Collectors.joining());
    }

    private boolean mostlyFalse() {
        return rs.nextInt(100) < 80;
    }

    private String generateUpper() {
        return generateLetter('A');
    }

    private String generateLower() {
        return generateLetter('a');
    }

    private String generateLetter(char start) {
        return Character.toString(start + (rs.nextInt(200) % 25));
    }
}
