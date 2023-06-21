/*
 * Copyright 2011-Present Author or Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cp.build.tools.core.model.support;

import java.io.File;
import java.util.stream.Stream;

import org.cp.build.tools.core.model.Project;

/**
 * Strategy interface used to resolve the {@link String name} for a {@link Project}.
 *
 * @author John Blum
 * @see org.cp.build.tools.core.model.Project
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public interface ProjectNameResolver {

  static Stream<Strategies> strategies() {
    return Stream.of(Strategies.values());
  }

  /**
   * Determines whether the {@link Project#getName()} can be resolved by this {@link ProjectNameResolver}
   * from the given {@link Object source}.
   *
   * @param source {@link Object} to evaluate.
   * @return a boolean value indicating whether the {@link Project#getName()} can be resolved
   * from the given {@link Object source}.
   */
  default boolean isResolvableFrom(Object source) {
    return source != null;
  }

  /**
   * Resolves a {@link Project#getName()} from the given {@link Object source}.
   *
   * @param source {@link Object} from which the {@link Project#getName()} will be resolved.
   * @return the resolved {@link String name} for a {@link Project}.
   */
  default String resolve(Object source) {
    return source != null ? source.getClass().getSimpleName() : null;
  }

  enum Strategies implements ProjectNameResolver {

    FILE_SYSTEM_DIRECTORY_NAME_RESOLVER {

      @Override
      public boolean isResolvableFrom(Object source) {
        return source instanceof File;
      }

      @Override
      public String resolve(Object source) {

        if (source instanceof File file) {
          file = file.isDirectory() ? file : file.getParentFile();
          return file.getName();
        }

        return null;
      }
    },

    MAVEN_PROJECT_NAME_RESOLVER {

      @Override
      public boolean isResolvableFrom(Object source) {
        return source instanceof org.apache.maven.project.MavenProject;
      }

      @Override
      public String resolve(Object source) {
        return (source instanceof org.apache.maven.project.MavenProject mavenProject) ? mavenProject.getName() : null;
      }
    },

    PROJECT_NAME_RESOLVER {

      @Override
      public boolean isResolvableFrom(Object source) {
        return source instanceof Project;
      }

      @Override
      public String resolve(Object source) {
        return (source instanceof Project project) ? project.getName() : null;
      }
    },

    DEFAULT
  }
}
