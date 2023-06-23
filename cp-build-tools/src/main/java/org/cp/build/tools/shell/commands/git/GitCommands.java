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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.git.model.CommitRecord;
import org.cp.build.tools.git.support.GitTemplate;
import org.cp.build.tools.shell.commands.AbstractCommandsSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.shell.Availability;
import org.springframework.shell.AvailabilityProvider;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;
import org.springframework.shell.command.annotation.Option;
import org.springframework.util.StringUtils;

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
 * @see org.cp.build.tools.shell.commands.AbstractCommandsSupport
 * @see org.springframework.shell.command.annotation.Command
 * @since 2.0.0
 */
@Command(command = "git")
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class GitCommands extends AbstractCommandsSupport {

  protected static final boolean DEFAULT_SHOW_FILES = false;

  protected static final String COMMIT_AUTHOR_TO_STRING = "%1$s <%2$s>";
  protected static final String COMMIT_DATE_TIME_PATTERN = "EEE, yyyy-MMM-dd HH:mm:ss";
  protected static final String USER_INPUT_DATE_PATTERN = "yyyy-MM-dd";

  private static final DateTimeFormatter COMMIT_DATE_FORMATTER = DateTimeFormatter.ofPattern(COMMIT_DATE_TIME_PATTERN);
  private static final DateTimeFormatter USER_INPUT_DATE_FORMATTER =
    DateTimeFormatter.ofPattern(USER_INPUT_DATE_PATTERN);

  private static final String DEFAULT_COMMIT_HISTORY_LIMIT = "5";

  private final GitTemplate gitTemplate;

  private final ProjectManager projectManager;

  protected Optional<Project> getCurrentProject() {
    return getProjectManager().getCurrentProject();
  }

  @Command(command = "commit-count")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public int commitCount(@Option(longNames = "since", shortNames = 's') String dateString) {
    return queryCommitHistory(commitsSincePredicate(dateString)).size();
  }

  @Command(command = "commit-history")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public String commitHistory(
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_HISTORY_LIMIT) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = "false") boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String since) {

    if (count) {
      return String.valueOf(commitCount(since));
    }
    else {

      StringBuilder output = new StringBuilder();

      queryCommitHistory().stream()
        .limit(limit)
        .sorted()
        .toList()
        .forEach(commitRecord -> showCommitRecord(output, commitRecord, showFiles));

      return output.toString();
    }
  }

  @Command(command = "commits-after-hours")
  @CommandAvailability(provider = "gitCommandsAvailability")
  @SuppressWarnings("all")
  public String commitsAfterHours(
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "since", shortNames = 's') String since) {

    Predicate<CommitRecord> commitsAfterHoursPredicate = commitRecord -> {

      LocalDateTime commitDateTime = commitRecord.getDateTime();

      LocalTime commitTime = commitDateTime.toLocalTime();
      LocalTime fivePm = LocalTime.of(17, 0, 0);
      LocalTime nineAm = LocalTime.of(9, 0, 0);

      boolean afterHours = Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(commitDateTime.getDayOfWeek())
        || (commitTime.isBefore(nineAm) || commitTime.isAfter(fivePm));

      return afterHours;
    };

    Predicate<CommitRecord> queryPredicate = commitsSincePredicate(since).and(commitsAfterHoursPredicate);

    List<CommitRecord> commits = queryCommitHistory(queryPredicate).stream().sorted().toList();

    return count ? String.valueOf(commits.size()) : "Not Implemented";
  }

  @Command(command = "commits-during-work")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public String commitsOnTheClock(
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "since", shortNames = 's') String since) {

    Predicate<CommitRecord> commitsDuringWorkHoursPredicate = commitRecord -> {

      LocalDateTime commitDateTime = commitRecord.getDateTime();

      LocalTime commitTime = commitDateTime.toLocalTime();
      LocalTime fivePm = LocalTime.of(17, 0, 0);
      LocalTime nineAm = LocalTime.of(9, 0, 0);

      boolean duringWorkHours =
        !Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(commitDateTime.getDayOfWeek());

      duringWorkHours &= !(commitTime.isBefore(nineAm) || commitTime.isAfter(fivePm));

      return duringWorkHours;
    };

    Predicate<CommitRecord> queryPredicate = commitsSincePredicate(since).and(commitsDuringWorkHoursPredicate);

    List<CommitRecord> commits = queryCommitHistory(queryPredicate).stream().sorted().toList();

    return count ? String.valueOf(commits.size()) : "Not Implemented";
  }

  private @NonNull CommitHistory queryCommitHistory() {
    return queryCommitHistory(commitRecord -> true);
  }

  private @NonNull CommitHistory queryCommitHistory(@NonNull Predicate<CommitRecord> queryPredicate) {

    return getCurrentProject()
      .map(project -> queryCommitHistory(project, Utils.nullSafeNonMatchingPredicate(queryPredicate)))
      .orElseGet(CommitHistory::empty);
  }

  private @NonNull CommitHistory queryCommitHistory(@NonNull Project project,
      @NonNull Predicate<CommitRecord> queryPredicate) {

    return queryCommitHistory(resolveCommitHistory(project), queryPredicate);
  }

  private @NonNull CommitHistory queryCommitHistory(@NonNull CommitHistory commitHistory,
      @NonNull Predicate<CommitRecord> queryPredicate) {

    return CommitHistory.of(commitHistory.findBy(queryPredicate));
  }

  private static @NonNull Predicate<CommitRecord> commitsSincePredicate(@Nullable String dateString) {

    return commitRecord -> {

      LocalDate since = StringUtils.hasText(dateString)
        ? LocalDate.parse(dateString, USER_INPUT_DATE_FORMATTER)
        : Utils.atEpoch().toLocalDate();

      LocalDateTime commitDateTime = commitRecord.getDateTime();

      LocalDate commitDate = commitDateTime.toLocalDate();

      return commitDate.isAfter(since);
    };
  }

  private @NonNull CommitHistory resolveCommitHistory(@NonNull Project project) {
    return Utils.get(Utils.get(project.getCommitHistory(), () -> loadCommitHistory(project)), CommitHistory::empty);
  }

  private @NonNull CommitHistory loadCommitHistory(@NonNull Project project) {
    return project.withCommitHistory(getGitTemplate().getCommitHistory()).getCommitHistory();
  }

  private @NonNull StringBuilder showCommitRecord(@NonNull StringBuilder output, @NonNull CommitRecord commitRecord) {
    return showCommitRecord(output, commitRecord, DEFAULT_SHOW_FILES);
  }

  private StringBuilder showCommitRecord(@NonNull StringBuilder output,
    @NonNull CommitRecord commitRecord, boolean showFiles) {

    output.append("Author: ").append(toCommitAuthorString(commitRecord.getAuthor())).append(Utils.newLine());
    output.append("Commit: ").append(commitRecord.getHash()).append(Utils.newLine());
    output.append("Date/Time: ").append(toCommitDateString(commitRecord)).append(Utils.newLine());
    output.append("Message: ").append(Utils.newLine()).append(Utils.newLine())
      .append(toCommitMessageString(commitRecord)).append(Utils.newLine());

    if (showFiles) {
      output.append(Utils.newLine());
      for (File sourceFile : commitRecord) {
        output.append(toCommitSourceFileString(sourceFile)).append(Utils.newLine());
      }
    }

    output.append(Utils.newLine());

    return output;
  }

  private String toCommitAuthorString(CommitRecord.Author author) {
    return String.format(COMMIT_AUTHOR_TO_STRING, author.getName(), author.getEmailAddress());
  }

  private String toCommitDateString(CommitRecord commitRecord) {
    return commitRecord.getDateTime().format(COMMIT_DATE_FORMATTER);
  }

  private String toCommitMessageString(CommitRecord commitRecord) {
    return indent(commitRecord.getMessage());
  }

  private String toCommitSourceFileString(File source) {
    return source.getAbsolutePath();
  }

  @NonNull @Bean
  AvailabilityProvider gitCommandsAvailability() {

    return getCurrentProject().isPresent()
      ? Availability::available
      : () -> Availability.unavailable("the current project is not set;"
        + " please call 'project load <location>' or 'project switch <name>'");
  }
}
