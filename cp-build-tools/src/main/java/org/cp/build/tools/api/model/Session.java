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
package org.cp.build.tools.api.model;

import java.util.Objects;

import org.cp.build.tools.api.support.Utils;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract Data Type (ADT) and Spring {@link Service} bean modeling a user's current, interactive session
 * with the Codeprimate Build Tools shell.
 *
 * @author John Blum
 * @see org.cp.build.tools.api.model.Project
 * @see org.springframework.stereotype.Service
 * @since 2.0.0
 */
@Slf4j
@Getter
@Service
@SuppressWarnings("unused")
public class Session {

  @Nullable
  private volatile Project currentProject;

  @PostConstruct
  public void onInitialization() {
    getLogger().info("User is [{}]", getUsername());
    getLogger().info("Current working directory is [{}]", getWorkingDirectory());
  }

  protected Logger getLogger() {
    return log;
  }

  public boolean isProjectSet() {
    return Objects.nonNull(getCurrentProject());
  }

  public @NonNull Project requireProject() {
    return Utils.requireState(getCurrentProject(), "Project has not be set");
  }

  public @NonNull Session setProject(@Nullable Project project) {
    this.currentProject = project;
    return this;
  }

  public @NonNull String getUsername() {
    return System.getProperty("user.name");
  }

  public @NonNull String getWorkingDirectory() {
    return Utils.WORKING_DIRECTORY.getAbsolutePath();
  }
}
