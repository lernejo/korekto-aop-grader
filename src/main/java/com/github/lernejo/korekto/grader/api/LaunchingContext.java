package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.GradingContext;
import com.github.lernejo.korekto.toolkit.partgrader.MavenContext;
import org.apache.maven.cli.MavenExposer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class LaunchingContext extends GradingContext implements MavenContext {

    private final MavenExposer mavenExposer = new MavenExposer();
    private final IdentifierGenerator identifierGenerator = new IdentifierGenerator(getRandomSource());
    private boolean compilationFailed;
    private boolean testFailed;
    private List<URL> mavenClassPath;
    private ClassLoader mavenMainClassloader;

    public LaunchingContext(GradingConfiguration configuration) {
        super(configuration);
    }

    @Override
    public boolean hasCompilationFailed() {
        return compilationFailed;
    }

    @Override
    public boolean hasTestFailed() {
        return testFailed;
    }

    @Override
    public void markAsCompilationFailed() {
        compilationFailed = true;
    }

    @Override
    public void markAsTestFailed() {
        testFailed = true;
    }

    public Optional<Path> detectJarPath() {
        try (Stream<Path> targetContent = Files.list(getExercise().getRoot().resolve("target"))) {
            return
                targetContent.filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.toString().endsWith(".jar"))
                    .findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ClassLoader getMavenMainClassloader() {
        initClassPathAndClassLoader();
        return mavenMainClassloader;
    }

    public List<URL> getMavenClassPath() {
        initClassPathAndClassLoader();
        return List.copyOf(mavenClassPath);
    }

    private void initClassPathAndClassLoader() {
        if (mavenClassPath == null) {
            mavenClassPath = new ArrayList<>(MavenClassloader.getMavenClassPath(mavenExposer, getExercise()));
            mavenClassPath.add(getCurrentCompiledClasses()); // So we can access our classes such as ErrorTester

            mavenMainClassloader = MavenClassloader.buildIsolatedClassLoader(mavenClassPath);
        }
    }

    private URL getCurrentCompiledClasses() {
        try {
            return Paths.get("").toAbsolutePath().resolve("target").resolve("classes").toUri().toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String newTypeId() {
        return identifierGenerator.generateId(true);
    }

    public String newId() {
        return identifierGenerator.generateId(false);
    }
}
