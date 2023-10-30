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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.service.ProjectManager.RecentProject;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.git.model.CommitRecord;
import org.cp.build.tools.maven.model.MavenProject;
import org.cp.build.tools.shell.commands.AbstractCommandsSupport;
import org.cp.build.tools.shell.jline.Colors;
import org.jline.utils.AttributedStringBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.shell.Availability;
import org.springframework.shell.AvailabilityProvider;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;
import org.springframework.shell.command.annotation.Option;
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

  protected static final int DEFAULT_WORK_HOURS_PER_DAY = 8;

  protected static final BigDecimal DEFAULT_HOURLY_RATE = BigDecimal.valueOf(100.0d);

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
      .map(project -> {

        AttributedStringBuilder output = new AttributedStringBuilder();

        Colors labelColor = Colors.LIME;
        Colors textColor = Colors.WHITE;

        output.style(toBoldText(labelColor)).append("Name: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getName()))
          .style(toBoldItalicText(labelColor)).append("Description: ")
          .style(toPlainText(textColor)).append("~").append(indent(project.getDescription())).append(Utils.newLine())
          .style(toBoldItalicText(labelColor)).append("Version: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getVersion().toString()))
          .style(toBoldItalicText(labelColor)).append("Artifact: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getArtifact().toString()))
          .append(Utils.newLine())
          .style(toBoldItalicText(labelColor)).append("Licenses: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getLicenses().toString()))
          .style(toBoldItalicText(labelColor)).append("Organization: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(Utils.nullSafeToString(project.getOrganization())))
          .style(toBoldItalicText(labelColor)).append("Developers: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getDevelopers().toString()))
          .append(Utils.newLine())
          .style(toBoldItalicText(labelColor)).append("Source Repository: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getSourceRepository().toString()))
          .style(toBoldItalicText(labelColor)).append("Issue Tracker: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getIssueTracker().toString()));

          return output.toAnsi();

      })
      .orElseThrow(() -> new IllegalStateException("Project was not set"));
  }

  @Command(command = "development", description="Details the project's development effort and activity")
  @CommandAvailability(provider = "projectCommandsAvailabilityProvider")
  public String development(@Option(longNames = "hourly-rate", defaultValue = "100.0") BigDecimal hourlyRate) {

    return currentProject()
      .map(project -> {

        int daysOfDevelopmentCount = countDaysOfDevelopment(project);
        int hoursOfDevelopmentCount = daysOfDevelopmentCount * DEFAULT_WORK_HOURS_PER_DAY;

        BigDecimal resolvedHourlyRate = hourlyRate != null ? hourlyRate : DEFAULT_HOURLY_RATE;
        BigDecimal costOfDevelopment = resolvedHourlyRate.multiply(BigDecimal.valueOf(hoursOfDevelopmentCount));

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

        String costOfDevelopmentText = currencyFormat.format(costOfDevelopment.doubleValue());

        AttributedStringBuilder output = new AttributedStringBuilder();

        Colors labelColor = Colors.LIME;
        Colors textColor = Colors.WHITE;

        output.style(toBoldText(labelColor)).append("Name: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(project.getName()))
          .style(toBoldItalicText(labelColor)).append("Days of Development: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(String.valueOf(daysOfDevelopmentCount)))
          .style(toBoldItalicText(labelColor)).append("Hours of Development: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(String.valueOf(hoursOfDevelopmentCount)))
          .style(toBoldItalicText(labelColor)).append("Cost of Development: ")
          .style(toPlainText(textColor)).append(Utils.newLineAfter(costOfDevelopmentText));

        return output.toAnsi();

      })
      .orElseThrow(() -> new IllegalStateException("Project was not set"));
  }

  private int countDaysOfDevelopment(Project project) {

    int currentDay = 0;
    int dayCount = 0;

    for (CommitRecord commit : project.getCommitHistory()) {
      if (commit.getDateTime().getDayOfYear() != currentDay) {
        currentDay = commit.getDateTime().getDayOfYear();
        dayCount++;
      }
    }

    return dayCount;
  }

  @Command(command = "list", description = "List all loaded projects")
  public String list() {

    List<Project> projects = projects();

    int length = projects.stream()
      .map(Project::getName)
      .map(String::length)
      .reduce(Math::max)
      .orElse(25);

    StringBuilder output = new StringBuilder();

    for (Project project : projects) {

      String projectName = project.getName();

      String formattedProjectName = Utils.nullSafeFormatString(projectName, length);

      String resolvedProjectName = isCurrentProject(project) ? "[*] ".concat(formattedProjectName)
        : "[ ] ".concat(formattedProjectName);

      output.append(resolvedProjectName).append(" | ")
        .append(Utils.newLineAfter(project.getDirectory().getAbsolutePath()));
    }

    return output.toString();
  }

  @Command(command = "load", description = "Loads project from the given location")
  public String load(@NonNull @OptionValues(provider = "projectLoadCompletionProvider") File location) {

    ProjectManager projectManager = getProjectManager();

    Project project = projectManager.setCurrentProject(projectManager.resolveByLocation(location));

    return String.format("Project set to [%s]", project);
  }

  @Command(command = "recent", description = "List all recent projects")
  public String recent() {

    List<ProjectManager.RecentProject> recentProjects = recentProjects();

    int length = recentProjects.stream()
      .map(ProjectManager.RecentProject::getName)
      .map(String::length)
      .reduce(Math::max)
      .orElse(25);

    StringBuilder output = new StringBuilder();

    for (ProjectManager.RecentProject recentProject : recentProjects) {

      String recentProjectName = recentProject.getName();
      String formattedRecentProjectName = Utils.nullSafeFormatString(recentProjectName, length);

      output.append(formattedRecentProjectName).append(" | ")
        .append(Utils.newLineAfter(recentProject.getLocation().getAbsolutePath()));
    }

    return output.toString();
  }

  @Command(command = "use", description = "Sets current project to the given name")
  public String use(@NonNull String projectName) {

    ProjectManager projectManager = getProjectManager();

    return projectManager.findProjectByName(projectName)
      .map(projectManager::setCurrentProject)
      .map(project -> String.format("Project set to [%s]", project))
      .orElseGet(() -> projectManager.findRecentProjectByName(projectName)
        .filter(RecentProject::exists)
        .map(RecentProject::getLocation)
        .map(projectManager::resolveByLocation)
        .map(projectManager::setCurrentProject)
        .map(project -> String.format("Project set to [%s]", project))
        .orElseGet(() -> String.format("Project with name [%s] not found", projectName)));
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
