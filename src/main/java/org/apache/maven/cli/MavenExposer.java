package org.apache.maven.cli;

import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.project.*;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

import java.nio.file.Path;
import java.util.List;

@SubjectForToolkitInclusion
public class MavenExposer {

    private final PlexusContainer container;
    private final DefaultRepositorySystemSession repoSession;
    private final MavenExecutionRequest requestTemplate;
    private final ProjectBuilder projectBuilder;
    private final ProjectDependenciesResolver projectDependenciesResolver;

    public MavenExposer() {
        MavenCli mavenCli = new MavenCli();
        ClassWorld classWorld = new ClassWorld();
        try {
            classWorld.newRealm("plexus.core");

            CliRequest cliRequest = new CliRequest(new String[]{"validate"}, classWorld);
            mavenCli.cli(cliRequest);
            mavenCli.logging(cliRequest);
            container = mavenCli.container(cliRequest);
            DefaultRepositorySystemSessionFactory sessionFactory = container.lookup(DefaultRepositorySystemSessionFactory.class);
            MavenExecutionRequestPopulator executionRequestPopulator = container.lookup(MavenExecutionRequestPopulator.class);
            executionRequestPopulator.populateDefaults(cliRequest.request);

            requestTemplate = cliRequest.request;
            repoSession = sessionFactory.newRepositorySession(cliRequest.request);
            projectBuilder = container.lookup(ProjectBuilder.class);
            projectDependenciesResolver = container.lookup(ProjectDependenciesResolver.class);
        } catch (Exception e) {
            // yes... MavenCli#cli throws Exception
            throw new RuntimeException(e);
        }
    }

    public List<Dependency> resolveEffectiveRuntimeDependencies(Path pomFile) {
        MavenExecutionRequest oneShotRequest = DefaultMavenExecutionRequest.copy(requestTemplate);
        oneShotRequest.setPom(pomFile.toFile());

        ProjectBuildingRequest buildingRequest = requestTemplate.getProjectBuildingRequest();
        try {
            ProjectBuildingResult buildingResult = projectBuilder.build(pomFile.toFile(), buildingRequest);
            DefaultDependencyResolutionRequest depResolutionRequest =
                new DefaultDependencyResolutionRequest(buildingResult.getProject(), repoSession);
            depResolutionRequest.setResolutionFilter(new ScopeDependencyFilter("test"));
            DependencyResolutionResult result = projectDependenciesResolver.resolve(depResolutionRequest);
            return result.getDependencies();
        } catch (ProjectBuildingException e) {
            throw new RuntimeException(e);
        } catch (DependencyResolutionException e) {
            throw new RuntimeException(e);
        }
    }
}
