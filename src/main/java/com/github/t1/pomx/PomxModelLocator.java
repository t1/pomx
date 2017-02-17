package com.github.t1.pomx;

import lombok.SneakyThrows;
import org.apache.maven.model.locator.ModelLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.component.annotations.*;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.*;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.*;

import java.io.File;
import java.nio.file.*;

@Component(role = ModelLocator.class)
public class PomxModelLocator implements ModelLocator {
    static final Path REPOSITORY = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");

    @Requirement
    Logger log;

    @Requirement
    RepositorySystem repositorySystem;

    @Override public File locatePom(File dir) {
        Path pom = dir.toPath().resolve("pom.xml");
        Path pomx = dir.toPath().resolve("pomx.xml");
        if (Files.exists(pomx)) {
            log.info("convert " + pomx + " to " + pom);
            ProjectObjectModel.readFrom(pomx, resolver).writeTo(pom);
        }
        return pom.toFile();
    }


    private final Resolver resolver = this::resolve;

    @SneakyThrows(ArtifactResolutionException.class)
    private Path resolve(GAV gav, String type) {
        RepositorySystemSession session = newRepositorySystemSession();
        Artifact artifact = new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), type, gav.getVersion());
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        // RemoteRepository central = new RemoteRepository.Builder("central", "default", remoteRepository).build();
        // request.addRepository(central);
        ArtifactResult resolved = repositorySystem.resolveArtifact(session, request);
        return resolved.getArtifact().getFile().toPath();
    }

    private DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(REPOSITORY.toFile());
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));

        // session.setTransferListener(new ConsoleTransferListener());
        // session.setRepositoryListener(new ConsoleRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }
}
