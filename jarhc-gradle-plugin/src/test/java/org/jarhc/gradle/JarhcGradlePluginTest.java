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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.jarhc.app.Options;
import org.jarhc.java.ClassLoaderStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class JarhcGradlePluginTest {

	@Mock
	Project project;

	@Mock
	Logger logger;

	@Mock
	JarhcReportTask task;

	@BeforeEach
	void setUp() {

		// setup: project
		when(project.getLogger()).thenReturn(logger);
		ConfigurationContainer configurations = mock(ConfigurationContainer.class);
		when(project.getConfigurations()).thenReturn(configurations);
		Configuration configuration = mock(Configuration.class);
		when(configurations.getByName("runtimeClasspath")).thenReturn(configuration);
		when(configuration.iterator()).thenReturn(List.of(new File("/jarhc/a.jar")).iterator());

		// setup: task
		when(task.getProject()).thenReturn(project);

	}

	@Test
	void apply() {

		// prepare
		Project project = ProjectBuilder.builder().build();
		PluginContainer plugins = project.getPlugins();
		TaskContainer tasks = project.getTasks();

		// test
		plugins.apply("org.jarhc");

		// assert
		Task task = tasks.findByName("jarhcReport");
		assertNotNull(task);
		assertTrue(task instanceof JarhcReportTask);
		assertEquals("verification", task.getGroup());
		assertEquals("Generates a JarHC report.", task.getDescription());
		assertTrue(task.getDependsOn().isEmpty());
	}

	@Test
	void run() {

		// prepare: options
		Options options = new Options();

		// prepare: task
		when(task.createOptions(project, logger)).thenReturn(options);
		when(task.runJarHC(options, logger)).thenReturn(0);
		doCallRealMethod().when(task).run();

		// test
		task.run();

		// verify
		verify(logger).info("JarHC Report Task");
		verify(task).runJarHC(options, logger);
		verify(logger).info("JarHC exit code: {}", 0);
	}

	@Test
	void run_throwsGradleException_whenExitCodeIsNotZero() {

		// prepare: options
		Options options = new Options();

		// prepare: task
		when(task.createOptions(project, logger)).thenReturn(options);
		when(task.runJarHC(options, logger)).thenReturn(123);
		doCallRealMethod().when(task).run();

		// test
		assertThrows(GradleException.class, () -> task.run());

		// verify
		verify(logger).info("JarHC Report Task");
		verify(task).runJarHC(options, logger);
		verify(logger).info("JarHC exit code: {}", 123);
	}

	@Test
	void runJarHC(@TempDir File tempDir) {

		// prepare: options
		File dataDir = new File(tempDir, "data");
		File reportFile = new File(tempDir, "report.html");
		Options options = new Options();
		options.setDataPath(dataDir.getAbsolutePath());
		options.addReportFile(reportFile.getAbsolutePath());

		// prepare: task
		when(task.runJarHC(options, logger)).thenCallRealMethod();

		// test
		int exitCode = task.runJarHC(options, logger);

		// assert
		assertEquals(0, exitCode);
		assertTrue(reportFile.exists());
	}

	@Test
	void createOptions_withDefaultConfig() {

		// prepare: task
		when(task.getClasspath()).thenReturn(null);

		when(task.getProvided()).thenReturn(null);

		when(task.getRuntime()).thenReturn(null);

		ListProperty<String> sections = mock(ListProperty.class);
		when(sections.isPresent()).thenReturn(false);
		when(task.getSections()).thenReturn(sections);

		Property<Boolean> skipEmpty = mock(Property.class);
		when(skipEmpty.isPresent()).thenReturn(false);
		when(task.getSkipEmpty()).thenReturn(skipEmpty);

		Property<Boolean> sortRows = mock(Property.class);
		when(sortRows.isPresent()).thenReturn(false);
		when(task.getSortRows()).thenReturn(sortRows);

		Property<Integer> release = mock(Property.class);
		when(release.isPresent()).thenReturn(false);
		when(task.getRelease()).thenReturn(release);

		Property<String> strategy = mock(Property.class);
		when(strategy.isPresent()).thenReturn(false);
		when(task.getStrategy()).thenReturn(strategy);

		Property<Boolean> removeVersion = mock(Property.class);
		when(removeVersion.isPresent()).thenReturn(false);
		when(task.getRemoveVersion()).thenReturn(removeVersion);

		Property<Boolean> useArtifactName = mock(Property.class);
		when(useArtifactName.isPresent()).thenReturn(false);
		when(task.getUseArtifactName()).thenReturn(useArtifactName);

		Property<Boolean> ignoreMissingAnnotations = mock(Property.class);
		when(ignoreMissingAnnotations.isPresent()).thenReturn(false);
		when(task.getIgnoreMissingAnnotations()).thenReturn(ignoreMissingAnnotations);

		Property<Boolean> ignoreExactCopt = mock(Property.class);
		when(ignoreExactCopt.isPresent()).thenReturn(false);
		when(task.getIgnoreExactCopy()).thenReturn(ignoreExactCopt);

		Directory directory = mock(Directory.class);
		when(directory.getAsFile()).thenReturn(new File("/jarhc/data"));
		DirectoryProperty dataDir = mock(DirectoryProperty.class);
		when(dataDir.isPresent()).thenReturn(true);
		when(dataDir.get()).thenReturn(directory);
		when(task.getDataDir()).thenReturn(dataDir);

		Property<String> reportTitle = mock(Property.class);
		when(reportTitle.isPresent()).thenReturn(false);
		when(task.getReportTitle()).thenReturn(reportTitle);

		when(task.getReportFiles()).thenReturn(null);

		when(task.createOptions(project, logger)).thenCallRealMethod();

		// test
		Options options = task.createOptions(project, logger);

		// assert
		assertNotNull(options);
		assertEquals(List.of("/jarhc/a.jar"), options.getClasspathJarPaths());
		assertEquals(List.of(), options.getProvidedJarPaths());
		assertEquals(List.of(), options.getRuntimeJarPaths());
		assertNull(options.getSections());
		assertFalse(options.isSkipEmpty());
		assertEquals(11, options.getRelease());
		assertEquals(ClassLoaderStrategy.ParentLast, options.getClassLoaderStrategy());
		assertFalse(options.isIgnoreMissingAnnotations());
		assertFalse(options.isIgnoreExactCopy());
		assertEquals("/jarhc/data", options.getDataPath());
		assertEquals("JAR Health Check Report", options.getReportTitle());
		assertEquals(List.of(), options.getReportFiles());
	}

	@Test
	void createOptions_withFullConfig() {

		// prepare: task
		ConfigurableFileCollection classpath = mock(ConfigurableFileCollection.class);
		when(classpath.iterator()).thenReturn(List.of(new File("/jarhc/a.jar")).iterator());
		when(task.getClasspath()).thenReturn(classpath);

		ConfigurableFileCollection provided = mock(ConfigurableFileCollection.class);
		when(provided.iterator()).thenReturn(List.of(new File("/jarhc/b.jar")).iterator());
		when(task.getProvided()).thenReturn(provided);

		ConfigurableFileCollection runtime = mock(ConfigurableFileCollection.class);
		when(runtime.iterator()).thenReturn(List.of(new File("/jarhc/c.jar")).iterator());
		when(task.getRuntime()).thenReturn(runtime);

		ListProperty<String> sections = mock(ListProperty.class);
		when(sections.isPresent()).thenReturn(true);
		when(sections.get()).thenReturn(List.of("jf", "bl"));
		when(task.getSections()).thenReturn(sections);

		Property<Boolean> skipEmpty = mock(Property.class);
		when(skipEmpty.isPresent()).thenReturn(true);
		when(skipEmpty.get()).thenReturn(true);
		when(task.getSkipEmpty()).thenReturn(skipEmpty);

		Property<Boolean> sortRows = mock(Property.class);
		when(sortRows.isPresent()).thenReturn(true);
		when(sortRows.get()).thenReturn(true);
		when(task.getSortRows()).thenReturn(sortRows);

		Property<Integer> release = mock(Property.class);
		when(release.isPresent()).thenReturn(true);
		when(release.get()).thenReturn(17);
		when(task.getRelease()).thenReturn(release);

		Property<String> strategy = mock(Property.class);
		when(strategy.isPresent()).thenReturn(true);
		when(strategy.get()).thenReturn("ParentFirst");
		when(task.getStrategy()).thenReturn(strategy);

		Property<Boolean> removeVersion = mock(Property.class);
		when(removeVersion.isPresent()).thenReturn(true);
		when(removeVersion.get()).thenReturn(true);
		when(task.getRemoveVersion()).thenReturn(removeVersion);

		Property<Boolean> useArtifactName = mock(Property.class);
		when(useArtifactName.isPresent()).thenReturn(true);
		when(useArtifactName.get()).thenReturn(true);
		when(task.getUseArtifactName()).thenReturn(useArtifactName);

		Property<Boolean> ignoreMissingAnnotations = mock(Property.class);
		when(ignoreMissingAnnotations.isPresent()).thenReturn(true);
		when(ignoreMissingAnnotations.get()).thenReturn(true);
		when(task.getIgnoreMissingAnnotations()).thenReturn(ignoreMissingAnnotations);

		Property<Boolean> ignoreExactCopt = mock(Property.class);
		when(ignoreExactCopt.isPresent()).thenReturn(true);
		when(ignoreExactCopt.get()).thenReturn(true);
		when(task.getIgnoreExactCopy()).thenReturn(ignoreExactCopt);

		Directory directory = mock(Directory.class);
		when(directory.getAsFile()).thenReturn(new File("/jarhc/data"));
		DirectoryProperty dataDir = mock(DirectoryProperty.class);
		when(dataDir.isPresent()).thenReturn(true);
		when(dataDir.get()).thenReturn(directory);
		when(task.getDataDir()).thenReturn(dataDir);

		Property<String> reportTitle = mock(Property.class);
		when(reportTitle.isPresent()).thenReturn(true);
		when(reportTitle.get()).thenReturn("JarHC Test Report");
		when(task.getReportTitle()).thenReturn(reportTitle);

		ConfigurableFileCollection reportFiles = mock(ConfigurableFileCollection.class);
		when(reportFiles.iterator()).thenReturn(List.of(new File("/jarhc/report.html"), new File("/jarhc/report.txt")).iterator());
		when(task.getReportFiles()).thenReturn(reportFiles);

		when(task.createOptions(project, logger)).thenCallRealMethod();

		// test
		Options options = task.createOptions(project, logger);

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
