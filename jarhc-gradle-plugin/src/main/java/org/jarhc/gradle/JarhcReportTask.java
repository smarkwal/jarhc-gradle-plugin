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

import static org.jarhc.artifacts.MavenRepository.MAVEN_CENTRAL_URL;

import java.io.File;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.jarhc.app.Application;
import org.jarhc.app.Options;
import org.jarhc.artifacts.ArtifactFinder;
import org.jarhc.artifacts.MavenArtifactFinder;
import org.jarhc.artifacts.MavenRepository;
import org.jarhc.artifacts.Repository;
import org.slf4j.Logger;

public abstract class JarhcReportTask extends DefaultTask {

	@InputFiles
	public abstract ConfigurableFileCollection getClasspath();

	@InputFiles
	public abstract ConfigurableFileCollection getProvided();

	@Internal
	public abstract DirectoryProperty getDataDir();

	@Input
	public abstract Property<String> getReportTitle();

	@OutputFiles
	public abstract ConfigurableFileCollection getReportFiles();

	@TaskAction
	void run() {
		Project project = getProject();
		Logger logger = project.getLogger();
		logger.info("JarHC Report Task");

		Options options = new Options();

		FileCollection classpath = getClasspath();
		if (classpath == null || classpath.isEmpty()) {
			ConfigurationContainer configurations = project.getConfigurations();
			classpath = configurations.getByName("runtimeClasspath");
		}
		logger.info("Classpath:");
		for (File file : classpath) {
			options.addClasspathJarPath(file.getAbsolutePath());
			logger.info("- {}", logger.isDebugEnabled() ? file.getAbsolutePath() : file.getName());
		}

		FileCollection provided = getProvided();
		if (provided != null && !provided.isEmpty()) {
			logger.info("Provided:");
			for (File file : provided) {
				options.addProvidedJarPath(file.getAbsolutePath());
				logger.info("- {}", logger.isDebugEnabled() ? file.getAbsolutePath() : file.getName());
			}
		}

		if (getDataDir().isPresent()) {
			String path = getDataDir().get().getAsFile().getAbsolutePath();
			logger.info("Data path: " + path);
			options.setDataPath(path);
		} else {
			throw new GradleException("No data path specified.");
		}

		String reportTitle = getReportTitle().get();
		logger.info("Report title: " + reportTitle);
		options.setReportTitle(reportTitle);

		ConfigurableFileCollection reportFiles = getReportFiles();
		if (reportFiles != null && !reportFiles.isEmpty()) {
			for (File reportFile : reportFiles) {
				String path = reportFile.getAbsolutePath();
				logger.info("Report file: " + path);
				options.addReportFile(path);
			}
		}

		// TODO: support more JarHC options

		int exitCode = runJarHC(options, logger);
		logger.info("JarHC exit code: " + exitCode);

		if (exitCode != 0) {
			throw new GradleException("JarHC failed with exit code " + exitCode);
		}

	}

	private static int runJarHC(Options options, Logger logger) {

		String dataPath = options.getDataPath();
		if (dataPath == null) throw new GradleException("Data path is not set");

		File directory = new File(dataPath);
		if (!directory.isDirectory()) {
			boolean created = directory.mkdirs();
			if (!created) {
				throw new GradleException("Failed to create directory: " + directory.getAbsolutePath());
			}
		}

		File cacheDir = new File(dataPath, "checksums");
		ArtifactFinder artifactFinder = new MavenArtifactFinder(cacheDir, logger);

		int javaVersion = options.getRelease();
		Repository repository = new MavenRepository(javaVersion, MAVEN_CENTRAL_URL, dataPath, artifactFinder, logger);

		Application application = new Application(logger);
		application.setRepository(repository);

		return application.run(options);
	}

}
