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

plugins {
    `java-gradle-plugin`
    jacoco
    signing
    `maven-publish`

    // Gradle Publish plugin
    // https://plugins.gradle.org/docs/publish-plugin
    id("com.gradle.plugin-publish") version "1.2.1"

    // Gradle Shadow plugin
    id("com.github.johnrengelman.shadow") version "8.1.1"

    // run Sonar analysis
    id("org.sonarqube") version "5.1.0.4882"

    // get current Git branch name
    id("org.ajoberstar.grgit") version "5.2.2"
}

// Preconditions based on which tasks should be executed -----------------------

gradle.taskGraph.whenReady {

    // if sonar task should be executed ...
    if (gradle.taskGraph.hasTask(":sonar")) {
        // environment variable SONAR_TOKEN or system property "sonar.token" must be set
        val tokenFound = System.getProperties().containsKey("sonar.token") || System.getenv("SONAR_TOKEN") != null
        if (!tokenFound) {
            val error = "Sonar: Token not found.\nPlease set system property 'sonar.token' or environment variable 'SONAR_TOKEN'."
            throw GradleException(error)
        }
    }

}

dependencies {

    // JarHC 2.2.0
    implementation("org.jarhc:jarhc:2.2.0")

    // Gradle API
    // (this is required because the Gradle API dependency gets removed by the Shadow plugin)
    api(gradleApi())
    testImplementation(gradleApi())

    // JUnit 5 and Mockito
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-junit-jupiter:5.13.0")
}

gradlePlugin {

    @Suppress("UnstableApiUsage")
    plugins {
        website.set("https://github.com/smarkwal/jarhc-gradle-plugin")
        vcsUrl.set("https://github.com/smarkwal/jarhc-gradle-plugin.git")
        create("jarhcPlugin") {
            id = "org.jarhc"
            displayName = "JarHC Gradle Plugin"
            description = "Gradle plugin to generate JarHC reports."
            tags.set(listOf("jarhc", "java", "classpath", "dependencies"))
            implementationClass = "org.jarhc.gradle.JarhcGradlePlugin"
        }
    }
}

// configuration properties ----------------------------------------------------

// flag to skip unit and integration tests
// command line option: -Pskip.tests
val skipTests: Boolean = project.hasProperty("skip.tests")

// Java ------------------------------------------------------------------------

java {

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    // automatically package source code as artifact -sources.jar
    withSourcesJar()

    // automatically package Javadoc as artifact -javadoc.jar
    withJavadocJar()
}

// special settings for IntelliJ IDEA
idea {
    module {
        testSources.from("src/functionalTest/java")
        testResources.from("src/functionalTest/resources")
    }
}

// build -----------------------------------------------------------------------

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// tests -----------------------------------------------------------------------

// source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    shouldRunAfter("test")
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.withType(Test::class) {

    // skip tests if property "skip.tests" is set
    onlyIf { !skipTests }

    // use JUnit 5
    useJUnitPlatform()

    // pass all 'jarhc.*' Gradle properties as system properties to JUnit JVM
    properties.forEach {
        if (it.key.startsWith("jarhc.")) {
            systemProperty(it.key, it.value.toString())
        }
    }

    // settings
    maxHeapSize = "1G"

    // test task output
    testLogging {
        events = mutableSetOf(
            // TestLogEvent.STARTED,
            // TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
        )
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

}

tasks.named<Task>("check") {
    // run functional tests as part of check
    dependsOn(functionalTest)
}

// JaCoCo test coverage --------------------------------------------------------

// JaCoCo coverage report
val jacocoTestReportXml: String = "${layout.buildDirectory.get()}/reports/jacoco/test/report.xml"

jacoco {
    toolVersion = "0.8.10"
}

tasks.jacocoTestReport {

    // run all tests first
    dependsOn("test", "functionalTest")

    // get JaCoCo data from all test tasks
    executionData.from(
        "${layout.buildDirectory.get()}/jacoco/test.exec",
        "${layout.buildDirectory.get()}/jacoco/functionalTest.exec"
    )

    reports {

        // generate XML report (required for Sonar)
        xml.required.set(true)
        xml.outputLocation.set(file(jacocoTestReportXml))

        // generate HTML report
        html.required.set(true)

        // generate CSV report
        // csv.required.set(true)
    }
}

// Sonar analysis --------------------------------------------------------------

// test results
val testReportPath: String = "${layout.buildDirectory.get()}/test-results/test"
val functionalTestReportPath: String = "${layout.buildDirectory.get()}/test-results/functionalTest"

sonar {
    // documentation: https://docs.sonarqube.org/latest/analyzing-source-code/scanners/sonarscanner-for-gradle/

    properties {

        // connection to SonarCloud
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "smarkwal")
        property("sonar.projectKey", "smarkwal_jarhc-gradle-plugin")

        // Git branch
        property("sonar.branch.name", getGitBranchName())

        // paths to test sources and test classes
        property("sonar.tests", "${projectDir}/src/test/java,${projectDir}/src/functionalTest/java")
        property("sonar.java.test.binaries", "${layout.buildDirectory.get()}/classes/java/test,${layout.buildDirectory.get()}/classes/java/functionalTest")

        // include test results
        property("sonar.junit.reportPaths", "${testReportPath},${functionalTestReportPath}")

        // include test coverage results
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", jacocoTestReportXml)
    }
}

tasks.sonar {
    // run all tests and generate JaCoCo XML report
    dependsOn(
        tasks.test, functionalTest,
        tasks.jacocoTestReport
    )
}

// helper functions ------------------------------------------------------------

fun getGitBranchName(): String {
    return grgit.branch.current().name
}
