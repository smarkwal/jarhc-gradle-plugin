/*
 * Copyright 2026 Stephan Markwalder
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

import java.io.File;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.jarhc.app.Application;
import org.jarhc.app.Options;
import org.jarhc.artifacts.ArtifactFinder;
import org.jarhc.artifacts.DepsDevApiArtifactFinder;
import org.jarhc.artifacts.DepsDevApiVulnerabilityFinder;
import org.jarhc.artifacts.DiskCacheArtifactFinder;
import org.jarhc.artifacts.MavenRepository;
import org.jarhc.artifacts.MemoryCacheArtifactFinder;
import org.jarhc.artifacts.Repository;
import org.jarhc.artifacts.VulnerabilityFinder;
import org.jarhc.java.ClassLoaderStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes JarHC. This class references JarHC directly, so it must only ever be
 * loaded from the isolated worker classloader (via {@link JarhcWorkAction#execute()})
 * or from the test source set, both of which carry JarHC on their classpath. It is
 * never referenced from the task or plugin classes that Gradle loads at apply time.
 */
final class JarhcRunner {

	private JarhcRunner() {
		// utility class
	}

	static void run(JarhcWorkParameters parameters) {

		Logger logger = LoggerFactory.getLogger("org.jarhc");
		logger.info("JarHC Report Task");

		Options options = createOptions(parameters, logger);

		int exitCode = runJarHC(options, logger);
		logger.info("JarHC exit code: {}", exitCode);

		if (exitCode != 0) {
			throw new GradleException("JarHC failed with exit code " + exitCode);
		}
	}

	// visible for testing
	static Options createOptions(JarhcWorkParameters parameters, Logger logger) {

		Options options = new Options();

		FileCollection classpath = parameters.getClasspath();
		logger.info("Classpath:");
		for (File file : classpath) {
			options.addClasspathJarPath(file.getAbsolutePath());
			logger.info("- {}", logger.isDebugEnabled() ? file.getAbsolutePath() : file.getName());
		}

		FileCollection provided = parameters.getProvided();
		if (provided != null && !provided.isEmpty()) {
			logger.info("Provided:");
			for (File file : provided) {
				options.addProvidedJarPath(file.getAbsolutePath());
				logger.info("- {}", logger.isDebugEnabled() ? file.getAbsolutePath() : file.getName());
			}
		}

		FileCollection runtime = parameters.getRuntime();
		if (runtime != null && !runtime.isEmpty()) {
			logger.info("Runtime:");
			for (File file : runtime) {
				options.addRuntimeJarPath(file.getAbsolutePath());
				logger.info("- {}", logger.isDebugEnabled() ? file.getAbsolutePath() : file.getName());
			}
		}

		if (parameters.getSections().isPresent() && !parameters.getSections().get().isEmpty()) {
			List<String> sections = parameters.getSections().get();
			logger.info("Sections: {}", sections);
			options.setSections(sections);
		}

		if (parameters.getSkipEmpty().isPresent()) {
			boolean skipEmpty = parameters.getSkipEmpty().get();
			logger.info("Skip empty: {}", skipEmpty);
			options.setSkipEmpty(skipEmpty);
		}

		if (parameters.getRelease().isPresent() && parameters.getRelease().get() > 0) {
			int release = parameters.getRelease().get();
			logger.info("Release: {}", release);
			options.setRelease(release);
		}

		if (parameters.getStrategy().isPresent()) {
			String strategy = parameters.getStrategy().get();
			logger.info("Strategy: {}", strategy);
			options.setClassLoaderStrategy(ClassLoaderStrategy.valueOf(strategy));
		}

		if (parameters.getIgnoreMissingAnnotations().isPresent()) {
			boolean ignoreMissingAnnotations = parameters.getIgnoreMissingAnnotations().get();
			logger.info("Ignore missing annotations: {}", ignoreMissingAnnotations);
			options.setIgnoreMissingAnnotations(ignoreMissingAnnotations);
		}

		if (parameters.getIgnoreExactCopy().isPresent()) {
			boolean ignoreExactCopy = parameters.getIgnoreExactCopy().get();
			logger.info("Ignore exact copy: {}", ignoreExactCopy);
			options.setIgnoreExactCopy(ignoreExactCopy);
		}

		if (parameters.getDataDir().isPresent()) {
			String path = parameters.getDataDir().get().getAsFile().getAbsolutePath();
			logger.info("Data path: {}", path);
			options.setDataPath(path);
		} else {
			throw new GradleException("No data path specified.");
		}

		if (parameters.getReportTitle().isPresent()) {
			String reportTitle = parameters.getReportTitle().get();
			logger.info("Report title: {}", reportTitle);
			options.setReportTitle(reportTitle);
		}

		FileCollection reportFiles = parameters.getReportFiles();
		if (reportFiles != null && !reportFiles.isEmpty()) {
			for (File reportFile : reportFiles) {
				String path = reportFile.getAbsolutePath();
				logger.info("Report file: {}", path);
				options.addReportFile(path);
			}
		}

		return options;
	}

	// visible for testing
	static int runJarHC(Options options, Logger logger) {

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
		ArtifactFinder artifactFinder = new DepsDevApiArtifactFinder();
		artifactFinder = new DiskCacheArtifactFinder(cacheDir, artifactFinder);
		artifactFinder = new MemoryCacheArtifactFinder(artifactFinder);

		VulnerabilityFinder vulnerabilityFinder = new DepsDevApiVulnerabilityFinder();

		int javaVersion = options.getRelease();
		Repository repository = new MavenRepository(javaVersion, options, dataPath, artifactFinder, logger);

		Application application = new Application(logger);
		application.setRepository(repository);
		application.setVulnerabilityFinder(vulnerabilityFinder);

		return application.run(options);
	}

}
