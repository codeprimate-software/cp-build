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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.api.time.TimePeriods;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.git.model.CommitRecord;
import org.cp.build.tools.git.model.CommitRecord.Author;
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
  protected static final String DEFAULT_COMMIT_HISTORY_LIMIT = "5";
  protected static final String INPUT_DATE_PATTERN = "yyyy-MM-dd";

  private static final DateTimeFormatter COMMIT_DATE_FORMATTER = DateTimeFormatter.ofPattern(COMMIT_DATE_TIME_PATTERN);
  private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern(INPUT_DATE_PATTERN);

  private final GitTemplate gitTemplate;

  private final ProjectManager projectManager;

  protected Optional<Project> currentProject() {
    return getProjectManager().getCurrentProject();
  }

  @Command(command = "commit-count")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public int commitCount(@Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "exclude-dates") String excludingDates,
      @Option(longNames = "during", shortNames = 'd') String duringDates) {

    Predicate<CommitRecord> queryPredicate = commitsByTimeQueryPredicate(sinceDate, excludingDates, duringDates);

    return queryCommitHistory(queryPredicate).size();
  }

  @Command(command = "commit-history")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public String commitHistory(@Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates") String excludingDates,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_HISTORY_LIMIT) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = "false") boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate) {

    if (count) {
      return String.valueOf(commitCount(sinceDate, excludingDates, duringDates));
    }
    else {

      StringBuilder output = new StringBuilder();

      Predicate<CommitRecord> queryPredicate = commitsByTimeQueryPredicate(sinceDate, excludingDates, duringDates);

      queryCommitHistory(queryPredicate).stream()
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
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates") String excludingDates,
      @Option(longNames = "since", shortNames = 's') String sinceDate) {

    Predicate<CommitRecord> commitsAfterHoursQueryPredicate = commitRecord -> {

      LocalTime commitTime = commitRecord.getTime();
      LocalTime fivePm = LocalTime.of(17, 0, 0);
      LocalTime nineAm = LocalTime.of(9, 0, 0);

      boolean afterHours =
        Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(commitRecord.getDate().getDayOfWeek())
          || (commitTime.isBefore(nineAm) || commitTime.isAfter(fivePm));

      return afterHours;
    };

    Predicate<CommitRecord> queryPredicate = commitsByTimeQueryPredicate(sinceDate, excludingDates, duringDates)
      .and(commitsAfterHoursQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return count ? String.valueOf(commits.size()) : showCommitHistory(commits).toString();
  }

  @Command(command = "commits-by")
  public String commitsBy(@Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates") String excludingDates,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = "false") boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      String committer) {

    Predicate<CommitRecord> commitsByCommitterQueryPredicate = commitRecord -> {

      Author author = commitRecord.getAuthor();

      return author.getName().contains(committer) || author.getEmailAddress().contains(committer);
    };

    Predicate<CommitRecord> queryPredicate = commitsByTimeQueryPredicate(sinceDate, excludingDates, duringDates)
      .and(commitsByCommitterQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return count ? String.valueOf(commits.size()) : showCommitHistory(commits, showFiles).toString();
  }

  @Command(command = "commits-during-work")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public String commitsOnTheClock(
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates") String excludingDates,
      @Option(longNames = "since", shortNames = 's') String sinceDate) {

    Predicate<CommitRecord> commitsDuringWorkHoursQueryPredicate = commitRecord -> {

      LocalTime commitTime = commitRecord.getTime();
      LocalTime fivePm = LocalTime.of(17, 0, 0);
      LocalTime nineAm = LocalTime.of(9, 0, 0);

      boolean duringWorkHours =
        !Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(commitRecord.getDate().getDayOfWeek());

      duringWorkHours &= !(commitTime.isBefore(nineAm) || commitTime.isAfter(fivePm));

      return duringWorkHours;
    };

    Predicate<CommitRecord> queryPredicate = commitsByTimeQueryPredicate(sinceDate, excludingDates, duringDates)
      .and(commitsDuringWorkHoursQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return count ? String.valueOf(commits.size()) : showCommitHistory(commits).toString();
  }

  private @NonNull CommitHistory queryCommitHistory() {
    return queryCommitHistory(commitRecord -> true);
  }

  private @NonNull CommitHistory queryCommitHistory(@NonNull Predicate<CommitRecord> queryPredicate) {

    return currentProject()
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

  private static @NonNull Predicate<CommitRecord> commitsByTimeQueryPredicate(
      @Nullable String sinceDate, @Nullable String excludingDates, @Nullable String duringDates) {

    return commitsSinceDateQueryPredicate(sinceDate)
      .and(commitsExcludingDatesQueryPredicate(excludingDates))
      .and(commitsDuringDatesQueryPredicate(duringDates));
  }

  private static @NonNull Predicate<CommitRecord> commitsDuringDatesQueryPredicate(@Nullable String duringDates) {

    return StringUtils.hasText(duringDates)
      ? commitRecord -> TimePeriods.parse(duringDates).asPredicate().test(commitRecord.getDate())
      : commitRecord -> true;
  }

  private static @NonNull Predicate<CommitRecord> commitsExcludingDatesQueryPredicate(@Nullable String excludingDates) {

    return StringUtils.hasText(excludingDates)
      ? commitRecord -> !TimePeriods.parse(excludingDates).asPredicate().test(commitRecord.getDate())
      : commitRecord -> true;
  }

  private static @NonNull Predicate<CommitRecord> commitsSinceDateQueryPredicate(@Nullable String sinceDate) {

    return commitRecord -> {

      LocalDate since = StringUtils.hasText(sinceDate)
        ? LocalDate.parse(sinceDate, INPUT_DATE_FORMATTER)
        : Utils.atEpoch().toLocalDate();

      LocalDate commitDate = commitRecord.getDate();

      return commitDate.isAfter(since);
    };
  }

  private @NonNull CommitHistory resolveCommitHistory(@NonNull Project project) {
    return Utils.get(Utils.get(project.getCommitHistory(), () -> loadCommitHistory(project)), CommitHistory::empty);
  }

  private @NonNull CommitHistory loadCommitHistory(@NonNull Project project) {
    return project.withCommitHistory(getGitTemplate().getCommitHistory()).getCommitHistory();
  }

  private @NonNull StringBuilder showCommitHistory(@NonNull CommitHistory commits) {
    return showCommitHistory(commits, DEFAULT_SHOW_FILES);
  }

  private @NonNull StringBuilder showCommitHistory(@NonNull CommitHistory commits, boolean showFiles) {

    StringBuilder output = new StringBuilder();

    commits.stream().sorted().toList()
      .forEach(commitRecord -> showCommitRecord(output, commitRecord, showFiles));

    return output;
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

    return currentProject().isPresent()
      ? Availability::available
      : () -> Availability.unavailable("the current project is not set;"
        + " please call 'project load <location>' or 'project switch <name>'");
  }
}
