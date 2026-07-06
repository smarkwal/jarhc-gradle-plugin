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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class JarhcReportTaskTest {

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

		// prepare: task inputs, stubbed to distinct mocks so the wiring onto the
		// work parameters can be verified by instance
		ConfigurableFileCollection taskClasspath = mock(ConfigurableFileCollection.class);
		ConfigurableFileCollection taskProvided = mock(ConfigurableFileCollection.class);
		ConfigurableFileCollection taskRuntime = mock(ConfigurableFileCollection.class);
		ConfigurableFileCollection taskReportFiles = mock(ConfigurableFileCollection.class);
		when(task.getClasspath()).thenReturn(taskClasspath);
		when(task.getProvided()).thenReturn(taskProvided);
		when(task.getRuntime()).thenReturn(taskRuntime);
		when(task.getReportFiles()).thenReturn(taskReportFiles);

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

		ArgumentCaptor<Action<? super JarhcWorkParameters>> action = ArgumentCaptor.forClass(Action.class);
		verify(workQueue).submit(eq(JarhcWorkAction.class), action.capture());

		// execute the parameter-configuration action to cover wiring the task
		// inputs onto the work parameters
		JarhcWorkParameters parameters = mockWorkParameters();
		action.getValue().execute(parameters);

		// verify: each task input is wired onto the matching work parameter
		verify(parameters.getClasspath()).from(taskClasspath);
		verify(parameters.getProvided()).from(taskProvided);
		verify(parameters.getRuntime()).from(taskRuntime);
		verify(parameters.getReportFiles()).from(taskReportFiles);
	}

	@Test
	void run_warnsAboutDeprecatedOptions() {

		// prepare: task
		JarhcReportTask task = mock(JarhcReportTask.class);
		Logger logger = mock(Logger.class);
		when(task.getLogger()).thenReturn(logger);

		// all three deprecated options are present, so each logs a warning
		Property<Boolean> present = mock(Property.class);
		when(present.isPresent()).thenReturn(true);
		when(task.getSortRows()).thenReturn(present);
		when(task.getRemoveVersion()).thenReturn(present);
		when(task.getUseArtifactName()).thenReturn(present);

		// prepare: worker executor
		WorkerExecutor workerExecutor = mock(WorkerExecutor.class);
		WorkQueue workQueue = mock(WorkQueue.class);
		when(task.getWorkerExecutor()).thenReturn(workerExecutor);
		when(workerExecutor.classLoaderIsolation(any())).thenReturn(workQueue);

		doCallRealMethod().when(task).run();

		// test
		task.run();

		// verify: a deprecation warning is logged for each deprecated option
		verify(logger, times(3)).warn(anyString());
	}

	// a JarhcWorkParameters mock whose getters return typed mocks, so the
	// parameter-configuration action can be executed against it
	private static JarhcWorkParameters mockWorkParameters() {
		JarhcWorkParameters parameters = mock(JarhcWorkParameters.class);
		when(parameters.getClasspath()).thenReturn(mock(ConfigurableFileCollection.class));
		when(parameters.getProvided()).thenReturn(mock(ConfigurableFileCollection.class));
		when(parameters.getRuntime()).thenReturn(mock(ConfigurableFileCollection.class));
		when(parameters.getSections()).thenReturn(mock(ListProperty.class));
		when(parameters.getSkipEmpty()).thenReturn(mock(Property.class));
		when(parameters.getRelease()).thenReturn(mock(Property.class));
		when(parameters.getStrategy()).thenReturn(mock(Property.class));
		when(parameters.getIgnoreMissingAnnotations()).thenReturn(mock(Property.class));
		when(parameters.getIgnoreExactCopy()).thenReturn(mock(Property.class));
		when(parameters.getDataDir()).thenReturn(mock(DirectoryProperty.class));
		when(parameters.getReportTitle()).thenReturn(mock(Property.class));
		when(parameters.getReportFiles()).thenReturn(mock(ConfigurableFileCollection.class));
		return parameters;
	}

}
