package com.codelab.gradle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodelabGradlePluginTest {

  @TempDir Path testProjectDir;

  @Test
  void acceptsRequiredGradleVersion() throws Exception {
    writeBuildFiles();
    copyGradleWrapper();

    GradleRunner.create()
        .withProjectDir(testProjectDir.toFile())
        .withPluginClasspath()
        .withGradleVersion("9.7.1")
        .withArguments("tasks")
        .build();
  }

  @Test
  void rejectsUnsupportedGradleVersion() throws Exception {
    writeBuildFiles();
    copyGradleWrapper();

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withPluginClasspath()
            .withGradleVersion("9.7.0")
            .withArguments("tasks")
            .buildAndFail();

    assertTrue(
        result
            .getOutput()
            .contains("Codelab build requires Gradle 9.7.1, but Gradle 9.7.0 is running."));
  }

  @Test
  void rejectsMissingGradleWrapper() throws Exception {
    writeBuildFiles();

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withPluginClasspath()
            .withGradleVersion("9.7.1")
            .withArguments("tasks")
            .buildAndFail();

    assertTrue(result.getOutput().contains("Required Gradle wrapper file is missing"));
  }

  private void writeBuildFiles() throws Exception {
    Files.writeString(
        testProjectDir.resolve("settings.gradle"), "rootProject.name = 'test-project'\n");

    Files.writeString(
        testProjectDir.resolve("build.gradle"),
        """
                plugins {
                    id 'com.codelab.gradle'
                }
                """);
  }

  private void copyGradleWrapper() throws Exception {
    Path sourceProjectDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

    copy(sourceProjectDir.resolve("gradlew"), testProjectDir.resolve("gradlew"));

    copy(sourceProjectDir.resolve("gradlew.bat"), testProjectDir.resolve("gradlew.bat"));

    Path sourceWrapperDir = sourceProjectDir.resolve("gradle/wrapper");

    Path targetWrapperDir = testProjectDir.resolve("gradle/wrapper");

    Files.createDirectories(targetWrapperDir);

    copy(
        sourceWrapperDir.resolve("gradle-wrapper.jar"),
        targetWrapperDir.resolve("gradle-wrapper.jar"));

    copy(
        sourceWrapperDir.resolve("gradle-wrapper.properties"),
        targetWrapperDir.resolve("gradle-wrapper.properties"));
  }

  private void copy(Path source, Path target) throws Exception {
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
  }
}
