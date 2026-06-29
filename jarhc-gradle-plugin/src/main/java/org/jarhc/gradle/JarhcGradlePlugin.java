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
package org.jarhc.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.util.GradleVersion;

public class JarhcGradlePlugin implements Plugin<Project> {

	// minimum supported Gradle version
	static final String MINIMUM_GRADLE_VERSION = "8.8";

	@Override
	public void apply(Project project) {

		// fail fast with a clear message on unsupported Gradle versions
		if (GradleVersion.current().compareTo(GradleVersion.version(MINIMUM_GRADLE_VERSION)) < 0) {
			throw new GradleException("The JarHC Gradle plugin requires Gradle " + MINIMUM_GRADLE_VERSION + " or later.");
		}

		// register jarhcReport task
		project.getTasks().register("jarhcReport", JarhcReportTask.class, task -> setDefaultConfiguration(task, project));
	}

	private static void setDefaultConfiguration(JarhcReportTask task, Project project) {

		// get project information
		String name = project.getName();
		String version = project.getVersion().toString();

		Directory rootDir = project.getRootProject().getLayout().getProjectDirectory();
		DirectoryProperty buildDir = project.getLayout().getBuildDirectory();

		// prepare default values
		Directory dataDir = rootDir.dir(".jarhc");
		String reportTitle = "JarHC Report for " + name + " " + version;
		Provider<RegularFile> htmlReportFile = buildDir.file("reports/jarhc/jarhc-report.html");
		Provider<RegularFile> textReportFile = buildDir.file("reports/jarhc/jarhc-report.txt");

		// apply default configuration
		// default the classpath to the project's runtime classpath, wired at
		// configuration time so the task never accesses Task.project at execution
		// time (which is deprecated and removed for the configuration cache).
		// convention() is used instead of setFrom() so an explicit classpath
		// configured on the task always wins, regardless of plugin apply order.
		project.getPluginManager().withPlugin("java", plugin ->
				task.getClasspath().convention(project.getConfigurations().named("runtimeClasspath")));
		// provided and runtime are left at their default (empty) instead of being
		// set explicitly, so configuration on the task is never overwritten
		task.getSections().empty();
		task.getSkipEmpty().set(false);
		task.getSortRows().set(false);
		task.getRelease().set(-1);
		task.getStrategy().set("ParentLast");
		task.getRemoveVersion().set(false);
		task.getUseArtifactName().set(false);
		task.getIgnoreMissingAnnotations().set(false);
		task.getIgnoreExactCopy().set(false);
		task.getDataDir().set(dataDir);
		task.getReportTitle().set(reportTitle);
		task.getReportFiles().from(htmlReportFile, textReportFile);
	}

}
