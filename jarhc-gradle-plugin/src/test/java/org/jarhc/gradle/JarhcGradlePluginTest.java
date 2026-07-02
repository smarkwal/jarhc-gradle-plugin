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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.jarhc.app.Options;
import org.jarhc.java.ClassLoaderStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class JarhcGradlePluginTest {

	@Test
	void apply() {

		// prepare
		Project project = ProjectBuilder.builder().build();
		PluginContainer plugins = project.getPlugins();
		TaskContainer tasks = project.getTasks();

		// test
		plugins.apply("org.jarhc");

		// assert: task
		Task task = tasks.findByName("jarhcReport");
		assertNotNull(task);
		assertInstanceOf(JarhcReportTask.class, task);
		assertEquals("verification", task.getGroup());
		assertEquals("Generates a JarHC report.", task.getDescription());
		assertTrue(task.getDependsOn().isEmpty());

		// assert: 'jarhc' configuration with the default JarHC dependency
		assertNotNull(project.getConfigurations().findByName(JarhcGradlePlugin.JARHC_CONFIGURATION_NAME));
	}

	@Test
	void run_submitsWorkToIsolatedWorker() {

		// prepare: task
		JarhcReportTask task = mock(JarhcReportTask.class);
		Logger logger = mock(Logger.class);
		when(task.getLogger()).thenReturn(logger);

		// the deprecated options are queried before submitting the work
		when(task.getSortRows()).thenReturn(mock(Property.class));
		when(task.getRemoveVersion()).thenReturn(mock(Property.class));
		when(task.getUseArtifactName()).thenReturn(mock(Property.class));

		// prepare: worker executor
		WorkerExecutor workerExecutor = mock(WorkerExecutor.class);
		WorkQueue workQueue = mock(WorkQueue.class);
		when(task.getWorkerExecutor()).thenReturn(workerExecutor);
		when(workerExecutor.classLoaderIsolation(any())).thenReturn(workQueue);

		doCallRealMethod().when(task).run();

		// test
		task.run();

		// verify: work submitted to an isolated worker classloader
		verify(workerExecutor).classLoaderIsolation(any());
		verify(workQueue).submit(eq(JarhcWorkAction.class), any());
	}

	@Test
	void runJarHC(@TempDir File tempDir) {

		// prepare: options
		File dataDir = new File(tempDir, "data");
		File reportFile = new File(tempDir, "report.html");
		Options options = new Options();
		options.setDataPath(dataDir.getAbsolutePath());
		options.addReportFile(reportFile.getAbsolutePath());

		// test
		int exitCode = JarhcRunner.runJarHC(options, mock(Logger.class));

		// assert
		assertEquals(0, exitCode);
		assertTrue(reportFile.exists());
	}

	@Test
	void createOptions_withDefaultConfig() {

		// prepare: work parameters
		JarhcWorkParameters parameters = mock(JarhcWorkParameters.class);

		ConfigurableFileCollection classpath = mock(ConfigurableFileCollection.class);
		when(classpath.iterator()).thenReturn(List.of(new File("/jarhc/a.jar")).iterator());
		when(parameters.getClasspath()).thenReturn(classpath);

		when(parameters.getProvided()).thenReturn(null);

		when(parameters.getRuntime()).thenReturn(null);

		ListProperty<String> sections = mock(ListProperty.class);
		when(sections.isPresent()).thenReturn(false);
		when(parameters.getSections()).thenReturn(sections);

		Property<Boolean> skipEmpty = mock(Property.class);
		when(skipEmpty.isPresent()).thenReturn(false);
		when(parameters.getSkipEmpty()).thenReturn(skipEmpty);

		Property<Integer> release = mock(Property.class);
		when(release.isPresent()).thenReturn(false);
		when(parameters.getRelease()).thenReturn(release);

		Property<String> strategy = mock(Property.class);
		when(strategy.isPresent()).thenReturn(false);
		when(parameters.getStrategy()).thenReturn(strategy);

		Property<Boolean> ignoreMissingAnnotations = mock(Property.class);
		when(ignoreMissingAnnotations.isPresent()).thenReturn(false);
		when(parameters.getIgnoreMissingAnnotations()).thenReturn(ignoreMissingAnnotations);

		Property<Boolean> ignoreExactCopy = mock(Property.class);
		when(ignoreExactCopy.isPresent()).thenReturn(false);
		when(parameters.getIgnoreExactCopy()).thenReturn(ignoreExactCopy);

		Directory directory = mock(Directory.class);
		when(directory.getAsFile()).thenReturn(new File("/jarhc/data"));
		DirectoryProperty dataDir = mock(DirectoryProperty.class);
		when(dataDir.isPresent()).thenReturn(true);
		when(dataDir.get()).thenReturn(directory);
		when(parameters.getDataDir()).thenReturn(dataDir);

		Property<String> reportTitle = mock(Property.class);
		when(reportTitle.isPresent()).thenReturn(false);
		when(parameters.getReportTitle()).thenReturn(reportTitle);

		when(parameters.getReportFiles()).thenReturn(null);

		// test
		Options options = JarhcRunner.createOptions(parameters, mock(Logger.class));

		// assert
		assertNotNull(options);
		assertEquals(List.of("/jarhc/a.jar"), options.getClasspathJarPaths());
		assertEquals(List.of(), options.getProvidedJarPaths());
		assertEquals(List.of(), options.getRuntimeJarPaths());
		assertNull(options.getSections());
		assertFalse(options.isSkipEmpty());
		// default release is the Java version running the tests (see Options)
		assertEquals(Runtime.version().feature(), options.getRelease());
		assertEquals(ClassLoaderStrategy.ParentLast, options.getClassLoaderStrategy());
		assertFalse(options.isIgnoreMissingAnnotations());
		assertFalse(options.isIgnoreExactCopy());
		assertEquals("/jarhc/data", options.getDataPath());
		assertEquals("JAR Health Check Report", options.getReportTitle());
		assertEquals(List.of(), options.getReportFiles());
	}

	@Test
	void createOptions_withFullConfig() {

		// prepare: work parameters
		JarhcWorkParameters parameters = mock(JarhcWorkParameters.class);

		ConfigurableFileCollection classpath = mock(ConfigurableFileCollection.class);
		when(classpath.iterator()).thenReturn(List.of(new File("/jarhc/a.jar")).iterator());
		when(parameters.getClasspath()).thenReturn(classpath);

		ConfigurableFileCollection provided = mock(ConfigurableFileCollection.class);
		when(provided.isEmpty()).thenReturn(false);
		when(provided.iterator()).thenReturn(List.of(new File("/jarhc/b.jar")).iterator());
		when(parameters.getProvided()).thenReturn(provided);

		ConfigurableFileCollection runtime = mock(ConfigurableFileCollection.class);
		when(runtime.isEmpty()).thenReturn(false);
		when(runtime.iterator()).thenReturn(List.of(new File("/jarhc/c.jar")).iterator());
		when(parameters.getRuntime()).thenReturn(runtime);

		ListProperty<String> sections = mock(ListProperty.class);
		when(sections.isPresent()).thenReturn(true);
		when(sections.get()).thenReturn(List.of("jf", "bl"));
		when(parameters.getSections()).thenReturn(sections);

		Property<Boolean> skipEmpty = mock(Property.class);
		when(skipEmpty.isPresent()).thenReturn(true);
		when(skipEmpty.get()).thenReturn(true);
		when(parameters.getSkipEmpty()).thenReturn(skipEmpty);

		Property<Integer> release = mock(Property.class);
		when(release.isPresent()).thenReturn(true);
		when(release.get()).thenReturn(17);
		when(parameters.getRelease()).thenReturn(release);

		Property<String> strategy = mock(Property.class);
		when(strategy.isPresent()).thenReturn(true);
		when(strategy.get()).thenReturn("ParentFirst");
		when(parameters.getStrategy()).thenReturn(strategy);

		Property<Boolean> ignoreMissingAnnotations = mock(Property.class);
		when(ignoreMissingAnnotations.isPresent()).thenReturn(true);
		when(ignoreMissingAnnotations.get()).thenReturn(true);
		when(parameters.getIgnoreMissingAnnotations()).thenReturn(ignoreMissingAnnotations);

		Property<Boolean> ignoreExactCopy = mock(Property.class);
		when(ignoreExactCopy.isPresent()).thenReturn(true);
		when(ignoreExactCopy.get()).thenReturn(true);
		when(parameters.getIgnoreExactCopy()).thenReturn(ignoreExactCopy);

		Directory directory = mock(Directory.class);
		when(directory.getAsFile()).thenReturn(new File("/jarhc/data"));
		DirectoryProperty dataDir = mock(DirectoryProperty.class);
		when(dataDir.isPresent()).thenReturn(true);
		when(dataDir.get()).thenReturn(directory);
		when(parameters.getDataDir()).thenReturn(dataDir);

		Property<String> reportTitle = mock(Property.class);
		when(reportTitle.isPresent()).thenReturn(true);
		when(reportTitle.get()).thenReturn("JarHC Test Report");
		when(parameters.getReportTitle()).thenReturn(reportTitle);

		ConfigurableFileCollection reportFiles = mock(ConfigurableFileCollection.class);
		when(reportFiles.isEmpty()).thenReturn(false);
		when(reportFiles.iterator()).thenReturn(List.of(new File("/jarhc/report.html"), new File("/jarhc/report.txt")).iterator());
		when(parameters.getReportFiles()).thenReturn(reportFiles);

		// test
		Options options = JarhcRunner.createOptions(parameters, mock(Logger.class));

		// assert
		assertNotNull(options);
		assertEquals(List.of("/jarhc/a.jar"), options.getClasspathJarPaths());
		assertEquals(List.of("/jarhc/b.jar"), options.getProvidedJarPaths());
		assertEquals(List.of("/jarhc/c.jar"), options.getRuntimeJarPaths());
		assertEquals(List.of("jf", "bl"), options.getSections());
		assertTrue(options.isSkipEmpty());
		assertEquals(17, options.getRelease());
		assertEquals(ClassLoaderStrategy.ParentFirst, options.getClassLoaderStrategy());
		assertTrue(options.isIgnoreMissingAnnotations());
		assertTrue(options.isIgnoreExactCopy());
		assertEquals("/jarhc/data", options.getDataPath());
		assertEquals("JarHC Test Report", options.getReportTitle());
		assertEquals(List.of("/jarhc/report.html", "/jarhc/report.txt"), options.getReportFiles());
	}

}
