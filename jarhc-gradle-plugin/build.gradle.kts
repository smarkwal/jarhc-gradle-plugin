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
    idea
    jacoco
    signing
    `maven-publish`

    // Gradle Publish plugin
    // https://plugins.gradle.org/docs/publish-plugin
    alias(libs.plugins.plugin.publish)

    // run Sonar analysis
    alias(libs.plugins.sonarqube)
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

    // JarHC is compiled against but NOT bundled into the plugin jar. At the
    // consumer's build time it is resolved by the plugin's 'jarhc' configuration
    // and executed in an isolated worker classloader (see JarhcGradlePlugin).
    compileOnly(libs.jarhc)
    testImplementation(libs.jarhc)

    // Gradle API on the test runtime classpath (e.g. ProjectBuilder)
    testImplementation(gradleApi())

    // JUnit and Mockito
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.junit.jupiter)
}

gradlePlugin {
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
val skipTests = providers.gradleProperty("skip.tests").isPresent

// Java ------------------------------------------------------------------------

java {

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    // automatically package source code as artifact -sources.jar
    withSourcesJar()

    // automatically package Javadoc as artifact -javadoc.jar
    withJavadocJar()
}

// The plugin bytecode must remain Java 11 compatible, but it is compiled with the
// Java 17 toolchain above (targeting Java 11 via --release below). The Java 17
// toolchain is required because parts of the Gradle 9 API the plugin references
// (e.g. the worker API) ship as Java 17 bytecode, which a Java 11 compiler cannot
// read; --release 11 still emits Java 11 bytecode. The unit and functional tests
// additionally use Gradle test fixtures (e.g. ProjectBuilder) that require Java 17,
// so the test sources target Java 17.
tasks.named<JavaCompile>("compileJava") {
    options.release.set(11)
}

tasks.withType<JavaCompile>().matching { it.name.endsWith("TestJava") }.configureEach {
    options.release.set(17)
}

// special settings for IntelliJ IDEA
idea {
    module {
        testSources.from("src/functionalTest/java")
        testResources.from("src/functionalTest/resources")
    }
}

// tests -----------------------------------------------------------------------

// source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest")

// compile and run the functional tests against the plugin's main classes so they
// can reference constants such as JarhcGradlePlugin.MINIMUM_GRADLE_VERSION
functionalTestSourceSet.compileClasspath += sourceSets["main"].output
functionalTestSourceSet.runtimeClasspath += sourceSets["main"].output

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// task to run the functional tests
val functionalTest = tasks.register<Test>("functionalTest") {
    group = "verification"
    description = "Runs the functional tests."
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    shouldRunAfter("test")
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

// 'jarhc.*' Gradle properties to pass as system properties to the JUnit JVM
val jarhcProperties = providers.gradlePropertiesPrefixedBy("jarhc.")

tasks.withType<Test>().configureEach {

    // run tests on Java 17 (Gradle 9 test fixtures require Java 17)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })

    // skip tests if property "skip.tests" is set
    if (skipTests) enabled = false

    // use JUnit
    useJUnitPlatform()

    // pass all 'jarhc.*' Gradle properties as system properties to JUnit JVM
    jarhcProperties.get().forEach { (key, value) ->
        systemProperty(key, value)
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
        val sonarBranchName = getGitBranchName()
        property("sonar.branch.name", sonarBranchName)

        // target Git branch for merge
        if (sonarBranchName != "main") {
            // https://docs.sonarsource.com/sonarqube-cloud/enriching/branch-analysis-setup/
            property("sonar.branch.target", "main")
            // https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/analysis-parameters/
            property("sonar.newCode.referenceBranch", "main")
        }

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
    var gitDir = rootDir.resolve(".git")
    // In worktrees and submodules, .git is a file: "gitdir: <path>"
    if (gitDir.isFile) {
        val gitdir = gitDir.readText(Charsets.UTF_8).trim().removePrefix("gitdir: ")
        gitDir = rootDir.resolve(gitdir)
    }
    val head = gitDir.resolve("HEAD")
    if (head.isFile) {
        val content = head.readText(Charsets.UTF_8).trim()
        return content.removePrefix("ref: refs/heads/")
    }
    throw GradleException("Git branch name not found.")
}
