/*
 * Copyright 2022 Stephan Markwalder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.*

plugins {
    idea

    // Gradle Versions Plugin
    // https://github.com/ben-manes/gradle-versions-plugin
    alias(libs.plugins.versions)

}

// lock the buildscript classpath (plugins) of root project
buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

tasks {

    // add a custom "clean" task to root project
    register("clean") {
        group = "build"
        doLast {
            delete("${rootDir}/build")
        }
    }

    // Refresh all Gradle dependency lockfiles in one invocation: root project,
    // every subproject, and their buildscript classpaths. New subprojects are
    // picked up automatically, so there is no list to keep in sync.
    // Usage: ./gradlew updateGradleLockfiles --write-locks
    register("updateGradleLockfiles") {
        group = "help"
        description = "Resolves all dependencies of all projects to refresh the lockfiles (run with --write-locks)."
        dependsOn(allprojects.flatMap { p ->
            listOf(p.tasks.named("dependencies"), p.tasks.named("buildEnvironment"))
        })
    }

    dependencyUpdates {
        gradleReleaseChannel = "current"
        rejectVersionIf {
            isUnstableVersion(candidate)
        }
    }

}

allprojects {

    // lock all dependency configurations in every project ---------------------
    dependencyLocking {
        lockAllConfigurations()
    }

    // load user-specific properties -------------------------------------------
    val userPropertiesFile = file("${rootDir}/gradle.user.properties")
    if (userPropertiesFile.exists()) {
        val userProperties = Properties()
        userProperties.load(userPropertiesFile.inputStream())
        userProperties.forEach {
            project.ext.set(it.key.toString(), it.value)
        }
    }

    repositories {
        mavenCentral()
    }

}

subprojects {

    // lock the buildscript classpath (plugins) of every subproject ------------
    buildscript {
        configurations.classpath {
            resolutionStrategy.activateDependencyLocking()
        }
    }

}

// special settings for IntelliJ IDEA
idea {
    project {
        jdkName = "17"
        languageLevel = org.gradle.plugins.ide.idea.model.IdeaLanguageLevel(JavaVersion.VERSION_11)
        vcs = "Git"
    }
}

// Fail fast if updateGradleLockfiles is run without --write-locks, instead of
// silently resolving dependencies without rewriting the lockfiles.
gradle.taskGraph.whenReady {
    if (hasTask(":updateGradleLockfiles") && !gradle.startParameter.isWriteDependencyLocks) {
        throw GradleException("Run with --write-locks: ./gradlew updateGradleLockfiles --write-locks")
    }
}

fun isUnstableVersion(candidate: ModuleComponentIdentifier): Boolean {
    return candidate.version.contains("-M") // ignore milestone version
            || candidate.version.contains("-rc") // ignore release candidate versions
            || candidate.version.contains("-alpha") // ignore alpha versions
}
