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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
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
class JarhcRunnerTest {

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
		assertEquals(List.of(absPath("/jarhc/a.jar")), options.getClasspathJarPaths());
		assertEquals(List.of(), options.getProvidedJarPaths());
		assertEquals(List.of(), options.getRuntimeJarPaths());
		assertNull(options.getSections());
		assertFalse(options.isSkipEmpty());
		// default release is the Java version running the tests (see Options)
		assertEquals(Runtime.version().feature(), options.getRelease());
		assertEquals(ClassLoaderStrategy.ParentLast, options.getClassLoaderStrategy());
		assertFalse(options.isIgnoreMissingAnnotations());
		assertFalse(options.isIgnoreExactCopy());
		assertEquals(absPath("/jarhc/data"), options.getDataPath());
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
		assertEquals(List.of(absPath("/jarhc/a.jar")), options.getClasspathJarPaths());
		assertEquals(List.of(absPath("/jarhc/b.jar")), options.getProvidedJarPaths());
		assertEquals(List.of(absPath("/jarhc/c.jar")), options.getRuntimeJarPaths());
		assertEquals(List.of("jf", "bl"), options.getSections());
		assertTrue(options.isSkipEmpty());
		assertEquals(17, options.getRelease());
		assertEquals(ClassLoaderStrategy.ParentFirst, options.getClassLoaderStrategy());
		assertTrue(options.isIgnoreMissingAnnotations());
		assertTrue(options.isIgnoreExactCopy());
		assertEquals(absPath("/jarhc/data"), options.getDataPath());
		assertEquals("JarHC Test Report", options.getReportTitle());
		assertEquals(List.of(absPath("/jarhc/report.html"), absPath("/jarhc/report.txt")), options.getReportFiles());
	}

	@Test
	void createOptions_throwsWhenDataPathMissing() {

		// prepare: work parameters without a data directory
		JarhcWorkParameters parameters = defaultCreateOptionsParameters();
		DirectoryProperty dataDir = mock(DirectoryProperty.class);
		when(dataDir.isPresent()).thenReturn(false);
		when(parameters.getDataDir()).thenReturn(dataDir);

		// test and assert
		GradleException exception = assertThrows(GradleException.class,
				() -> JarhcRunner.createOptions(parameters, mock(Logger.class)));
		assertEquals("No data path specified.", exception.getMessage());
	}

	@Test
	void createOptions_ignoresEmptySections() {

		// prepare: an empty (but present) section list
		JarhcWorkParameters parameters = defaultCreateOptionsParameters();
		ListProperty<String> sections = mock(ListProperty.class);
		when(sections.isPresent()).thenReturn(true);
		when(sections.get()).thenReturn(List.of());
		when(parameters.getSections()).thenReturn(sections);

		// test
		Options options = JarhcRunner.createOptions(parameters, mock(Logger.class));

		// assert: an empty section list does not override the default (all sections)
		assertNull(options.getSections());
	}

	@Test
	void createOptions_ignoresNonPositiveRelease() {

		// prepare: a present but non-positive release value
		JarhcWorkParameters parameters = defaultCreateOptionsParameters();
		Property<Integer> release = mock(Property.class);
		when(release.isPresent()).thenReturn(true);
		when(release.get()).thenReturn(0);
		when(parameters.getRelease()).thenReturn(release);

		// test
		Options options = JarhcRunner.createOptions(parameters, mock(Logger.class));

		// assert: release 0 does not override the default release
		assertEquals(Runtime.version().feature(), options.getRelease());
	}

	@Test
	void createOptions_logsAbsolutePathsWhenDebugEnabled() {

		// prepare: a logger with debug enabled
		JarhcWorkParameters parameters = defaultCreateOptionsParameters();
		Logger logger = mock(Logger.class);
		when(logger.isDebugEnabled()).thenReturn(true);

		// test
		Options options = JarhcRunner.createOptions(parameters, logger);

		// assert: in debug mode the absolute path is logged instead of the file name
		assertEquals(List.of(absPath("/jarhc/a.jar")), options.getClasspathJarPaths());
		verify(logger).info("- {}", absPath("/jarhc/a.jar"));
	}

	@Test
	void runJarHC_throwsWhenDataPathNull() {

		// prepare: options without a data path
		Options options = new Options();

		// test and assert
		GradleException exception = assertThrows(GradleException.class,
				() -> JarhcRunner.runJarHC(options, mock(Logger.class)));
		assertEquals("Data path is not set", exception.getMessage());
	}

	@Test
	void runJarHC_throwsWhenDirectoryCannotBeCreated(@TempDir File tempDir) throws IOException {

		// prepare: a data path under a regular file, so the directory cannot be created
		File file = new File(tempDir, "not-a-directory");
		assertTrue(file.createNewFile());
		File dataDir = new File(file, "data");
		Options options = new Options();
		options.setDataPath(dataDir.getAbsolutePath());

		// test and assert
		GradleException exception = assertThrows(GradleException.class,
				() -> JarhcRunner.runJarHC(options, mock(Logger.class)));
		assertTrue(exception.getMessage().startsWith("Failed to create directory:"));
	}

	// the platform-specific absolute path for the given path, matching the
	// File.getAbsolutePath() transformation the production code applies, so the
	// assertions stay portable (e.g. on Windows development machines)
	private static String absPath(String path) {
		return new File(path).getAbsolutePath();
	}

	// a JarhcWorkParameters mock configured like createOptions_withDefaultConfig:
	// one classpath JAR, no optional inputs set, a present data directory. Tests
	// override individual getters to exercise specific branches.
	private static JarhcWorkParameters defaultCreateOptionsParameters() {
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

		return parameters;
	}

}
