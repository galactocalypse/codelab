# Codelab Gradle Plugin

`com.codelab.gradle` provides shared build conventions for Codelab projects. It centralizes Java, Gradle, dependency, and Maven publishing configuration so individual projects only need to declare their project-specific settings and dependencies.

## What the plugin does

### Gradle version enforcement

The plugin requires all builds to use the configured Gradle version.

By default:

```text
Gradle: 9.7.1
```

A build fails when a different Gradle version is used.

The plugin also validates that the project contains the required Gradle Wrapper files.

### Java version enforcement

The plugin requires the configured Java version to be used to run the build.

By default:

```text
Java: 25
```

It also configures the Java toolchain for projects that apply:

* `java`
* `java-library`

### Project conventions

The plugin exposes a `codelab` extension for configuring shared build conventions.

Default values include:

```text
Java version:           25
Gradle version:         9.7.1
Required group prefix:  com.codelab
License:                Apache License 2.0
SCM URL:                https://github.com/codelab
```

### Publishing conventions

The plugin applies Gradle's `maven-publish` plugin automatically.

For projects that also apply `java-platform`, it automatically:

* configures Maven publishing;
* publishes the `javaPlatform` component;
* creates the `maven` publication;
* configures `mavenLocal()` as the publishing repository;
* configures common Maven POM metadata;
* sets the POM name from the project name;
* sets the POM description from the project description;
* sets the POM URL and SCM URL;
* adds the shared license information.

Projects do not need their own `publishing { ... }` block.

For example, a BOM only needs:

```groovy
plugins {
    id 'com.codelab.gradle'
    id 'java-platform'
}

group = 'com.codelab'
version = '1.0.0'
description = 'Codelab BOM'
```

### Publishing validation

Projects configured for publishing must satisfy the following requirements:

* `group` must start with `com.codelab`;
* `version` must be explicitly defined;
* `description` must be explicitly defined.

The build fails when any of these requirements are not satisfied.

## Settings-level conventions

Build-wide dependency resolution is configured through the Codelab settings plugin, `com.codelab.settings`.

The settings plugin centralizes:

* dependency repositories;
* repository resolution rules;
* other settings-level dependency-resolution conventions.

For example:

```groovy
pluginManagement {
    includeBuild('../codelab-gradle')
}

plugins {
    id 'com.codelab.settings'
}

rootProject.name = 'codelab-bom'
```

The settings plugin configures the dependency repositories for the entire build, so individual projects do not need to declare:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}
```

This keeps `settings.gradle` limited to build identity and loading the shared Codelab settings conventions.

## Usage

A typical project has a settings file:

```groovy
pluginManagement {
    includeBuild('../codelab-gradle')
}

plugins {
    id 'com.codelab.settings'
}

rootProject.name = 'codelab-project'
```

and a project build file:

```groovy
plugins {
    id 'com.codelab.gradle'
}
```

### Java project

```groovy
plugins {
    id 'com.codelab.gradle'
    id 'java-library'
}

group = 'com.codelab.example'
version = '1.0.0'
description = 'Example library'
```

### BOM project

```groovy
plugins {
    id 'com.codelab.gradle'
    id 'java-platform'
}

group = 'com.codelab'
version = '1.0.0'
description = 'Codelab BOM'

dependencies {
    constraints {
        api 'com.example:example-library:1.0.0'
    }
}
```

The goal is to keep individual project build files focused on **project identity and dependencies**, while `com.codelab.gradle` and `com.codelab.settings` own the shared build, dependency-resolution, and publishing policies.
