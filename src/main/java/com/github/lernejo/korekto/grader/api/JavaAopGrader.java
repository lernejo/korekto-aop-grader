package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.grader.api.parts.Part1Grader;
import com.github.lernejo.korekto.toolkit.Grader;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.partgrader.JacocoCoveragePartGrader;
import com.github.lernejo.korekto.toolkit.partgrader.MavenCompileAndTestPartGrader;

import java.util.Collection;
import java.util.List;

public class JavaAopGrader implements Grader<LaunchingContext> {

    @Override
    public String name() {
        return "AOP exercise";
    }

    @Override
    public LaunchingContext gradingContext(GradingConfiguration configuration) {
        return new LaunchingContext(configuration);
    }

    public Collection<PartGrader<LaunchingContext>> graders() {
        return List.of(
            new MavenCompileAndTestPartGrader<>(
                "Compilation & Tests",
                4.0D),
            new Part1Grader("Part 1 - Retry dynamic proxy", 6.0),
            new JacocoCoveragePartGrader<>("Code Coverage", 20.0D, 1.0D)
        );
    }

    @Override
    public String slugToRepoUrl(String slug) {
        return "https://github.com/" + slug + "/java_aop_training";
    }

    @Override
    public boolean needsWorkspaceReset() {
        return true;
    }
}
