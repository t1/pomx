package com.github.t1.pomx;

import org.apache.maven.model.locator.ModelLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.component.annotations.*;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.*;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.RemoteRepository;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.nio.file.*;

@Component(role = ModelLocator.class)
public class PomxModelLocator implements ModelLocator {
    static final Path REPOSITORY = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");

    @Requirement Logger log;
    // lazy: eager injection of RepositorySystem would create a circular dependency
    // (RepositorySystem -> ... -> ModelProcessor -> ModelLocator -> RepositorySystem)
    @Inject Provider<RepositorySystem> repositorySystemProvider;

    private RepositorySystem repositorySystem() { return repositorySystemProvider.get(); }

    @Override public File locatePom(File dir) {
        Path pom = dir.toPath().resolve("pom.xml");
        Path pomx = dir.toPath().resolve("pomx.xml");
        if (Files.exists(pomx)) {
            log.info("convert " + pomx + " to " + pom);
            ProjectObjectModel.readFrom(pomx, this::resolve).writeTo(pom);
        }
        return pom.toFile();
    }


    private Path resolve(GAV gav, String type) {
        RepositorySystemSession session = newRepositorySystemSession();
        Artifact artifact = new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), type, gav.getVersion());
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.addRepository(new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2").build());
        try {
            ArtifactResult resolved = repositorySystem().resolveArtifact(session, request);
            return resolved.getArtifact().getFile().toPath();
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    private DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setOffline(false);

        LocalRepository localRepo = new LocalRepository(REPOSITORY.toFile());
        // SimpleLocalRepositoryManager: no _remote.repositories tracking, so locally
        // cached artifacts are always considered available
        try {
            session.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory().newInstance(session, localRepo));
        } catch (org.eclipse.aether.repository.NoLocalRepositoryManagerException e) {
            throw new RuntimeException(e);
        }

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }
}
