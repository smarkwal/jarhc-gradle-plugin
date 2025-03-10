plugins {
    java
    id("org.jarhc")
}

group = "org.jarhc.gradle.test"
version = "0.9.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-commons:9.4")
    implementation("org.ow2.asm:asm-tree:9.4")
    implementation("org.ow2.asm:asm-util:9.4")
}

tasks {
    jarhcReport {
        classpath.setFrom(configurations.runtimeClasspath)
        dataDir.set(file("${layout.buildDirectory.get()}/jarhc-data"))
        sections.addAll("jf", "d", "dc", "bc", "bl", "jm", "m", "ob")
        skipEmpty.set(true)
        sortRows.set(true)
        release.set(11)
        strategy.set("ParentFirst")
        removeVersion.set(true)
        useArtifactName.set(true)
        ignoreMissingAnnotations.set(true)
        ignoreExactCopy.set(true)
        reportTitle.set("ASM 9.4")
        reportFiles.setFrom(
            file("${projectDir}/jarhc-report-asm-9.4.html"),
            file("${projectDir}/jarhc-report-asm-9.4.txt")
        )
    }
}
