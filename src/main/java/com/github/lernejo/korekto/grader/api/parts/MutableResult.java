package com.github.lernejo.korekto.grader.api.parts;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.ArrayList;
import java.util.List;

record MutableResult(AtomicDouble grade, List<String> messages) {

    static MutableResult maxGrade(double maxGrade) {
        return new MutableResult(new AtomicDouble(maxGrade), new ArrayList<>());
    }
}
