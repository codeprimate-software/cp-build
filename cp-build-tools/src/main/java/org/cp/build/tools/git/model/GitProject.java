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
package org.cp.build.tools.git.model;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cp.build.tools.core.model.Project;
import org.cp.build.tools.core.support.Utils;
import org.cp.build.tools.git.support.GitException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Abstract Data Type (ADT) modeling a {@literal Git} project
 *
 * @author John Blum
 * @see org.cp.build.tools.core.model.Project
 * @see org.eclipse.jgit.api.Git
 * @since 2.0.0
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class GitProject {

  private static final Map<Project, GitProject> cache = new ConcurrentHashMap<>();

  protected static final String GIT_DIRECTORY_NAME = ".git";

  /**
   * Factory method used to construct a new {@link GitProject} initialized from the given, required {@link Project}.
   *
   * @param project {@link Project} to adapt as a {@link GitProject}; must not be {@literal null}.
   * @return a new {@link GitProject} initialized from the given {@link Project}.
   * @throws GitException if the given {@link Project} is not a {@literal Git-based} project.
   * @throws IllegalArgumentException if the given {@link Project} is {@literal null},
   * or the {@link Project#getDirectory() project directory} does not exist or is not valid,
   * or a {@link File#isDirectory() project .git directory} could not be found.
   * @see org.cp.build.tools.core.model.Project
   */
  public static @NonNull GitProject from(@NonNull Project project) {

    Assert.notNull(project, "Project is required");

    return cache.computeIfAbsent(project, it -> {
      try {
        return new GitProject(newGit(it), it);
      }
      catch (IOException cause) {
        String message = String.format("Failed to create GitProject from Project [%s]", it);
        throw new GitException(message, cause);
      }
    });
  }

  private static @NonNull Git newGit(@NonNull Project project) throws IOException {

    File projectDirectory = project.getDirectory();

    Assert.isTrue(Utils.nullSafeIsDirectory(projectDirectory),
      () -> String.format("Project directory [%s] must be an existing, valid directory", projectDirectory));

    File gitDirectory = findGitDirectory(projectDirectory);

    Assert.notNull(gitDirectory,
      () -> String.format("Project [%1$s] in directory [%2$s] must contain a [%3$s] directory",
        project.getName(), projectDirectory, GIT_DIRECTORY_NAME));

    return new Git(FileRepositoryBuilder.create(gitDirectory));
  }

  private static @Nullable File findGitDirectory(@Nullable File directory) {

    if (Utils.nullSafeIsDirectory(directory)) {
      File gitDirectory = new File(directory, GIT_DIRECTORY_NAME);
      return Utils.nullSafeIsDirectory(gitDirectory) ? gitDirectory : findGitDirectory(directory.getParentFile());
    }

    return null;
  }

  public final Git git;

  private final Project project;

}
