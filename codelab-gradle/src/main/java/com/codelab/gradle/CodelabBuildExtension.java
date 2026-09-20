package com.codelab.gradle;

import org.gradle.api.provider.Property;

public abstract class CodelabBuildExtension {

  public abstract Property<Integer> getJavaVersion();

  public abstract Property<String> getGradleVersion();

  public abstract Property<String> getRequiredGroupPrefix();

  public abstract Property<String> getLicenseName();

  public abstract Property<String> getLicenseUrl();

  public abstract Property<String> getScmUrl();
}
