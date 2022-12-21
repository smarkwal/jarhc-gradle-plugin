# JarHC Gradle Plugin

Gradle plugin to generate a [JarHC - JAR Health Check](https://github.com/smarkwal/jarhc) report for your project's dependencies.

![JarHC Logo](https://github.com/smarkwal/jarhc/blob/master/docs/jarhc-logo.png?raw=true)

[![License](https://img.shields.io/github/license/smarkwal/jarhc-gradle-plugin?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Release](https://img.shields.io/github/v/release/smarkwal/jarhc-gradle-plugin?label=Latest)](https://github.com/smarkwal/jarhc-gradle-plugin/releases/latest)
[![Build](https://github.com/smarkwal/jarhc-gradle-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/smarkwal/jarhc-gradle-plugin/actions/workflows/build.yml)
[![Tests](https://img.shields.io/sonar/tests/smarkwal_jarhc-gradle-plugin/main?label=Tests&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/component_measures?metric=test_success_density&view=list&id=smarkwal_jarhc-gradle-plugin)
[![Coverage](https://img.shields.io/sonar/coverage/smarkwal_jarhc-gradle-plugin/main?label=Coverage&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/component_measures?id=smarkwal_jarhc-gradle-plugin&metric=coverage&view=list)
[![Quality](https://img.shields.io/sonar/quality_gate/smarkwal_jarhc-gradle-plugin/main?label=Quality&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=smarkwal_jarhc-gradle-plugin)
[![Issues](https://img.shields.io/github/issues/smarkwal/jarhc-gradle-plugin?label=Issues)](https://github.com/smarkwal/jarhc-gradle-plugin/issues)

---

## Installation

Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block) for Kotlin:

```kotlin
plugins {
    id("org.jarhc") version "1.0.0"
}
```

## Configuration

The plugin adds a `jarhcReport` task to your project:

```shell
./gradlew jarhcReport
```

Without a specific configuration, the task will generate an HTML and a text report for all runtime dependencies of your project. 
Both reports can be found in the `build` directory:

```
build/
└── reports/
    └── jarhc/
        ├── jarhc-report.html
        └── jarhc-report.txt
```

Example reports can be found here: [JarHC example reports](https://github.com/smarkwal/jarhc/wiki/Reports)

You can control many aspects of the JarHC task and take influence on the analysis and the report(s):

```kotlin
tasks {

    jarhcReport {

        // Classpath with JAR files (dependencies) to be analyzed by JarHC.
        // Default: configurations.runtimeClasspath
        classpath.setFrom(configurations.runtimeClasspath)

        // There are also properties "provided" and "runtime" to set the classpath
        // for JAR files used as provided libraries and Java runtime libraries.
        // provided.setFrom(...)
        // runtime.setFrom(...)

        // Title of the JarHC report.
        // Default: "JarHC Report for ${project.name} ${project.version}"
        reportTitle.set("ASM 7.0")

        // Path to generated report files (*.html or *.txt).
        // Default: "${buildDir}/reports/jarhc/jarhc-report.html"
        //      and "${buildDir}/reports/jarhc/jarhc-report.txt"
        reportFiles.setFrom(
            file("${projectDir}/jarhc-report-asm-7.0.html"),
            file("${projectDir}/jarhc-report-asm-7.0.txt")
        )

        // Sections to include in the report.
        // See https://github.com/smarkwal/jarhc/wiki/Usage for more details.
        // Default: empty list = include all sections
        sections.addAll("jf", "m", "cv", "jd", "d", "p", "dc", "bc", "bl")

        // Exclude sections without any issues from the report.
        // Default: false
        skipEmpty.set(true)

        // Java release (8, 9, 10, ...) to use for analysis of multi-release JAR files.
        // Default: version of Java used to run Gradle build
        release.set(11)

        // Classloader strategy. Used only if provided and/or runtime classpath is set.
        // Default: "ParentLast"
        strategy.set("ParentFirst")

        // Remove version numbers from JAR file names in the report.
        // Default: false
        removeVersion.set(true)

        // Recreate JAR file names based on Maven coordinates if possible.
        // Default: false
        useArtifactName.set(true)

        // Ignore issues related to missing Java annotations.
        // Default: false
        ignoreMissingAnnotations.set(true)

        // Ignore duplicate classes or resources if they are identical copies.
        // Default: false
        ignoreExactCopy.set(true)

        // Path to the data directory used by JarHC to cache downloaded JAR and POM files.
        // Default: "${rootDir}/.jarhc"
        dataDir.set(file("${buildDir}/jarhc-data"))

    }

}
```

Most configuration properties are 1-to-1 equivalents of the command line options explained in the [JarHC documentation](https://github.com/smarkwal/jarhc/wiki/Usage).

## Advanced examples

Add project artifact to classpath:

```kotlin
tasks {
    jarhcReport {
        classpath.setFrom(
            jar.get().archiveFile,
            configurations.runtimeClasspath
        )
    }    
}
```

Add libraries to the "provided" classpath:

```kotlin
// create a custom configuration
val providedDependencies by configurations.creating

// add dependencies to the custom configuration
dependencies {
    providedDependencies("javax.servlet:javax.servlet-api:3.1.0")
}

// use the custom configuration in the task
tasks {
    jarhcReport {
        provided.setFrom(providedDependencies)
    }
}
```
