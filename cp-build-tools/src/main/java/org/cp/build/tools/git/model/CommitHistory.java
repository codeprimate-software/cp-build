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

import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.git.model.CommitRecord.Author;
import org.springframework.lang.NonNull;

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
      .forEach(this.commitRecords::add);
  }

  /**
   * Determines whether this {@link CommitHistory} contains any {@link CommitRecord CommitRecords}.
   *
   * @return a boolean value indicating whether this {@link CommitHistory}
   * contains any {@link CommitRecord CommitRecords}.
   * @see #getCommitRecords()
   */
  public boolean isEmpty() {
    return getCommitRecords().isEmpty();
  }

  /**
   * Returns an unmodifiable {@link List} of {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}.
   *
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
   * Finds all {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}
   * matching the given {@link Predicate}.
   *
   * @param predicate {@link Predicate} used to match and return {@link CommitRecord CommitRecords}
   * from this {@link CommitHistory}.
   * @return a {@link List} of {@link CommitRecord CommitRecords} from this {@link CommitHistory}
   * matching the given {@link Predicate}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.function.Predicate
   * @see #getCommitRecords()
   * @see java.util.List
   */
  public List<CommitRecord> findBy(@NonNull Predicate<CommitRecord> predicate) {

    return getCommitRecords().stream()
      .filter(Utils.nullSafeNonMatchingPredicate(predicate))
      .collect(Collectors.toList());
  }

  /**
   * Finds all {@link CommitRecord CommitRecords} contained in this {@link CommitHistory} for the given {@link Author}.
   *
   * @param author {@link Author} who's {@link CommitRecord CommitRecords} will be returned.
   * @return all {@link CommitRecord CommitRecords} in this {@link CommitHistory} for the given {@link Author}.
   * @see org.cp.build.tools.git.model.CommitRecord.Author
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see #findBy(Predicate)
   * @see java.util.List
   */
  public List<CommitRecord> findByAuthor(@NonNull Author author) {
    return findBy(commitRecord -> commitRecord.getAuthor().equals(author));
  }

  /**
   * Finds all {@link CommitRecord CommitRecords} contained in this {@link CommitHistory}
   * on the given {@link LocalDate} regardless of time.
   *
   * @param author {@link LocalDate} used to match and return {@link CommitRecord CommitRecords} on a particular day.
   * @return all {@link CommitRecord CommitRecords} in this {@link CommitHistory} on the given {@link LocalDate}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.time.LocalDate
   * @see #findBy(Predicate)
   * @see java.util.List
   */
  @SuppressWarnings("all")
  public List<CommitRecord> findByDate(@NonNull LocalDate date) {

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
   * @return all {@link CommitRecord CommitRecords} in this {@link CommitHistory}
   * in which the given {@link File source file} was changed and committed.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see #findBy(Predicate)
   * @see java.util.List
   * @see java.io.File
   */
  public List<CommitRecord> findBySourceFile(@NonNull File sourceFile) {
    return findBy(commitRecord -> commitRecord.contains(sourceFile));
  }

  @Override
  public Iterator<CommitRecord> iterator() {
    return getCommitRecords().iterator();
  }

  /**
   * Streams the {@link CommitRecord CommitRecords} in this {@link CommitHistory}.
   *
   * @return a {@link Stream} of {@link CommitRecord CommitRecords} in this {@link CommitHistory}.
   * @see org.cp.build.tools.git.model.CommitRecord
   * @see java.util.stream.Stream
   */
  public Stream<CommitRecord> stream() {
    return Utils.stream(this);
  }

  /**
   * Returns the {@link Integer number} of {@literal commits} in this {@link CommitHistory}.
   *
   * @return the {@link Integer number} of {@literal commits} in this {@link CommitHistory}.
   */
  public int size() {
    return getCommitRecords().size();
  }
}
