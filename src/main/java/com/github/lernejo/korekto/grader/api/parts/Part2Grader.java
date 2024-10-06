package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.Result;
import com.github.lernejo.korekto.grader.api.TypeSupplier;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.PartGrader;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Part2Grader(String name, Double maxGrade) implements PartGrader<LaunchingContext> {
    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.hasCompilationFailed()) {
            return result(List.of("Not trying due to previous compilation failure"), 0.0D);
        }

        ClassLoader mavenClassLoader = context.getMavenMainClassloader();
        ByteArrayClassLoader byteArrayClassLoader = new ByteArrayClassLoader(mavenClassLoader, false, Map.of(), ByteArrayClassLoader.PersistenceHandler.MANIFEST);

        var typeSupplier = TypeSupplier.springConfig();

        Result<TypeSupplier.SpringConfigType, String> potentialType = typeSupplier.supply(context, byteArrayClassLoader);
        if (!potentialType.isOk()) {
            return result(List.of(potentialType.err()), 0.0D);
        }

        return result(List.of(), maxGrade);
    }
}
