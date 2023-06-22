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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.cp.build.tools.core.support.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract Data Type (ADT) modeling a {@literal Git commit}.
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @since 2.0.0
 */
@Getter
@EqualsAndHashCode(of = "hash")
@RequiredArgsConstructor(staticName = "of")
@SuppressWarnings("unused")
public class CommitRecord implements Comparable<CommitRecord>, Iterable<File> {

  protected static final int SHORT_HASH_LENGTH = 7;

  protected static final String COMMIT_DATE_FORMAT_PATTERN = "EEE MMM dd HH:mm:ss yyyy Z";

  @lombok.NonNull
  private final Author author;

  @lombok.NonNull
  private final LocalDateTime date;

  @Getter(AccessLevel.PROTECTED)
  private final Set<File> sourceFiles = new ConcurrentSkipListSet<>();

  @lombok.NonNull
  private final String hash;

  @Setter(AccessLevel.PROTECTED)
  private String message;

  public @NonNull String getShortHash() {
    return getHash().substring(0, SHORT_HASH_LENGTH);
  }

  public @NonNull CommitRecord add(File... sourceFiles) {

    Arrays.stream(Utils.nullSafeFileArray(sourceFiles))
      .filter(Objects::nonNull)
      .forEach(this.sourceFiles::add);

    return this;
  }

  @Override
  public int compareTo(@NonNull CommitRecord that) {
    return that.getDate().compareTo(this.getDate());
  }

  public boolean contains(File sourceFile) {
    return getSourceFiles().contains(sourceFile);
  }

  @Override
  public Iterator<File> iterator() {
    return Collections.unmodifiableSet(getSourceFiles()).stream().sorted().iterator();
  }

  public @NonNull CommitRecord withMessage(String message) {
    setMessage(message);
    return this;
  }

  @Override
  public String toString() {
    return getHash();
  }

  @Getter
  @EqualsAndHashCode(of = "name")
  @RequiredArgsConstructor(staticName = "as")
  public static class Author implements Comparable<Author> {

    public static Author parse(@NonNull String authorString) {

      String[] authorComponents = authorString.split(Utils.SINGLE_SPACE);

      Author author = new Author(authorComponents[0]);

      return authorComponents.length > 1
        ? author.withEmail(authorComponents[1])
        : author;
    }

    @lombok.NonNull
    private final String name;

    @Setter(AccessLevel.PROTECTED)
    private String email;

    @Override
    public int compareTo(@NonNull CommitRecord.Author that) {
      return this.getName().compareTo(that.getName());
    }

    public @NonNull Author withEmail(@Nullable String email) {
      setEmail(email);
      return this;
    }

    @Override
    public String toString() {
      return getName();
    }
  }
}
