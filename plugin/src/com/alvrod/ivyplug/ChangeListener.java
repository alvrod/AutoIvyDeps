package com.alvrod.ivyplug;

import jetbrains.buildServer.BuildType;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.dependency.Dependency;
import jetbrains.buildServer.serverSide.dependency.DependencyFactory;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.vcs.VcsFileModification;
import jetbrains.buildServer.vcs.VcsModification;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;

public class ChangeListener extends BuildServerAdapter {
    @NotNull
    private final SBuildServer buildServer;
    @NotNull
    private final DependencyFactory dependencyFactory;

    public ChangeListener(
            @NotNull final EventDispatcher<BuildServerListener> dispatcher,
            @NotNull SBuildServer server,
            @NotNull DependencyFactory dependencyFactory) {
        dispatcher.addListener(this);
        buildServer = server;
        this.dependencyFactory = dependencyFactory;

        Loggers.SERVER.info("Listening for changes in Ivy descriptor files");
    }

    /**
     * Called when a new modification (user commit) was detected.
     * Vcs modification can be detected during periodical process of changes collecting
     * (in this case buildTypes parameter is null)
     * or during build(s) startup phase.
     * In the latter case buildTypes parameter will contain collection
     * of build configurations for which changes collection was performed.
     */
    @Override
    public void changeAdded(@NotNull final VcsModification modification,
                            @NotNull final VcsRoot root,
                            @Nullable final Collection<SBuildType> buildTypes) {
        Loggers.SERVER.info("Change detected");

        for (VcsFileModification change : modification.getChanges()) {
            try {
                String path = change.getRelativeFileName();
                if (path.endsWith("ivy.xml")) {
                    Loggers.SERVER.info("Change detected in Ivy descriptor");
                    byte[] newContent = change.getContentAfter();
                    byte[] oldContent = change.getContentBefore();

                    if (!Arrays.equals(oldContent, newContent)) {
                        Loggers.SERVER.info("New descriptor is different");

                        IvyDescriptor oldDescriptor = new IvyDescriptor(oldContent);
                        IvyDescriptor newDescriptor = new IvyDescriptor(newContent);
                        updateSnapshotDependencies(oldDescriptor, newDescriptor);
                    }
                }
            } catch (Exception e) {
                Loggers.SERVER.error("This is rather unexpected", e);
            }
        }
    }

    private static final String OPTED_IN_KEY = "AutoSetSnapshotDependenciesFromIvy";

    private boolean optedIn(SBuildType buildType) {
        Map<String, String> parameters = buildType.getParameters();
        if (parameters.containsKey(OPTED_IN_KEY)) {
            return true;
        }

        Loggers.SERVER.info("BuildType does not have the AutoSetSnapshotDependenciesFromIvy parameter set");
        return false;
    }

    private static final String OPTED_OUT_KEY = "DisableAutoDependFromMe";

    private void updateSnapshotDependencies(final IvyDescriptor oldDescriptor, final IvyDescriptor newDescriptor) {
        Loggers.SERVER.info("Updating dependencies");
        ProjectManager pm = buildServer.getProjectManager();

        List<SBuildType> buildTypes = pm.getActiveBuildTypes();
        SBuildType myBuild = null; // find "myself"
        List<SBuildType> dependsOnBuilds = new LinkedList<SBuildType>();
        // find builds that I depend on

        for (SBuildType candidateBuild : buildTypes) {
            IvyDescriptor candidateBuildDescriptor = getIvyDescriptor(candidateBuild);
            if (candidateBuildDescriptor.equals(oldDescriptor)) {
                myBuild = candidateBuild;
                Loggers.SERVER.info("Determined that changed descriptor corresponds with build " + myBuild.getExtendedFullName());
            } else if (newDescriptor.dependsOn(candidateBuildDescriptor)) {
                if (candidateBuild.getParameters().containsKey(OPTED_OUT_KEY)) {
                    Loggers.SERVER.info("Would depend on " + candidateBuild.getExtendedFullName() + " but it has disabled auto dependencies");
                }
                else {
                    dependsOnBuilds.add(candidateBuild);
                    Loggers.SERVER.info("Detecting a dependency on build " + candidateBuild.getExtendedFullName());
                }
            }
        }

        if (myBuild == null) {
            Loggers.SERVER.info("Could not identify my build");
            return;
        }

        if (!optedIn(myBuild)) {
            Loggers.SERVER.info("Build has not opted in for automatic dependency management");
            return;
        }

        removeDependencies(myBuild);
        for (SBuildType depBt : dependsOnBuilds) {
            Loggers.SERVER.info("Detected dependency on " + depBt.getExtendedName());
            final Dependency dependency = dependencyFactory.createDependency(depBt);
            myBuild.addDependency(dependency);
        }
        myBuild.persist();
    }

    private IvyDescriptor getIvyDescriptor(BuildType buildType) {
        final SBuild build = (SBuild) buildType.getLastChangesFinished();

        final IvyDescriptor[] result = new IvyDescriptor[1];
        result[0] = null;

        if (build != null) {
            final BuildArtifacts artifacts = build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT);
            artifacts.iterateArtifacts(new BuildArtifacts.BuildArtifactsProcessor() {
                @NotNull
                @Override
                public Continuation processBuildArtifact(@NotNull BuildArtifact buildArtifact) {
                    final String relativePath = buildArtifact.getRelativePath();
                    Loggers.SERVER.info("Analyzing artifact " + relativePath);
                    if (buildArtifact.isFile() &&
                            relativePath.startsWith("ivy") &&
                            relativePath.endsWith(".xml")) {
                        try {
                            result[0] = new IvyDescriptor(buildArtifact.getInputStream());
                            return Continuation.BREAK;
                        } catch (XPathExpressionException e) {
                            Loggers.SERVER.error("XPath error processing descriptor", e);
                        } catch (IOException e) {
                            Loggers.SERVER.error("IO error processing descriptor", e);
                        }
                    }

                    return Continuation.CONTINUE;
                }
            });
        }
        return result[0];
    }

    public void removeDependencies(SBuildType buildType) {
        Loggers.SERVER.info("Removing dependencies");

        List<Dependency> dependencies = buildType.getOwnDependencies();
        for (Dependency dependency : dependencies) {
            buildType.removeDependency(dependency);
        }
    }
}
