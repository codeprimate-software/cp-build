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
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.model.SourceFile;
import org.cp.build.tools.api.model.SourceFileSet;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.api.time.TimePeriods;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.git.model.CommitHistory.Group;
import org.cp.build.tools.git.model.CommitRecord;
import org.cp.build.tools.git.model.CommitRecord.Author;
import org.cp.build.tools.git.model.GitStatus;
import org.cp.build.tools.git.model.support.CommitRecordComparator;
import org.cp.build.tools.git.support.GitTemplate;
import org.cp.build.tools.shell.commands.AbstractCommandsSupport;
import org.cp.build.tools.shell.jline.Colors;
import org.jline.utils.AttributedStringBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.shell.Availability;
import org.springframework.shell.AvailabilityProvider;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;
import org.springframework.shell.command.annotation.Option;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;
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
 * @see org.cp.build.tools.git.model.GitStatus
 * @see org.cp.build.tools.git.support.GitTemplate
 * @see org.cp.build.tools.shell.commands.AbstractCommandsSupport
 * @see org.springframework.shell.command.annotation.Command
 * @since 2.0.0
 */
@Command(command = "git", group = "git commands")
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class GitCommands extends AbstractCommandsSupport {

  protected static final boolean DEFAULT_SHOW_FILES = false;

  protected static final String COMMIT_AUTHOR_TO_STRING = "%1$s <%2$s>";
  protected static final String COMMIT_DATE_PATTERN = "yyyy-MMM-dd";
  protected static final String COMMIT_DATE_TIME_PATTERN = "EEE, yyyy-MMM-dd HH:mm:ss";
  protected static final String COMMIT_DATE_YEAR_MONTH_PATTERN = "yyyy-MMMM";
  protected static final String DEFAULT_COMMIT_COUNT_GROUP_BY_LIMIT_OPTION = "12";
  protected static final String DEFAULT_COMMIT_LOG_LIMIT_OPTION = "5";
  protected static final String DEFAULT_SHOW_FILES_OPTION = "false";
  protected static final String INPUT_DATE_PATTERN = "yyyy-MM-dd";
  protected static final String PROJECT_NOT_FOUND = "Project not set";

  protected static final Predicate<CommitRecord> ALL_COMMITS_PREDICATE = commitRecord -> true;

  private static final DateTimeFormatter COMMIT_DATE_FORMATTER = DateTimeFormatter.ofPattern(COMMIT_DATE_PATTERN);
  private static final DateTimeFormatter COMMIT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(COMMIT_DATE_TIME_PATTERN);
  private static final DateTimeFormatter COMMIT_DATE_YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern(COMMIT_DATE_YEAR_MONTH_PATTERN);
  private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern(INPUT_DATE_PATTERN);

  private final GitTemplate gitTemplate;

  private final ProjectManager projectManager;

  @Command(command = "commit-count", description = "Counts all commits")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public int commitCount(@Option(description = "Commit count by author") String author,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, excludingDates, duringDates)
        .and(commitsByAuthorQueryPredicate(author));

    return queryCommitHistory(queryPredicate).size();
  }

  @Command(command = "commit-count-group", description = "Counts all commits grouped by a given time period")
  public @NotNull String commitCountGroupedBy(@Option(description = "Commit count by author") String author,
      @Option(longNames = "by-day", defaultValue = "false") boolean groupedByDay,
      @Option(longNames = "by-month", defaultValue = "false") boolean groupedByMonth,
      @Option(longNames = "by-year", defaultValue = "false") boolean groupedByYear,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_COUNT_GROUP_BY_LIMIT_OPTION) int limit,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, null, null)
        .and(commitsByAuthorQueryPredicate(author));

    CommitHistory commitRecords = queryCommitHistory(queryPredicate);

    Set<CommitHistory.Group> groupedCommitRecords =
      groupedByYear ? commitRecords.groupByYear()
      : groupedByMonth ? commitRecords.groupByMonth()
      : commitRecords.groupByDay();

    List<CommitHistory.Group> limitedSortedCommitRecords = groupedCommitRecords.stream()
      .sorted(Comparator.comparing(Group::size).reversed())
      .limit(limit)
      .toList();

    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append("    TIME PERIOD    |    COUNT    ").append(Utils.newLine());
    stringBuilder.append("--------------------------------").append(Utils.newLine());

    limitedSortedCommitRecords.forEach(group ->
      stringBuilder.append(Utils.padRight(toCommitDateByCountString(group.getGroupedBy()), 19))
        .append("| ").append(group.size()).append(Utils.newLine()));

    return stringBuilder.toString();
  }

  @Command(command = "commit-log", description = "Logs all commits")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public @NonNull String commitLog(@Option(description = "Commit with hash; options not applicable") String hash,
      @Option(longNames = "author") String author,
      @Option(longNames = "after-hash", shortNames = 'a', description = "[--until, --exclude-dates]") String afterHash,
      @Option(longNames = "before-hash", shortNames = 'b', description = "[--since, --exclude-dates]") String beforeHash,
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_LOG_LIMIT_OPTION) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    if (Utils.isSet(hash)) {

      return currentProject()
        .map(this::resolveCommitHistory)
        .flatMap(commitHistory -> commitHistory.findByHash(hash))
        .map(commitRecord -> showCommitRecord(commitRecord, showFiles))
        .map(Object::toString)
        .orElseGet(() -> isProjectSet() ? String.format("Commit for hash ID [%s] not found", hash)
          : PROJECT_NOT_FOUND);
    }
    else if (Utils.isSet(afterHash, beforeHash)) {

      // Limit commits by hash
      Function<CommitHistory, CommitHistory> hashFunction =
        Utils.isSet(afterHash) ? commitHistory -> commitHistory.findAllCommitsAfterHash(afterHash)
          : Utils.isSet(beforeHash) ? commitHistory -> commitHistory.findAllCommitsBeforeHash(beforeHash)
          : Function.identity();

      // Query commits
      CommitHistory commits = currentProject()
          .map(this::resolveCommitHistory)
          .map(hashFunction)
          .orElseGet(CommitHistory::empty);

      // Filter commits by time (date)
      Predicate<CommitRecord> queryPredicate = commitsByAuthorQueryPredicate(author)
        .and(commitsExcludingDatesQueryPredicate(excludingDates));

      queryPredicate = Utils.isSet(afterHash) && Utils.isNotSet(beforeHash)
        ? queryPredicate.and(commitsUntilDateQueryPredicate(untilDate))
        : queryPredicate;

      queryPredicate = Utils.isSet(beforeHash) && Utils.isNotSet(afterHash)
        ? queryPredicate.and(commitsSinceDateQueryPredicate(sinceDate))
        : queryPredicate;

      commits = commits.findBy(queryPredicate);

      return isProjectSet()
        ? showCommitHistoryFunction(count, limit, showFiles).apply(commits)
        : PROJECT_NOT_FOUND;
    }
    else if (count) {
      return String.valueOf(commitCount(author, duringDates, excludingDates, sinceDate, untilDate));
    }
    else {

      Predicate<CommitRecord> queryPredicate =
        commitsByTimeQueryPredicate(sinceDate, untilDate, excludingDates, duringDates)
          .and(commitsByAuthorQueryPredicate(author));

      CommitHistory commits = queryCommitHistory(queryPredicate);

      return showCommitHistory(commits, limit, showFiles).toString();
    }
  }

  @Command(command = "commits-after-hours", description = "Finds all commits made after hours")
  @CommandAvailability(provider = "gitCommandsAvailability")
  @SuppressWarnings("all")
  public @NonNull String commitsAfterHours(@Option(description = "Commits by author") String author,
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_LOG_LIMIT_OPTION) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> commitsAfterHoursQueryPredicate = commitRecord -> {

      LocalTime commitTime = commitRecord.getTime();
      LocalTime fivePm = LocalTime.of(17, 0, 0);
      LocalTime nineAm = LocalTime.of(9, 0, 0);

      boolean afterHours =
        Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(commitRecord.getDate().getDayOfWeek())
          || (commitTime.isBefore(nineAm) || commitTime.isAfter(fivePm));

      return afterHours;
    };

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, excludingDates, duringDates)
        .and(commitsByAuthorQueryPredicate(author))
        .and(commitsAfterHoursQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return showCommitHistoryFunction(count, limit, showFiles).apply(commits);
  }

  @Command(command = "commits-by", description = "Finds all commits by author (committer)")
  public @NonNull String commitsBy(@Option(required = true) String committer,
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_LOG_LIMIT_OPTION) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> commitsByCommitterQueryPredicate = commitRecord -> {

      Author author = commitRecord.getAuthor();

      String resolvedCommitter = Utils.nullSafeTrimmedString(committer);

      return author.getName().toLowerCase().contains(resolvedCommitter)
        || author.getEmailAddress().toLowerCase().contains(resolvedCommitter);
    };

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, excludingDates, duringDates)
        .and(commitsByCommitterQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return showCommitHistoryFunction(count, limit, showFiles).apply(commits);
  }

  @Command(command = "commits-during-work", description = "Finds all commits during work hours (on-the-clock)")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public @NonNull String commitsOnTheClock(@Option(description = "Commits by author") String author,
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_LOG_LIMIT_OPTION) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> commitsDuringWorkHoursQueryPredicate = commitRecord -> {

      LocalTime commitTime = commitRecord.getTime();
      LocalTime fivePm = LocalTime.of(17, 0, 0);
      LocalTime nineAm = LocalTime.of(9, 0, 0);

      boolean duringWorkHours =
        !Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(commitRecord.getDate().getDayOfWeek());

      duringWorkHours &= !(commitTime.isBefore(nineAm) || commitTime.isAfter(fivePm));

      return duringWorkHours;
    };

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, excludingDates, duringDates)
        .and(commitsByAuthorQueryPredicate(author))
        .and(commitsDuringWorkHoursQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return showCommitHistoryFunction(count, limit, showFiles).apply(commits);
  }

  @Command(command = "commits-to", description = "Finds all commit to a source file or path")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public @NonNull String commitsTo(@Option(required = true) String sourceFilePath,
      @Option(longNames = "author", description = "By author") String author,
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_LOG_LIMIT_OPTION) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, excludingDates, duringDates)
        .and(commitsByAuthorQueryPredicate(author))
        .and(commitsToSourceFilePathQueryPredicate(sourceFilePath));

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return showCommitHistoryFunction(count, limit, showFiles).apply(commits);
  }

  @Command(command = "commits-with", description = "Finds all commits with message")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public @NonNull String commitsWith(@Option(required = true) String message,
      @Option(longNames = "author", description = "By author") String author,
      @Option(longNames = "count", shortNames = 'c', defaultValue = "false") boolean count,
      @Option(longNames = "during", shortNames = 'd') String duringDates,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates,
      @Option(longNames = "limit", shortNames = 'l', defaultValue = DEFAULT_COMMIT_LOG_LIMIT_OPTION) int limit,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> commitsWithMessageQueryPredicate = commitRecord ->
      commitRecord.getMessage().toLowerCase().contains(String.valueOf(message).toLowerCase());

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, excludingDates, duringDates)
        .and(commitsByAuthorQueryPredicate(author))
        .and(commitsWithMessageQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    return showCommitHistoryFunction(count, limit, showFiles).apply(commits);
  }

  @Command(command = "first-commit", description = "Finds the first commit since a given date")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public @NonNull String firstCommit(@Option(description = "By author") String author,
      @Option(longNames = "source") String sourceFilePath,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates ) {

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, null, excludingDates, null)
        .and(commitsByAuthorQueryPredicate(author))
        .and(commitsToSourceFilePathQueryPredicate(sourceFilePath));

    CommitHistory commits = queryCommitHistory(queryPredicate);

    Optional<CommitRecord> commitRecord = commits.stream().min(CommitRecordComparator.INSTANCE);

    return commitRecord
      .map(it -> showCommitRecord(it, showFiles))
      .map(Object::toString)
      .orElseGet(() -> isProjectSet()
        ? String.format("No commit after date [%s] was found", sinceDate)
        : PROJECT_NOT_FOUND);
  }

  @Command(command = "last-commit", description = "Finds the last commit before a given date")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public @NonNull String lastCommit(@Option(description = "By author") String author,
      @Option(longNames = "until", shortNames = 'u') String untilDate,
      @Option(longNames = "source") String sourceFilePath,
      @Option(longNames = "show-files", shortNames = 'f', defaultValue = DEFAULT_SHOW_FILES_OPTION) boolean showFiles,
      @Option(longNames = "exclude-dates", shortNames = 'e') String excludingDates ) {

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(null, untilDate, excludingDates, null)
        .and(commitsByAuthorQueryPredicate(author))
        .and(commitsToSourceFilePathQueryPredicate(sourceFilePath));

    CommitHistory commits = queryCommitHistory(queryPredicate);

    Optional<CommitRecord> commitRecord = commits.stream().max(CommitRecordComparator.INSTANCE);

    return commitRecord
      .map(it -> showCommitRecord(it, showFiles))
      .map(Object::toString)
      .orElseGet(() -> isProjectSet()
        ? String.format("No commit before date [%s] was found", untilDate)
        : PROJECT_NOT_FOUND);
  }

  @Command(command = "source-files", description = "Finds all source files with commit message like")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public @NotNull String sourceFilesWithCommitMessage(
      @Option(description = "Commit message like; use '|' to (OR) multiple commit messages") String commitMessage,
      @Option(longNames = "exclude-filter") String excludeFilter,
      @Option(longNames = "include-filter") String includeFilter,
      @Option(longNames = "since", shortNames = 's') String sinceDate,
      @Option(longNames = "strict", defaultValue = "false") boolean strict,
      @Option(longNames = "until", shortNames = 'u') String untilDate) {

    Predicate<CommitRecord> commitsWithMessageQueryPredicate = commitRecord -> {

      String[] commitMessages = commitMessage.split(Utils.PIPE_SEPARATOR_REGEX);

      for (String message : commitMessages) {
        if (commitRecord.getMessage().toLowerCase().contains(String.valueOf(message).toLowerCase())) {
          return true;
        }
      }

      return false;
    };

    Predicate<CommitRecord> queryPredicate =
      commitsByTimeQueryPredicate(sinceDate, untilDate, null, null)
        .and(commitsWithMessageQueryPredicate);

    CommitHistory commits = queryCommitHistory(queryPredicate);

    boolean excludeFilterSet = StringUtils.hasText(excludeFilter);
    boolean includeFilterSet = StringUtils.hasText(includeFilter);

    @SuppressWarnings("all")
    Predicate<SourceFile> sourceFilePredicate = sourceFile -> {
      boolean accept = sourceFile != null;
      accept &= !(excludeFilterSet && sourceFile.getFile().getAbsolutePath().contains(excludeFilter));
      accept &= !includeFilterSet || sourceFile.getFile().getAbsolutePath().contains(includeFilter);
      return accept;
    };

    if (strict) {

      Predicate<SourceFile> sourceFileTimePredicate = sourceFile -> {

         boolean result = !StringUtils.hasText(sinceDate) || sourceFile.getFirstRevisionDateTime()
           .map(LocalDateTime::toLocalDate)
           .filter(date -> !date.isBefore(LocalDate.parse(sinceDate, INPUT_DATE_FORMATTER)))
           .isPresent();

         result &= !StringUtils.hasText(untilDate) || sourceFile.getLastRevisionDateTime()
           .map(LocalDateTime::toLocalDate)
           .filter(date -> !date.isAfter(LocalDate.parse(untilDate, INPUT_DATE_FORMATTER)))
           .isPresent();

         return result;
      };

      sourceFilePredicate = sourceFilePredicate.and(sourceFileTimePredicate);
    }

    SourceFileSet sourceFileSet = commits.toSourceFileSet()
      .findBy(sourceFilePredicate);

    StringBuilder output = new StringBuilder(Utils.newLine());

    for (SourceFile sourceFile : sourceFileSet) {
      output.append(Utils.newLineBefore(sourceFile.getFile().getAbsolutePath()));
    }

    output.append(Utils.newLineBefore("Count: "))
      .append(Utils.newLineAfter(String.valueOf(sourceFileSet.size())));

    return output.toString();
  }

  @Command(command = "status", description = "Reports git status")
  @CommandAvailability(provider = "gitCommandsAvailability")
  public String status() {

    GitStatus status = getGitTemplate().getCommitStatus();

    Function<Stream<String>, String> streamToString = stream -> stream
      .map(it -> String.format("%s%n", it))
      .reduce(String::concat)
      .orElse(Utils.EMPTY_STRING);

    AttributedStringBuilder output = new AttributedStringBuilder();

    if (status.isDirty()) {
      if (status.hasAdded()) {
        output.style(toBoldItalicText(Colors.GREEN))
          .append(Utils.newLineBeforeAfter("Added: "))
          .style(toPlainText(Colors.GREEN))
          .append(streamToString.apply(status.streamAdded().sorted()));
      }
      if (status.hasConflicts()) {
        output.style(toBoldItalicText(Colors.RED))
          .append(Utils.newLineBeforeAfter("Conflicts: "))
          .style(toPlainText(Colors.RED))
          .append(streamToString.apply(status.streamConflicts().sorted()));
      }
      if (status.hasIgnoredChanges()) {
        output.style(toBoldItalicText(Colors.GREY))
          .append(Utils.newLineBeforeAfter("Ignored: "))
          .style(toPlainText(Colors.GREY))
          .append(streamToString.apply(status.streamIgnored().sorted()));
      }
      if (status.hasMissing()) {
        output.style(toBoldItalicText(Colors.YELLOW))
          .append(Utils.newLineBeforeAfter("Missing: "))
          .style(toPlainText(Colors.YELLOW))
          .append(streamToString.apply(status.streamMissing().sorted()));
      }
      if (status.hasRemoved()) {
        output.style(toBoldItalicText(Colors.BLUE))
          .append(Utils.newLineBeforeAfter("Removed: "))
          .style(toPlainText(Colors.BLUE))
          .append(streamToString.apply(status.streamRemoved().sorted()));
      }
      if (status.hasUncommittedChanges()) {
        output.style(toBoldItalicText(Colors.RED))
          .append(Utils.newLineBeforeAfter("Uncommitted: "))
          .style(toPlainText(Colors.RED))
          .append(streamToString.apply(status.streamUncommitted().sorted()));
      }
      if (status.hasUntrackedChanges()) {
        output.style(toBoldItalicText(Colors.YELLOW))
          .append(Utils.newLineBeforeAfter("Untracked: "))
          .style(toPlainText(Colors.YELLOW))
          .append(streamToString.apply(status.streamUntracked().sorted()));
      }
    }

    return output.toAnsi();
  }

  protected @NonNull CommitHistory queryCommitHistory() {
    return queryCommitHistory(ALL_COMMITS_PREDICATE);
  }

  protected @NonNull CommitHistory queryCommitHistory(@NonNull Predicate<CommitRecord> queryPredicate) {

    return currentProject()
      .map(project -> queryCommitHistory(project, Utils.nullSafeNonMatchingPredicate(queryPredicate)))
      .orElseGet(CommitHistory::empty);
  }

  protected @NonNull CommitHistory queryCommitHistory(@NonNull Project project,
      @NonNull Predicate<CommitRecord> queryPredicate) {

    return queryCommitHistory(resolveCommitHistory(project), queryPredicate);
  }

  protected @NonNull CommitHistory queryCommitHistory(@NonNull CommitHistory commitHistory,
      @NonNull Predicate<CommitRecord> queryPredicate) {

    return commitHistory.findBy(queryPredicate);
  }

  private static @NonNull Predicate<CommitRecord> commitsByAuthorQueryPredicate(@Nullable String authorCommitter) {

    return Utils.isNotSet(authorCommitter) ? ALL_COMMITS_PREDICATE
      : commitRecord -> {

        String resolvedAuthorCommitter = Utils.nullSafeTrimmedString(authorCommitter).toLowerCase();

        Author commitAuthor = commitRecord.getAuthor();

        return commitAuthor.getName().toLowerCase().contains(resolvedAuthorCommitter)
          || commitAuthor.getEmailAddress().contains(resolvedAuthorCommitter);
    };
  }

  private static @NonNull Predicate<CommitRecord> commitsByTimeQueryPredicate(@Nullable String sinceDate,
      @Nullable String untilDate, @Nullable String excludingDates, @Nullable String duringDates) {

    return commitsSinceDateQueryPredicate(sinceDate)
      .and(commitsUntilDateQueryPredicate(untilDate))
      .and(commitsExcludingDatesQueryPredicate(excludingDates))
      .and(commitsDuringDatesQueryPredicate(duringDates));
  }

  private static @NonNull Predicate<CommitRecord> commitsDuringDatesQueryPredicate(@Nullable String duringDates) {

    return StringUtils.hasText(duringDates)
      ? commitRecord -> TimePeriods.parse(duringDates).asPredicate().test(commitRecord.getDate())
      : ALL_COMMITS_PREDICATE;
  }

  private static @NonNull Predicate<CommitRecord> commitsExcludingDatesQueryPredicate(@Nullable String excludingDates) {

    return StringUtils.hasText(excludingDates)
      ? commitRecord -> !TimePeriods.parse(excludingDates).asPredicate().test(commitRecord.getDate())
      : ALL_COMMITS_PREDICATE;
  }

  private static @NonNull Predicate<CommitRecord> commitsSinceDateQueryPredicate(@Nullable String sinceDate) {

    return commitRecord -> {

      LocalDate since = StringUtils.hasText(sinceDate)
        ? LocalDate.parse(sinceDate, INPUT_DATE_FORMATTER)
        : Utils.atEpoch().toLocalDate();

      LocalDate commitDate = commitRecord.getDate();

      return !commitDate.isBefore(since);
    };
  }

  @SuppressWarnings("all")
  private static @NonNull Predicate<CommitRecord> commitsToSourceFilePathQueryPredicate(@Nullable String sourceFilePath) {

    return Utils.isSet(sourceFilePath) ? commitRecord -> commitRecord.stream().anyMatch(sourceFile ->
        sourceFile.getAbsolutePath().contains(sourceFilePath))
      : ALL_COMMITS_PREDICATE;
  }

  private static @NonNull Predicate<CommitRecord> commitsUntilDateQueryPredicate(@Nullable String untilDate) {

    return commitRecord -> {

      LocalDate until = StringUtils.hasText(untilDate)
        ? LocalDate.parse(untilDate, INPUT_DATE_FORMATTER)
        : LocalDate.now();

      LocalDate commitDate = commitRecord.getDate();

      return !commitDate.isAfter(until);
    };
  }

  protected @NonNull CommitHistory resolveCommitHistory(@NonNull Project project) {
    return Utils.get(Utils.get(project.getCommitHistory(), () -> loadCommitHistory(project)), CommitHistory::empty);
  }

  private @NonNull CommitHistory loadCommitHistory(@NonNull Project project) {
    return project.withCommitHistory(getGitTemplate().getCommitHistory()).getCommitHistory();
  }

  protected @NonNull StringBuilder showCommitHistory(@NonNull CommitHistory commits) {
    return showCommitHistory(commits, commits.size(), DEFAULT_SHOW_FILES);
  }

  protected @NonNull StringBuilder showCommitHistory(@NonNull CommitHistory commits, int limit) {
    return showCommitHistory(commits, limit, DEFAULT_SHOW_FILES);
  }

  protected @NonNull StringBuilder showCommitHistory(@NonNull CommitHistory commits, boolean showFiles) {
    return showCommitHistory(commits, commits.size(), showFiles);
  }

  protected @NonNull StringBuilder showCommitHistory(@NonNull CommitHistory commits, int limit, boolean showFiles) {

    StringBuilder output = new StringBuilder();

    commits.stream().limit(limit).sorted().toList()
      .forEach(commitRecord -> showCommitRecord(commitRecord, showFiles, output));

    return output;
  }

  protected @NonNull Function<CommitHistory, String> showCommitHistoryFunction(
      boolean count, int limit, boolean showFiles) {

    return commitHistory -> count
      ? String.valueOf(commitHistory.size())
      : showCommitHistory(commitHistory, limit, showFiles).toString();
  }

  protected @NonNull StringBuilder showCommitRecord(@NonNull CommitRecord commitRecord) {
    return showCommitRecord(commitRecord, DEFAULT_SHOW_FILES);
  }

  protected @NonNull StringBuilder showCommitRecord(@NonNull CommitRecord commitRecord, boolean showFiles) {

    StringBuilder output = new StringBuilder();

    showCommitRecord(commitRecord, showFiles, output);

    return output;
  }

  private @NonNull StringBuilder showCommitRecord(@NonNull CommitRecord commitRecord, @NonNull StringBuilder output) {
    return showCommitRecord(commitRecord, DEFAULT_SHOW_FILES, output);
  }

  private @NonNull StringBuilder showCommitRecord(@NonNull CommitRecord commitRecord, boolean showFiles,
      @NonNull StringBuilder output) {

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
    return commitRecord.getDateTime().format(COMMIT_DATE_TIME_FORMATTER);
  }

  private String toCommitDateByCountString(Object value) {

    return value instanceof LocalDate localDate ? localDate.format(COMMIT_DATE_FORMATTER)
      : value instanceof YearMonth yearMonth ? yearMonth.format(COMMIT_DATE_YEAR_MONTH_FORMATTER)
      : value instanceof Year year ? String.valueOf(year.getValue())
      : String.valueOf(value);
  }

  private String toCommitMessageString(CommitRecord commitRecord) {
    return indent(commitRecord.getMessage());
  }

  private String toCommitSourceFileString(File source) {
    return source.getAbsolutePath();
  }

  @NonNull @Bean
  AvailabilityProvider gitCommandsAvailability() {

    return isProjectSet() ? Availability::available
      : () -> Availability.unavailable("the current project is not set;"
        + " please call 'project load <location>' or 'project use <name>'");
  }
}
