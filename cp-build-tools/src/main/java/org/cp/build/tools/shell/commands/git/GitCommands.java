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
package org.cp.build.tools.shell.commands.git;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.git.support.GitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.shell.Availability;
import org.springframework.shell.AvailabilityProvider;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;
import org.springframework.shell.command.annotation.Option;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Shell {@link Command Commands} for {@literal git}.
 *
 * @author John Blum
 * @see org.cp.build.tools.api.model.Project
 * @see org.cp.build.tools.api.service.ProjectManager
 * @see org.cp.build.tools.git.model.CommitHistory
 * @see org.cp.build.tools.git.model.CommitRecord
 * @see org.cp.build.tools.git.support.GitTemplate
 * @see org.springframework.shell.command.annotation.Command
 * @since 2.0.0
 */
@Command(command = "git")
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class GitCommands {

  private static final DateTimeFormatter COMMIT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyy-MMM-dd HH:mm:ss");

  private static final String DEFAULT_COMMIT_HISTORY_LIMIT = "5";

  private final GitTemplate gitTemplate;

  private final ProjectManager projectManager;

  protected Optional<Project> getCurrentProject() {
    return getProjectManager().getCurrentProject();
  }

  @Command(command = "commit-count")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public int commitCount() {

    return getCurrentProject()
      .map(this::resolveCommitHistory)
      .map(CommitHistory::size)
      .orElse(0);
  }

  @Command(command = "commit-history")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public String commitHistory(
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_HISTORY_LIMIT) int limit,
      @Option(longNames = "files", shortNames = 'f', defaultValue = "false") boolean showFiles) {

    StringBuilder output = new StringBuilder();

    getCurrentProject()
      .map(this::resolveCommitHistory)
      .orElseGet(CommitHistory::empty)
      .stream()
      .limit(limit)
      .sorted()
      .toList()
      .forEach(commitRecord -> {

        output.append(String.format("Author: %s <%s>",
          commitRecord.getAuthor().getName(), commitRecord.getAuthor().getEmail())).append(Utils.newLine());
        output.append("Commit: ").append(commitRecord.getHash()).append(Utils.newLine());
        output.append("Date/Time: ").append(commitRecord.getDate().format(COMMIT_DATE_FORMATTER)).append(Utils.newLine());
        output.append("Message: ").append(Utils.newLine()).append(Utils.newLine())
          .append(commitRecord.getMessage()).append(Utils.newLine());

        if (showFiles) {
          output.append(Utils.newLine());
          for (File sourceFile : commitRecord) {
            output.append(sourceFile.getAbsolutePath()).append(Utils.newLine());
          }
        }

        output.append(Utils.newLine());
      });

    return output.toString();
  }

  private @Nullable CommitHistory resolveCommitHistory(@NonNull Project project) {
    return Utils.get(project.getCommitHistory(), () -> loadCommitHistory(project));
  }

  private @Nullable CommitHistory loadCommitHistory(@NonNull Project project) {
    return project.withCommitHistory(getGitTemplate().getCommitHistory()).getCommitHistory();
  }

  @NonNull @Bean
  AvailabilityProvider gitCommandsAvailability() {

    return getCurrentProject().isPresent()
      ? Availability::available
      : () -> Availability.unavailable("the current Project is not set; please call 'project set <location>'");
  }
}
