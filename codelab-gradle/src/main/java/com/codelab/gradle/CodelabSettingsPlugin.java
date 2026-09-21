package com.codelab.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

public class CodelabSettingsPlugin implements Plugin<Settings> {

  @Override
  public void apply(Settings settings) {
    settings
        .getPluginManagement()
        .plugins(
            plugins -> {
              plugins.id("org.springframework.boot").version("4.1.1");
              plugins.id("io.spring.dependency-management").version("1.1.7");
              // all other centrally managed plugins
            });
    settings
        .getDependencyResolutionManagement()
        .repositories(
            repositories -> {
              repositories.mavenLocal();
              repositories.mavenCentral();
            });
  }
}
