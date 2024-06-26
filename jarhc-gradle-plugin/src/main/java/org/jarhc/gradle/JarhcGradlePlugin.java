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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public class JarhcGradlePlugin implements Plugin<Project> {

	public void apply(Project project) {

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
		task.getClasspath().setFrom();
		task.getProvided().setFrom();
		task.getRuntime().setFrom();
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
