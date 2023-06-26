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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.cp.build.tools.api.support.Utils;
import org.springframework.lang.NonNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract Data Type (ADT) modeling {@literal git status} metadata.
 *
 * @author John Blum
 * @since 2.0.0
 */
@Getter(AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class GitStatus {

  public static @NonNull GitStatus create() {
    return new GitStatus();
  }

  @Setter(AccessLevel.PRIVATE)
  private boolean clean;

  private final Set<String> added = new HashSet<>();
  private final Set<String> changedModified = new HashSet<>();
  private final Set<String> conflicts = new HashSet<>();
  private final Set<String> ignored = new HashSet<>();
  private final Set<String> missing = new HashSet<>();
  private final Set<String> removed = new HashSet<>();
  private final Set<String> uncommitted = new HashSet<>();
  private final Set<String> untracked = new HashSet<>();

  public boolean isChangedOrModified() {
    return !this.changedModified.isEmpty();
  }

  public boolean isClean() {

    return this.clean && !(hasAdded()
      || hasConflicts()
      || hasIgnoredChanges()
      || hasMissing()
      || hasRemoved()
      || hasUncommittedChanges()
      || hasUntrackedChanges());
  }

  public boolean isDirty() {
    return !isClean();
  }

  public boolean hasAdded() {
    return !this.added.isEmpty();
  }

  public boolean hasConflicts() {
    return !this.conflicts.isEmpty();
  }

  public boolean hasIgnoredChanges() {
    return !this.ignored.isEmpty();
  }

  public boolean hasMissing() {
    return !this.missing.isEmpty();
  }

  public boolean hasRemoved() {
    return !this.removed.isEmpty();
  }

  public boolean hasUncommittedChanges() {
    return !this.uncommitted.isEmpty();
  }

  public boolean hasUntrackedChanges() {
    return !this.untracked.isEmpty();
  }

  public Stream<String> streamAdded() {
    return Collections.unmodifiableSet(getAdded()).stream();
  }

  public Stream<String> streamChangedModified() {
    return Collections.unmodifiableSet(getChangedModified()).stream();
  }

  public Stream<String> streamConflicts() {
    return Collections.unmodifiableSet(getConflicts()).stream();
  }

  public Stream<String> streamIgnored() {
    return  Collections.unmodifiableSet(getIgnored()).stream();
  }

  public Stream<String> streamMissing() {
    return  Collections.unmodifiableSet(getMissing()).stream();
  }

  public Stream<String> streamRemoved() {
    return  Collections.unmodifiableSet(getRemoved()).stream();
  }

  public Stream<String> streamUncommitted() {
    return  Collections.unmodifiableSet(getUncommitted()).stream();
  }

  public Stream<String> streamUntracked() {
    return  Collections.unmodifiableSet(getUntracked()).stream();
  }

  protected String[] toArray(Iterable<String> iterable) {
    return toStringArray(Utils.stream(iterable).filter(Objects::nonNull).toArray());
  }

  private String[] toStringArray(Object[] array) {

    int index = 0;
    String[] stringArray = new String[array.length];

    for (Object value : array) {
      stringArray[index++] = String.valueOf(value);
    }

    return stringArray;
  }

  protected @NonNull GitStatus with(Consumer<String> consumer, String... values) {

    Arrays.stream(values)
      .filter(Utils::isSet)
      .forEach(consumer);

    return this;
  }

  public @NonNull GitStatus withAdded(String... added) {
    return with(getAdded()::add, added);
  }

  public @NonNull GitStatus withAdded(Iterable<String> added) {
    return withAdded(toArray(added));
  }

  public @NonNull GitStatus withChangedModified(String... changedModified) {
    return with(getChangedModified()::add, changedModified);
  }

  public @NonNull GitStatus withChangedModified(Iterable<String> changedModified) {
    return withChangedModified(toArray(changedModified));
  }

  public @NonNull GitStatus withClean(boolean clean) {
    setClean(clean);
    return this;
  }

  public @NonNull GitStatus withConflicts(String... conflicts) {
    return with(getConflicts()::add, conflicts);
  }

  public @NonNull GitStatus withConflicts(Iterable<String> conflicts) {
    return withConflicts(toArray(conflicts));
  }

  public @NonNull GitStatus withIgnored(String... ignored) {
    return with(getIgnored()::add, ignored);
  }

  public @NonNull GitStatus withIgnored(Iterable<String> ignored) {
    return withIgnored(toArray(ignored));
  }

  public @NonNull GitStatus withMissing(String... missing) {
    return with(getMissing()::add, missing);
  }

  public @NonNull GitStatus withMissing(Iterable<String> missing) {
    return withMissing(toArray(missing));
  }

  public @NonNull GitStatus withRemoved(String... removed) {
    return with(getRemoved()::add, removed);
  }

  public @NonNull GitStatus withRemoved(Iterable<String> removed) {
    return withRemoved(toArray(removed));
  }

  public @NonNull GitStatus withUncommitted(String... uncommitted) {
    return with(getUncommitted()::add, uncommitted);
  }

  public @NonNull GitStatus withUncommitted(Iterable<String> uncommitted) {
    return withUncommitted(toArray(uncommitted));
  }

  public @NonNull GitStatus withUntracked(String... untracked) {
    return with(getUntracked()::add, untracked);
  }

  public @NonNull GitStatus withUntracked(Iterable<String> untracked) {
    return withUntracked(toArray(untracked));
  }
}
