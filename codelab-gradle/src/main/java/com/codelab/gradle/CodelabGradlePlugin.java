package com.codelab.gradle;

import com.codelab.gradle.CodelabBuildExtension;
import com.codelab.gradle.WrapperValidator;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.util.GradleVersion;

public class CodelabGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        CodelabBuildExtension extension =
                project.getExtensions().create(
                        "codelab",
                        CodelabBuildExtension.class
                );

        extension.getJavaVersion().convention(25);
        extension.getGradleVersion().convention("9.7.1");
        extension.getRequiredGroupPrefix().convention("com.codelab");

        extension.getLicenseName().convention(
                "The Apache Software License, Version 2.0"
        );

        extension.getLicenseUrl().convention(
                "https://www.apache.org/licenses/LICENSE-2.0.txt"
        );

        extension.getScmUrl().convention(
                "https://github.com/codelab"
        );

        checkGradleVersion(extension);
        checkJavaVersion(extension);

        project.getPluginManager().withPlugin("java", ignored -> {
            configureJavaToolchain(project, extension);
        });

        project.getPluginManager().withPlugin("java-library", ignored -> {
            configureJavaToolchain(project, extension);
        });

        WrapperValidator.validate(
                project,
                extension.getGradleVersion().get()
        );

        // Publishing is owned entirely by this plugin.
        project.getPluginManager().apply("maven-publish");

        // Configure publishing once both Maven Publish and Java Platform
        // are available, regardless of plugin declaration order.
        project.getPluginManager().withPlugin(
                "maven-publish",
                ignored -> project.getPluginManager().withPlugin(
                        "java-platform",
                        javaPlatformPlugin ->
                                configurePublishing(project, extension)
                )
        );
        project.getTasks()
                .withType(JavaCompile.class)
                .configureEach(task ->
                        task.getOptions().getCompilerArgs().add("-parameters")
                );
    }

    private void configurePublishing(
            Project project,
            CodelabBuildExtension extension
    ) {
        PublishingExtension publishing =
                project.getExtensions().getByType(
                        PublishingExtension.class
                );

        // Publishing destination is part of the convention.
        publishing.getRepositories().mavenLocal();

        // Create the BOM publication.
        publishing.getPublications().create(
                "maven",
                MavenPublication.class,
                publication -> {
                    publication.from(
                            project.getComponents()
                                    .getByName("javaPlatform")
                    );

                    configurePom(
                            project,
                            publication,
                            extension
                    );
                }
        );

        /*
         * The consumer build script is evaluated after the plugins block.
         * Therefore group/version/description are not necessarily available
         * when this plugin's apply() method runs.
         *
         * Validate them after the consumer project has been evaluated.
         */
        project.afterEvaluate(ignored ->
                validatePublishingInformation(
                        project,
                        extension
                )
        );
    }

    private void configurePom(
            Project project,
            MavenPublication publication,
            CodelabBuildExtension extension
    ) {
        publication.getPom().getName().set(project.getName());

        publication.getPom()
                .getDescription()
                .set(
                        project.getProviders()
                                .provider(project::getDescription)
                );

        publication.getPom()
                .getUrl()
                .set(extension.getScmUrl());

        publication.getPom().licenses(licenses ->
                licenses.license(license -> {
                    license.getName().set(
                            extension.getLicenseName()
                    );

                    license.getUrl().set(
                            extension.getLicenseUrl()
                    );
                })
        );

        publication.getPom().scm(scm ->
                scm.getUrl().set(
                        extension.getScmUrl()
                )
        );
    }

    private void validatePublishingInformation(
            Project project,
            CodelabBuildExtension extension
    ) {
        String group = project.getGroup().toString();
        String requiredPrefix =
                extension.getRequiredGroupPrefix().get();

        if ("undefined".equals(group)
                || !group.startsWith(requiredPrefix)) {
            throw new IllegalStateException(
                    "Published projects must have a group starting with "
                            + requiredPrefix
                            + ", but found: "
                            + group
            );
        }

        String version = project.getVersion().toString();

        if ("unspecified".equals(version)) {
            throw new IllegalStateException(
                    "Published projects must define an explicit version."
            );
        }

        String description = project.getDescription();

        if (description == null || description.isBlank()) {
            throw new IllegalStateException(
                    "Published projects must define a description."
            );
        }
    }

    private void configureJavaToolchain(
            Project project,
            CodelabBuildExtension extension
    ) {
        project.getExtensions()
                .configure(
                        org.gradle.api.plugins.JavaPluginExtension.class,
                        java -> java.getToolchain()
                                .getLanguageVersion()
                                .set(
                                        org.gradle.jvm.toolchain.JavaLanguageVersion.of(
                                                extension.getJavaVersion().get()
                                        )
                                )
                );
    }

    private void checkGradleVersion(
            CodelabBuildExtension extension
    ) {
        String actualVersion =
                GradleVersion.current().getVersion();

        String requiredVersion =
                extension.getGradleVersion().get();

        if (!requiredVersion.equals(actualVersion)) {
            throw new IllegalStateException(
                    "Codelab build requires Gradle "
                            + requiredVersion
                            + ", but Gradle "
                            + actualVersion
                            + " is running."
            );
        }
    }

    private void checkJavaVersion(
            CodelabBuildExtension extension
    ) {
        int actualVersion =
                Runtime.version().feature();

        int requiredVersion =
                extension.getJavaVersion().get();

        if (requiredVersion != actualVersion) {
            throw new IllegalStateException(
                    "Codelab build requires Java "
                            + requiredVersion
                            + ", but Java "
                            + actualVersion
                            + " is running Gradle."
            );
        }
    }
}

