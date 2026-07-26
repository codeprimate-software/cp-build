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
package org.cp.build.tools.api.model;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.api.time.TimePeriods;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Unique collection of ordered {@link SourceFile SourceFiles}.
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @see org.cp.build.tools.api.model.SourceFile
 * @since 0.1.0
 */
@SuppressWarnings("unused")
public class SourceFileSet implements Iterable<SourceFile> {

  public static SourceFileSet empty() {
    return of();
  }

  public static SourceFileSet of(SourceFile... array) {
    return of(Arrays.asList(array));
  }

  public static SourceFileSet of(Iterable<SourceFile> sourceFiles) {

    SourceFileSet collection = new SourceFileSet();

    Utils.stream(sourceFiles)
      .filter(Objects::nonNull)
      .forEach(collection.sourceFiles::add);

    return collection;
  }

  @Getter(AccessLevel.PROTECTED)
  private final Set<SourceFile> sourceFiles = new TreeSet<>();

  @SuppressWarnings("all")
  public boolean add(SourceFile sourceFile) {
    return sourceFile != null && this.sourceFiles.add(sourceFile);
  }

  public boolean contains( File file) {
    return file != null && stream().anyMatch(sourceFile -> sourceFile.getFile().equals(file));
  }

  public SourceFileSet findBy(Predicate<SourceFile> sourceFilePredicate) {

    return SourceFileSet.of(stream()
      .filter(Utils.nullSafeNonMatchingPredicate(sourceFilePredicate))
      .collect(Collectors.toSet()));
  }

  public SourceFileSet findByAuthor(SourceFile.Author author) {
    return findBy(sourceFile -> sourceFile.wasModifiedBy(author));
  }

  // Find (Query) by Author name or email address
  public SourceFileSet findByAuthor(String author) {
    return findBy(sourceFile -> sourceFile.wasModifiedBy(author));
  }

  public Optional<SourceFile> findByFile(File file) {
    return stream().filter(sourceFile -> sourceFile.getFile().equals(file)).findAny();
  }

  public SourceFileSet findByRevisionId(String revisionId) {
    return findBy(sourceFile -> sourceFile.getRevisionIds().contains(revisionId));
  }

  public SourceFileSet findDuring(TimePeriods timePeriods) {
    return findBy(sourceFile -> sourceFile.wasModifiedDuring(timePeriods));
  }

  public boolean isEmpty() {
    return getSourceFiles().isEmpty();
  }

  @Override
  public Iterator<SourceFile> iterator() {
    return Collections.unmodifiableSet(getSourceFiles()).iterator();
  }

  public SourceFile resolve(File file) {

    return findByFile(file)
      .orElseGet(() -> {
        SourceFile sourceFile = SourceFile.from(file);
        this.sourceFiles.add(sourceFile);
        return sourceFile;
      });
  }

  public int size() {
    return getSourceFiles().size();
  }

  public Stream<SourceFile> stream() {
    return Utils.stream(this);
  }
}
