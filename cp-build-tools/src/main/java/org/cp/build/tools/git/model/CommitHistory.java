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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cp.build.tools.api.model.SourceFile;
import org.cp.build.tools.api.model.SourceFile.Revision;
import org.cp.build.tools.api.model.SourceFileSet;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.git.model.CommitRecord.Author;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract Data Type (ADT) modeling {@literal Git log}, or {@literal commit history}.
 *
 * @author John Blum
 * @see org.cp.build.tools.git.model.CommitRecord
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class CommitHistory implements Iterable<CommitRecord> {

  /**
   * Factory method used to construct a new, empty {@link CommitHistory}.
   *
   * @return a new, empty {@link CommitHistory}.
   * @see #of(CommitRecord...)
   */
  public static @NonNull CommitHistory empty() {
    return of();
  }

  /**
   * Factory method used to construct a new {@link CommitHistory} initialized with the given array
   * of {@link CommitRecord CommitRecords}.
   *
   * @param commitRecords array of {@link CommitRecord CommitRecords} used to initialize the new {@link CommitHistory}.
   * @return a new {@link CommitHistory} initialize with the given array of {@link CommitRecord CommitRecords}.
   * @see org.cp.build.tools.git.model.CommitRecord
   */
  public static @NonNull CommitHistory of(CommitRecord... commitRecords) {

    return of(Arrays.stream(commitRecords)
      .filter(Objects::nonNull)
      .collect(Collectors.toList()));
  }

  /**
   * Factory method used to construct a new {@link CommitHistory} initialized with the given {@link Iterable}
   * of {@link CommitRecord CommitRecords}.
   *
   * @param commitRecords {@link Iterable collection} of {@link CommitRecord CommitRecords} used to initialize
   * the new {@link CommitHistory}.
   * @return a new {@link CommitHistory} initialize with the given {@link Iterable}
   * of {@link CommitRecord CommitRecords}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.lang.Iterable
   */
  public static @NonNull CommitHistory of(Iterable<CommitRecord> commitRecords) {
    return new CommitHistory(commitRecords);
  }

  private final List<CommitRecord> commitRecords = new ArrayList<>();

  /**
   * Constructs a new {@link CommitHistory} initialized with the given {@link Iterable collection}
   * of {@link CommitRecord CommitRecords}.
   *
   * @param commitRecords {@link Iterable collection} of {@link CommitRecord CommitRecords} making up the history
   * of the commit log.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.lang.Iterable
   */
  private CommitHistory(@NonNull Iterable<CommitRecord> commitRecords) {

    Utils.stream(commitRecords)
      .filter(Objects::nonNull)
      .sorted()
      .map(commitRecord -> commitRecord.from(this))
      .forEach(this.commitRecords::add);
  }

  /**
   * Determines whether this {@link CommitHistory} contains any {@link CommitRecord CommitRecords}.
   *
   * @return a boolean value indicating whether this {@link CommitHistory}
   * contains any {@link CommitRecord CommitRecords}.
   * @see #getCommitRecords()
   * @see #size()
   */
  public boolean isEmpty() {
    return getCommitRecords().isEmpty();
  }

  /**
   * Returns an unmodifiable {@link List} of {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}.
   * <p>
   * The {@link CommitRecord CommitRecords} will be sorted in reverse chronological order.
   *
   * @return an unmodifiable {@link List} of {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.List
   */
  protected List<CommitRecord> getCommitRecords() {
    return Collections.unmodifiableList(this.commitRecords);
  }

  /**
   * Collects all {@link CommitRecord commits} from this {@link CommitHistory} since and including
   * the {@link CommitRecord commit} with the given {@link String hash ID}.
   *
   * @param hash {@link String Hash ID} identifying a single {@link CommitRecord commit} in this {@link CommitHistory}.
   * @return a new {@link CommitHistory} containing all {@link CommitRecord commits} from this {@link CommitHistory}
   * since, including and after the {@link CommitRecord commit} with the given {@link String hash ID}.
   * @see #findAllCommitsBeforeHash(String)
   */
  public @NonNull CommitHistory findAllCommitsAfterHash(@NonNull String hash) {

    if (StringUtils.hasText(hash)) {

      List<CommitRecord> commits = new ArrayList<>();

      for (CommitRecord commit : this) {
        commits.add(commit);
        if (commit.getHash().equals(hash)) {
          break;
        }
      }

      return of(commits);
    }

    return empty();
  }

  /**
   * Collects all {@link CommitRecord commits} from this {@link CommitHistory} until and including
   * the {@link CommitRecord commit} with the given {@link String hash ID}.
   *
   * @param hash {@link String Hash ID} identifying a single {@link CommitRecord commit} in this {@link CommitHistory}.
   * @return a new {@link CommitHistory} containing all {@link CommitRecord commits} from this {@link CommitHistory}
   * until, including and before the {@link CommitRecord commit} with the given {@link String hash ID}.
   * @see #findAllCommitsAfterHash(String)
   */
  public @NonNull CommitHistory findAllCommitsBeforeHash(@NonNull String hash) {

    if (StringUtils.hasText(hash)) {

      boolean include = false;

      List<CommitRecord> commits = new ArrayList<>();

      for (CommitRecord commit : this) {
        if (include || commit.getHash().equals(hash)) {
          commits.add(commit);
          include = true;
        }
      }

      return of(commits);
    }

    return empty();
  }

  /**
   * Finds all {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}
   * matching the given {@link Predicate}.
   * <p>
   * If the given {@link Predicate query predicate} is {@literal null}, then the query will not match
   * any {@link CommitRecord commits} from this {@link CommitHistory}.
   *
   * @param predicate {@link Predicate} used to match and return {@link CommitRecord CommitRecords}
   * from this {@link CommitHistory}.
   * @return a new {@link CommitHistory} containing a {@link List} of {@link CommitRecord CommitRecords}
   * from this {@link CommitHistory} matching the given {@link Predicate}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.function.Predicate
   * @see #getCommitRecords()
   * @see #of(Iterable)
   */
  public @NonNull CommitHistory findBy(@NonNull Predicate<CommitRecord> predicate) {

    return CommitHistory.of(getCommitRecords().stream()
      .filter(Utils.nullSafeNonMatchingPredicate(predicate))
      .toList());
  }

  /**
   * Finds all {@link CommitRecord CommitRecords} contained in this {@link CommitHistory} for the given {@link Author}.
   *
   * @param author {@link Author} who's {@link CommitRecord CommitRecords} will be returned.
   * @return a new {@link CommitHistory} containing all {@link CommitRecord CommitRecords}
   * from this {@link CommitHistory} for the given {@link Author}.
   * @see org.cp.build.tools.git.model.CommitRecord.Author
   * @see #findBy(Predicate)
   */
  public @NonNull CommitHistory findByAuthor(@NonNull Author author) {
    return findBy(commitRecord -> commitRecord.getAuthor().equals(author));
  }

  /**
   * Finds all {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}
   * on the given {@link LocalDate} regardless of time.
   *
   * @param author {@link LocalDate} used to match and return {@link CommitRecord CommitRecords} on a particular day.
   * @return a new {@link CommitHistory} containin all {@link CommitRecord CommitRecords}
   * from this {@link CommitHistory} on the given {@link LocalDate}.
   * @see java.time.LocalDate
   * @see #findBy(Predicate)
   */
  @SuppressWarnings("all")
  public @NonNull CommitHistory findByDate(@NonNull LocalDate date) {

    LocalDate resolvedDate = date != null ? date : LocalDate.now();

    return findBy(commitRecord -> commitRecord.getDateTime().toLocalDate().equals(resolvedDate));
  }

  /**
   * Finds a single, {@link Optional} {@link CommitRecord} contained in this {@link CommitHistory}
   * with the given {@link String hash ID}.
   *
   * @param hash {@link String} containing the {@literal hash ID} identifying the {@link CommitRecord}
   * from this {@link CommitHistory} to return.
   * @return an {@link Optional} {@link CommitRecord} contained in this {@link CommitHistory}
   * with the given {@link String hash ID}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.Optional
   * @see #findBy(Predicate)
   */
  public Optional<CommitRecord> findByHash(@NonNull String hash) {
    return findBy(commitRecord -> commitRecord.getHash().equalsIgnoreCase(hash)).stream().findFirst();
  }

  /**
   * Finds all {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}
   * in which the given {@link File source file} was changed and committed.
   *
   * @param sourceFile {@link File} used to match and return {@link CommitRecord CommitRecords}
   * in which the {@literal source file} was changed and committed.
   * @return a new {@link CommitHistory} containing all {@link CommitRecord CommitRecords}
   * from this {@link CommitHistory} in which the given {@link File source file} was changed and committed.
   * @see #findBy(Predicate)
   * @see java.io.File
   */
  public @NonNull CommitHistory findBySourceFile(@NonNull File sourceFile) {
    return findBy(commitRecord -> commitRecord.contains(sourceFile));
  }

  /**
   * Returns an {@link Optional} {@link CommitRecord first commit} in this {@link CommitHistory}.
   *
   * @return an {@link Optional} {@link CommitRecord first commit} in this {@link CommitHistory}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.Optional
   */
  public Optional<CommitRecord> firstCommit() {
    return Optional.ofNullable(CollectionUtils.lastElement(getCommitRecords()));
  }

  /**
   * Iterates all the {@link CommitRecord commits} in this {@link CommitHistory} in reverse chronological order.
   *
   * @return an {@link Iterator} iterating over all the {@link CommitRecord commits} in this {@link CommitHistory}
   * in reverse chronological order.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.Iterator
   */
  @Override
  public @NonNull Iterator<CommitRecord> iterator() {
    return getCommitRecords().iterator();
  }

  /**
   * Returns the {@link Integer number} of {@literal commits} in this {@link CommitHistory}.
   *
   * @return the {@link Integer number} of {@literal commits} in this {@link CommitHistory}.
   * @see #isEmpty()
   */
  public int size() {
    return getCommitRecords().size();
  }

  /**
   * Streams the {@link CommitRecord CommitRecords} in this {@link CommitHistory}.
   *
   * @return a {@link Stream} of {@link CommitRecord CommitRecords} in this {@link CommitHistory}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.stream.Stream
   */
  public @NonNull Stream<CommitRecord> stream() {
    return Utils.stream(this);
  }

  /**
   * Converts this {@link CommitHistory} to a {@link SourceFileSet} with a {@literal revision history}.
   *
   * @return a {@link SourceFileSet} with {@literal revision history} from this {@link CommitHistory}.
   * @see org.cp.build.tools.api.model.SourceFileSet
   * @see org.cp.build.tools.api.model.SourceFile
   */
  public @NonNull SourceFileSet toSourceFileSet() {

    SourceFileSet sourceFileSet = SourceFileSet.empty();

    for (CommitRecord commitRecord : this) {
      for (File file : commitRecord) {
        if (Utils.nullSafeIsFile(file)) {

          SourceFile sourceFile = sourceFileSet.resolve(file)
            .withRevision(toSourceFileRevision(commitRecord));

          sourceFileSet.add(sourceFile);
        }
      }
    }

    return sourceFileSet;
  }

  private @NonNull Revision toSourceFileRevision(CommitRecord commitRecord) {

    return SourceFile.Revision.of(
      toSourceFileAuthor(commitRecord.getAuthor()),
      commitRecord.getDateTime(),
      commitRecord.getHash()
    );
  }

  private SourceFile.Author toSourceFileAuthor(CommitRecord.Author author) {
    return SourceFile.Author.as(author.getName()).withEmailAddress(author.getEmailAddress());
  }
}
