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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.maven.model.MavenProject;
import org.cp.build.tools.shell.commands.AbstractCommandsSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.shell.Availability;
import org.springframework.shell.AvailabilityProvider;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;
import org.springframework.shell.command.annotation.OptionValues;
import org.springframework.shell.completion.CompletionProvider;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Shell {@link Command Commands} for {@link Project Projects}.
 *
 * @author John Blum
 * @see org.cp.build.tools.api.model.Project
 * @see org.cp.build.tools.api.model.Session
 * @see org.cp.build.tools.shell.commands.AbstractCommandsSupport
 * @see org.springframework.shell.command.annotation.Command
 * @since 2.0.0
 */
@Command(command = "project")
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ProjectCommands extends AbstractCommandsSupport {

  private final ProjectManager projectManager;

  @Command(command = "current", description = "Shows the current project")
  public String current() {

    return currentProject()
      .map(project -> String.format("Current project is [%s] located in [%s]", project, project.getDirectory()))
      .orElse("Project not set");
  }

  @SuppressWarnings("all")
  @Command(command = "describe", description = "Describes the current project")
  @CommandAvailability(provider = "projectCommandsAvailabilityProvider")
  public String describe() {

    return currentProject()
      .map(project -> "Name: ".concat(project.getName()).concat(Utils.newLine())
        .concat("Description: ").concat(indent(project.getDescription())).concat(Utils.newLine())
        .concat("Version: ").concat(project.getVersion().toString()).concat(Utils.newLine())
        .concat("Artifact: ").concat(project.getArtifact().toString()).concat(Utils.newLine())
        .concat(Utils.newLine())
        .concat("Licenses: ").concat(project.getLicenses().toString()).concat(Utils.newLine())
        .concat("Organization: ").concat(Utils.nullSafeToString(project.getOrganization()).concat(Utils.newLine())
        .concat("Developers: ").concat(project.getDevelopers().toString()).concat(Utils.newLine()))
        .concat(Utils.newLine())
        .concat("Source Repository: ").concat(project.getSourceRepository().toString()).concat(Utils.newLine())
        .concat("Issue Tracker: ").concat(project.getIssueTracker().toString()).concat(Utils.newLine())
      )
      .orElseThrow(() -> new IllegalStateException("Project was not set"));
  }

  @Command(command = "list", description = "List all available projects")
  public String list() {

    int length = projects().stream()
      .map(Project::getName)
      .map(String::length)
      .reduce(Math::max)
      .orElse(25);

    StringBuilder output = new StringBuilder();

    for (Project project : projects()) {

      String projectName = project.getName();

      String formattedProjectName = Utils.nullSafeFormatString(projectName, length);

      String resolvedProjectName = isCurrentProject(project) ? "[*] ".concat(formattedProjectName)
        : "[ ] ".concat(formattedProjectName);

      output.append(resolvedProjectName).append(" | ").append(project.getDirectory()).append(Utils.newLine());
    }

    return output.toString();
  }

  @Command(command = "load", description = "Loads project from the given location")
  public String load(@NonNull @OptionValues(provider = "projectLoadCompletionProvider") File location) {

    ProjectManager projectManager = getProjectManager();

    Project project = projectManager.setCurrentProject(projectManager.resolveByLocation(location));

    return String.format("Project set to [%s]", project);
  }

  @Command(command = "use", description = "Sets current project to the given name")
  public String use(@NonNull String projectName) {

    return getProjectManager().findByName(projectName)
      .map(getProjectManager()::setCurrentProject)
      .map(project -> String.format("Project set to [%s]", project))
      .orElseGet(() -> String.format("Project with name [%s] not found", projectName));
  }

  @NonNull @Bean
  AvailabilityProvider projectCommandsAvailabilityProvider() {

    return isProjectSet() ? Availability::available
      : () -> Availability.unavailable("the current project is not set;"
        + " please call 'project load <location>' or 'project use <name>'");
  }

  @NonNull @Bean
  CompletionProvider projectLoadCompletionProvider() {

    return completionContext -> {

      String currentWord = completionContext.currentWord();

      File currentFile = StringUtils.hasText(currentWord) ? new File(currentWord) : Utils.WORKING_DIRECTORY;

      FileFilter fileFilter = file -> Utils.nullSafeIsDirectory(file) || MavenProject.isMavenPom(file);

      return Arrays.stream(Utils.nullSafeFileArray(currentFile.listFiles(fileFilter)))
        .map(File::getName)
        .map(CompletionProposal::new)
        .collect(Collectors.toList());
    };
  }
}
