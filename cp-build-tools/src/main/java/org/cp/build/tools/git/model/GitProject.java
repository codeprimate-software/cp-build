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

  public static GitProject from(@NonNull Project project) {

    return cache.computeIfAbsent(project, it -> {

      Assert.notNull(it, "Project is required");
      Assert.notNull(it.getDirectory(), "Project directory is required");

      File gitDirectory = findGitDirectory(project);

      Assert.notNull(gitDirectory,
        () -> String.format("Project [%s] in directory [%s] must contain a [%s] directory",
          it.getName(), it.getDirectory(), GIT_DIRECTORY_NAME));

      try {
        Git git = new Git(FileRepositoryBuilder.create(gitDirectory));
        return new GitProject(git, it);
      }
      catch (IOException cause) {
        String message = String.format("Failed to create GitProject from Project [%s]", it);
        throw new GitException(message, cause);
      }
    });
  }

  private static File findGitDirectory(@NonNull Project project) {
    return findGitDirectory(project.getDirectory());
  }

  private static File findGitDirectory(@Nullable File directory) {

    if (Utils.nullSafeIsDirectory(directory)) {
      File gitDirectory = new File(directory, GIT_DIRECTORY_NAME);
      return Utils.nullSafeIsDirectory(gitDirectory) ? gitDirectory : findGitDirectory(directory.getParentFile());
    }

    return null;
  }

  public final Git git;

  private final Project project;

}
