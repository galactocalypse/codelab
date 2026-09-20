package com.codelab.gradle;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.plugins.quality.CheckstyleExtension;

public class CodelabJavaQualityPlugin implements Plugin<Project> {

  private static final String CHECKSTYLE_VERSION = "14.1.0";
  private static final String CHECKSTYLE_CONFIG = "google_checks.xml";

  @Override
  public void apply(Project project) {
    project.getPluginManager().withPlugin("java", ignored -> configureJavaQuality(project));
  }

  private void configureJavaQuality(Project project) {
    project.getPluginManager().apply("checkstyle");
    project.getPluginManager().apply("com.diffplug.spotless");

    configureCheckstyle(project);
    configureSpotless(project);
    configureQualityChecks(project);
  }

  private void configureCheckstyle(Project project) {
    CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);

    checkstyle.setToolVersion(CHECKSTYLE_VERSION);

    checkstyle.setConfig(
        project
            .getResources()
            .getText()
            .fromArchiveEntry(
                resolveCheckstyleJar(project), CHECKSTYLE_CONFIG, StandardCharsets.UTF_8.name()));
  }

  private File resolveCheckstyleJar(Project project) {
    Configuration configuration = project.getConfigurations().getByName("checkstyle");

    ArtifactCollection artifacts =
        configuration
            .getIncoming()
            .artifactView(
                view ->
                    view.componentFilter(
                        componentIdentifier ->
                            componentIdentifier instanceof ModuleComponentIdentifier module
                                && module.getGroup().equals("com.puppycrawl.tools")
                                && module.getModule().equals("checkstyle")))
            .getArtifacts();

    Set<ResolvedArtifactResult> resolvedArtifacts = artifacts.getArtifacts();

    if (resolvedArtifacts.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly one Checkstyle artifact, but found " + resolvedArtifacts.size());
    }

    return resolvedArtifacts.iterator().next().getFile();
  }

  private void configureSpotless(Project project) {
    // Configure Spotless here.
  }

  private void configureQualityChecks(Project project) {
    project.getTasks().named("check").configure(check -> check.dependsOn("spotlessCheck"));
  }
}
