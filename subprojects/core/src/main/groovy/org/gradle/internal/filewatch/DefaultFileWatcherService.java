/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.internal.filewatch;

import org.gradle.api.JavaVersion;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.Cast;
import org.gradle.internal.concurrent.CompositeStoppable;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.reflect.DirectInstantiator;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class DefaultFileWatcherService implements FileWatcherService, Stoppable {
    private final ExecutorService executor;
    private final JavaVersion javaVersion;
    private final ClassLoader classLoader;
    private final FileWatcherService fileWatcherService;
    private final static Logger LOG = Logging.getLogger(DefaultFileWatcherService.class);

    private static class NoOpFileWatcherService implements FileWatcherService {
        @Override
        public Stoppable watch(FileWatchInputs inputs, Runnable callback) throws IOException {
            return CompositeStoppable.NO_OP_STOPPABLE;
        }
    }

    public DefaultFileWatcherService(ExecutorFactory executorFactory) {
        this(JavaVersion.current(), DefaultFileWatcherService.class.getClassLoader(), executorFactory);
    }

    DefaultFileWatcherService(JavaVersion javaVersion, ClassLoader classLoader, ExecutorFactory executorFactory) {
        this.javaVersion = javaVersion;
        this.classLoader = classLoader;
        this.executor = executorFactory.create("filewatcher");
        this.fileWatcherService = createFileWatcherService();
    }

    protected FileWatcherService createFileWatcherService() {
        if(javaVersion.isJava7Compatible()) {
            Class clazz;
            try {
                clazz = classLoader.loadClass("org.gradle.internal.filewatch.jdk7.DefaultFileWatcher");
                return Cast.uncheckedCast(DirectInstantiator.instantiate(clazz, executor));
            } catch (ClassNotFoundException e) {
                LOG.error("Could not load JDK7 class with a JDK7+ JVM, falling back to no-op implementation.");
            }
        }
        LOG.warn("Using no-op file watcher service.");
        // TODO: Maybe we'll eventually support Java 6
        return new NoOpFileWatcherService();
    }

    @Override
    public void stop() {
        executor.shutdown();
    }

    @Override
    public Stoppable watch(FileWatchInputs inputs, Runnable callback) throws IOException {
        return fileWatcherService.watch(inputs, callback);
    }
}