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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("DuplicatedCode")
class JarhcGradlePluginFunctionalTest {

	@TempDir
	File projectDir;

	private File getBuildFile() {
		return new File(projectDir, "build.gradle.kts");
	}

	private File getSettingsFile() {
		return new File(projectDir, "settings.gradle.kts");
	}

	@Test
	void canRunTask_withDefaultConfig() throws IOException {

		// prepare
		String projectPath = "projects/default-config";
		writeTextFile(getSettingsFile(), readTextResource(projectPath + "/settings.gradle.kts"));
		writeTextFile(getBuildFile(), readTextResource(projectPath + "/build.gradle.kts"));
		Path expectedDataPath = projectDir.toPath().resolve("build/jarhc-data");
		Path expectedHtmlReportPath = projectDir.toPath().resolve("build/reports/jarhc/jarhc-report.html");
		Path expectedTextReportPath = projectDir.toPath().resolve("build/reports/jarhc/jarhc-report.txt");
		String expectedOutput = readTextResource(projectPath + "/expected-output.txt");
		String expectedTextReport = readTextResource(projectPath + "/expected-report.txt");

		// test
		BuildResult result = runTask();

		// assert
		String output = result.getOutput();
		assertTrue(output.contains(expectedOutput));
		assertTrue(Files.isDirectory(expectedDataPath));
		assertTrue(Files.isRegularFile(expectedHtmlReportPath));
		assertTrue(Files.isRegularFile(expectedTextReportPath));

		String textReport = Files.readString(expectedTextReportPath);
		if (textReport.contains("Java Runtime")) { // remove section "Java Runtime"
			textReport = textReport.substring(0, textReport.indexOf("Java Runtime"));
		}
		assertEquals(expectedTextReport, textReport);
		System.out.println(textReport);
	}

	@Test
	void canRunTask_withFullConfig() throws IOException {

		// prepare
		String projectPath = "projects/full-config";
		writeTextFile(getSettingsFile(), readTextResource(projectPath + "/settings.gradle.kts"));
		writeTextFile(getBuildFile(), readTextResource(projectPath + "/build.gradle.kts"));
		Path expectedDataPath = projectDir.toPath().resolve("jarhc-data");
		Path expectedHtmlReportPath = projectDir.toPath().resolve("jarhc-report-asm-9.4.html");
		Path expectedTextReportPath = projectDir.toPath().resolve("jarhc-report-asm-9.4.txt");
		String expectedOutput = readTextResource(projectPath + "/expected-output.txt");
		String expectedTextReport = readTextResource(projectPath + "/expected-report.txt");

		// test
		BuildResult result = runTask();

		// assert
		String output = result.getOutput();
		assertTrue(output.contains(expectedOutput));
		assertTrue(Files.isDirectory(expectedDataPath));
		assertTrue(Files.isRegularFile(expectedHtmlReportPath));
		assertTrue(Files.isRegularFile(expectedTextReportPath));

		String textReport = Files.readString(expectedTextReportPath);
		assertEquals(expectedTextReport, textReport);
		System.out.println(textReport);
	}

	private BuildResult runTask() {
		GradleRunner runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("jarhcReport", "--stacktrace"); // "--info"
		runner.withProjectDir(projectDir);
		return runner.build();
	}

	private String readTextResource(String resource) throws IOException {
		try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(resource)) {
			if (stream == null) throw new IOException("Resource not found: " + resource);
			return IOUtils.toString(stream, StandardCharsets.UTF_8);
		}
	}

	private void writeTextFile(File file, String text) throws IOException {
		try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
			writer.write(text);
		}
	}

}
