plugins {
    java
    id("org.jarhc") version "1.0.0-SNAPSHOT"
}

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
        dataDir.set(file("${projectDir}/jarhc-data"))
        reportTitle.set("ASM 9.4")
        reportFiles.setFrom(
            file("${projectDir}/jarhc-report-asm-9.4.html"),
            file("${projectDir}/jarhc-report-asm-9.4.txt")
        )
    }
}
