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

import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

@DisableCachingByDefault(because = "JarHC report generation performs network lookups and is not worth caching")
public abstract class JarhcReportTask extends DefaultTask {

	/**
	 * The JarHC tool classpath. JarHC and its dependencies are resolved by the
	 * {@code jarhc} configuration (see {@link JarhcGradlePlugin}) and run in an
	 * isolated worker classloader, so they never leak onto the Gradle build's
	 * plugin classpath.
	 */
	@Classpath
	public abstract ConfigurableFileCollection getJarhcClasspath();

	@Classpath
	public abstract ConfigurableFileCollection getClasspath();

	@Classpath
	public abstract ConfigurableFileCollection getProvided();

	@Classpath
	public abstract ConfigurableFileCollection getRuntime();

	@Input
	public abstract ListProperty<String> getSections();

	@Input
	public abstract Property<Boolean> getSkipEmpty();

	@Input
	public abstract Property<Boolean> getSortRows();

	@Input
	public abstract Property<Integer> getRelease();

	@Input
	public abstract Property<String> getStrategy();

	@Input
	public abstract Property<Boolean> getRemoveVersion();

	@Input
	public abstract Property<Boolean> getUseArtifactName();

	@Input
	public abstract Property<Boolean> getIgnoreMissingAnnotations();

	@Input
	public abstract Property<Boolean> getIgnoreExactCopy();

	@Internal
	public abstract DirectoryProperty getDataDir();

	@Input
	public abstract Property<String> getReportTitle();

	@OutputFiles
	public abstract ConfigurableFileCollection getReportFiles();

	@Inject
	public abstract WorkerExecutor getWorkerExecutor();

	public JarhcReportTask() {
		setGroup("verification");
		setDescription("Generates a JarHC report.");
	}

	@TaskAction
	void run() {

		Logger logger = getLogger();

		// warn about deprecated options here, where the Gradle logger is available;
		// the worker uses a plain SLF4J logger whose output would otherwise be lost
		warnAboutDeprecatedOptions(logger);

		// Run JarHC in a worker with an isolated classloader that carries only the
		// JarHC classpath. This keeps JarHC and its dependencies (asm, slf4j,
		// maven-resolver, ...) off the Gradle plugin classpath, where they would
		// otherwise risk clashing with the versions Gradle itself ships.
		WorkQueue workQueue = getWorkerExecutor().classLoaderIsolation(spec ->
				spec.getClasspath().from(getJarhcClasspath()));

		workQueue.submit(JarhcWorkAction.class, parameters -> {
			parameters.getClasspath().from(getClasspath());
			parameters.getProvided().from(getProvided());
			parameters.getRuntime().from(getRuntime());
			parameters.getSections().set(getSections());
			parameters.getSkipEmpty().set(getSkipEmpty());
			parameters.getRelease().set(getRelease());
			parameters.getStrategy().set(getStrategy());
			parameters.getIgnoreMissingAnnotations().set(getIgnoreMissingAnnotations());
			parameters.getIgnoreExactCopy().set(getIgnoreExactCopy());
			parameters.getDataDir().set(getDataDir());
			parameters.getReportTitle().set(getReportTitle());
			parameters.getReportFiles().from(getReportFiles());
		});
	}

	private void warnAboutDeprecatedOptions(Logger logger) {
		if (getSortRows().isPresent()) {
			logger.warn("Option 'sortRows' has been deprecated and is ignored.");
		}
		if (getRemoveVersion().isPresent()) {
			logger.warn("Option 'removeVersion' has been deprecated and is ignored.");
		}
		if (getUseArtifactName().isPresent()) {
			logger.warn("Option 'useArtifactName' has been deprecated and is ignored.");
		}
	}

}
