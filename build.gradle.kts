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
    id("com.github.ben-manes.versions") version "0.45.0"

}

tasks {

    // add a custom "clean" task to root project
    register("clean") {
        group = "build"
        doLast {
            delete("${rootDir}/build")
        }
    }
}

allprojects {

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
    apply(plugin = "idea")
}

// special settings for IntelliJ IDEA
idea {
    project {
        jdkName = "11"
        languageLevel = org.gradle.plugins.ide.idea.model.IdeaLanguageLevel(JavaVersion.VERSION_11)
        vcs = "Git"
    }
}
