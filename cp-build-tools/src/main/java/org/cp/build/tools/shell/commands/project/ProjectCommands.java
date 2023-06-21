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
package org.cp.build.tools.shell.commands.project;

import java.io.File;

import org.cp.build.tools.core.model.Project;
import org.cp.build.tools.core.model.Session;
import org.cp.build.tools.core.support.Utils;
import org.springframework.lang.NonNull;
import org.springframework.shell.Availability;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Shell {@link Command Commands} for {@link Project Projects}.
 *
 * @author John Blum
 * @see org.cp.build.tools.core.model.Project
 * @see org.cp.build.tools.core.model.Session
 * @see org.springframework.shell.command.annotation.Command
 * @since 2.0.0
 */
@RequiredArgsConstructor
@Getter(AccessLevel.PROTECTED)
@Command(command = "project")
@SuppressWarnings("unused")
public class ProjectCommands {

  private final Session session;

  protected Project getCurrentProject() {
    return getSession().getCurrentProject();
  }

  @Command(command = "current")
  @CommandAvailability(provider = "projectCommandsAvailability")
  public String current() {
    return String.format("Current Project [%s]", getSession().getCurrentProject());
  }

  @SuppressWarnings("all")
  @Command(command = "describe")
  @CommandAvailability(provider = "projectCommandsAvailability")
  public String describe() {

    Project project = getCurrentProject();

    return "Project Name: ".concat(project.getName()).concat(Utils.newLine())
      .concat("Description: ").concat(project.getDescription()).concat(Utils.newLine())
      .concat("Version: ").concat(project.getVersion().toString()).concat(Utils.newLine())
      .concat("Source Repository: ").concat(project.getSourceRepository().toString()).concat(Utils.newLine())
      .concat("Artifact: ").concat(project.getArtifact().toString()).concat(Utils.newLine());
  }

  @Command(command = "set", description = "Sets the current Project")
  public String set(@NonNull File location) {

    Project project = Project.from(location);

    getSession().setProject(project);

    return String.format("Project set to [%s]", project);
  }

  public Availability projectCommandsAvailability() {

    return getSession().isProjectSet()
      ? Availability.available()
      : Availability.unavailable("the current Project is not set");
  }
}
