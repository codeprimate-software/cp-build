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
import java.util.List;
import java.util.Optional;

import org.cp.build.tools.core.model.Project;
import org.cp.build.tools.core.service.ProjectManager;
import org.cp.build.tools.core.support.Utils;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.shell.Availability;
import org.springframework.shell.AvailabilityProvider;
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
@Command(command = "project")
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ProjectCommands {

  private final ProjectManager projectManager;

  protected boolean isCurrentProject(@Nullable Project project) {
    return getCurrentProject().filter(currentProject -> currentProject.equals(project)).isPresent();
  }

  protected Optional<Project> getCurrentProject() {
    return getProjectManager().getCurrentProject();
  }

  protected @NonNull Project setCurrentProject(@NonNull Project project) {
    return getProjectManager().setCurrentProject(project);
  }

  protected List<Project> projects() {
    return getProjectManager().list();
  }

  @Command(command = "current")
  public String current() {

    return getCurrentProject()
      .map(project -> String.format("Current Project is [%s]", project))
      .orElseGet(() -> "Project not set");
  }

  @SuppressWarnings("all")
  @Command(command = "describe")
  @CommandAvailability(provider = "projectCommandsAvailability")
  public String describe() {

    return getCurrentProject()
      .map(project -> "Project Name: ".concat(project.getName()).concat(Utils.newLine())
        .concat("Description: ").concat(project.getDescription()).concat(Utils.newLine())
        .concat("Version: ").concat(project.getVersion().toString()).concat(Utils.newLine())
        .concat("Source Repository: ").concat(project.getSourceRepository().toString()).concat(Utils.newLine())
        .concat("Artifact: ").concat(project.getArtifact().toString()).concat(Utils.newLine()))
      .orElseThrow(() -> new IllegalStateException("Project was not set"));
  }

  @Command(command = "list")
  public String list() {

    int length = projects().stream()
      .map(Project::getName)
      .map(String::length)
      .reduce(Math::max)
      .orElse(25);

    StringBuilder output = new StringBuilder();

    for (Project project : projects()) {

      String projectName = project.getName();

      String paddedProjectName = Utils.nullSafeFormatStringToLength(projectName, length);

      String resolvedProjectName = isCurrentProject(project) ? "[*] ".concat(paddedProjectName)
        : "[ ] ".concat(paddedProjectName);

      output.append(resolvedProjectName).append(" - ").append(project.getDirectory()).append(Utils.newLine());
    }

    return output.toString();
  }

  @Command(command = "set", description = "Sets the current Project to the given location")
  public String set(@NonNull File location) {

    Project project = setCurrentProject(getProjectManager().resolveByLocation(location));

    return String.format("Project set to [%s]", project);
  }

  @NonNull @Bean
  AvailabilityProvider projectCommandsAvailability() {

    return getCurrentProject().isPresent()
      ? Availability::available
      : () -> Availability.unavailable("the current Project is not set; call 'project set <location>'");
  }
}
