package com.codelab.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.gradle.api.Project;

public final class WrapperValidator {

  private WrapperValidator() {}

  public static void validate(Project project, String requiredGradleVersion) {
    Path root = project.getRootDir().toPath();

    requireFile(root.resolve("gradlew"));
    requireFile(root.resolve("gradlew.bat"));
    requireFile(root.resolve("gradle/wrapper/gradle-wrapper.jar"));

    Path propertiesFile = root.resolve("gradle/wrapper/gradle-wrapper.properties");

    requireFile(propertiesFile);

    Properties properties = new Properties();

    try (InputStream input = Files.newInputStream(propertiesFile)) {
      properties.load(input);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read " + propertiesFile, e);
    }

    String distributionUrl = properties.getProperty("distributionUrl");

    if (distributionUrl == null) {
      throw new IllegalStateException("gradle-wrapper.properties is missing distributionUrl.");
    }

    String expected =
        "https://services.gradle.org/distributions/gradle-" + requiredGradleVersion + "-bin.zip";

    if (!expected.equals(distributionUrl)) {
      throw new IllegalStateException(
          "Codelab build requires the Gradle wrapper to use "
              + requiredGradleVersion
              + ", but found: "
              + distributionUrl);
    }
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("Required Gradle wrapper file is missing: " + path);
    }
  }
}
