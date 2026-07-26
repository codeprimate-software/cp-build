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
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.api.time.TimePeriods;
import org.springframework.util.Assert;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract Data Type (ADT) modeling a {@link File source file} under source control, such as {@literal git}.
 *
 * @author John Blum
 * @see java.io.File
 * @see java.lang.Comparable
 * @see java.lang.Iterable
 * @since 0.1.0
 */
@Getter
@SuppressWarnings("unused")
public class SourceFile implements Comparable<SourceFile>, Iterable<SourceFile.Revision> {

  protected static final boolean DEFAULT_SKIP_BLANK_LINES = false;

  protected static final String DOT_SEPARATOR = ".";
  protected static final String PACKAGE_SEPARATOR = DOT_SEPARATOR;
  protected static final String PATH_SEPARATOR = File.separator;
  protected static final String SOURCE_MAIN = "src%smain".formatted(File.separator);
  protected static final String SOURCE_TEST = "src%stest".formatted(File.separator);

  public static SourceFile from(File file) {
    return new SourceFile(file);
  }

  public static SourceFile from(File file,  Project project) {
    return new SourceFile(file, project);
  }

  private final AtomicReference<Long> lineCount = new AtomicReference<>(0L);
  private final AtomicReference<String> relativePath = new AtomicReference<>(null);
  private final AtomicReference<Type> type = new AtomicReference<>(null);

  private final File file;

  private final Project project;

  @Getter(AccessLevel.PROTECTED)
  private final Set<Revision> revisions = new TreeSet<>();

  public SourceFile(File file) {
    this(file, null);
  }

  public SourceFile(File file,  Project project) {

    Assert.notNull(file, "File is required");
    Assert.isTrue(file.isFile(), () -> "File [%s] must exist".formatted(file));

    this.file = file;
    this.project = project;
  }

  public Set<Author> getAuthors() {
    return stream()
      .map(Revision::getAuthor)
      .collect(Collectors.toSet());
  }

  @SuppressWarnings("all")
  public File getFile() {
    return this.file;
  }

  public Optional<Revision> getFirstRevision() {
    return stream().findFirst();
  }

  public Optional<LocalDateTime> getFirstRevisionDateTime() {
    return getFirstRevision().map(Revision::getDateTime);
  }

  public Instant getLastModified() {
    return Instant.ofEpochMilli(getFile().lastModified());
  }

  public ZonedDateTime getLastModifiedDateTime() {
    return ZonedDateTime.ofInstant(getLastModified(), ZoneId.systemDefault());
  }

  public Optional<Revision> getLastRevision() {
    List<Revision> revisions = stream().toList();
    Revision last = revisions.isEmpty() ? null : revisions.getLast();
    return Optional.ofNullable(last);
  }

  public Optional<LocalDateTime> getLastRevisionDateTime() {
    return getLastRevision().map(Revision::getDateTime);
  }

  @SuppressWarnings("all")
  public String getName() {
    String filename = getFile().getName();
    int index = filename.indexOf(DOT_SEPARATOR);
    String resolvedFilename = index > -1 ? filename.substring(0, index) : filename;
    return resolvedFilename;
  }

  @SuppressWarnings("all")
  public String getPackage() {
    String relativePath = getRelativePath();
    int index = relativePath.lastIndexOf(PATH_SEPARATOR);
    String packageName = relativePath.substring(0, index).replaceAll(PATH_SEPARATOR, PACKAGE_SEPARATOR);
    return packageName;
  }

  protected Optional<Project> getProject() {
    return Optional.ofNullable(this.project);
  }

  public String getRelativePath() {

    File file = getFile();

    Assert.isTrue(file.isFile(), "File [%s] no longer exists");

    String pathname = file.getAbsolutePath();
    int index = pathname.indexOf(SOURCE_MAIN);

    index = index > -1 ? index : pathname.indexOf(SOURCE_TEST);

    index = index > -1 ? index : getProject()
      .map(Project::getDirectory)
      .map(File::getAbsolutePath)
      .map(String::length)
      .orElse(index);

    return index > -1
      ? pathname.substring(index + SOURCE_MAIN.length())
      : pathname;
  }

  public Optional<Revision> getRevision(String id) {
    return stream().filter(revision -> revision.getId().equals(id)).findFirst();
  }

  public int getRevisionCount() {
    return getRevisions().size();
  }

  public Set<String> getRevisionIds() {
    return stream().map(Revision::getId).collect(Collectors.toSet());
  }

  public Set<Revision> getRevisions(Author author) {
    return stream().filter(revision -> revision.getAuthor().equals(author)).collect(Collectors.toSet());
  }

  // Find (Query) by Author name or email address
  public Set<Revision> getRevisions(String author) {

    return stream().filter(revision -> {

        Author revisionAuthor = revision.getAuthor();

        return revisionAuthor.getName().equalsIgnoreCase(author)
          || revisionAuthor.getEmailAddress().equalsIgnoreCase(author);
      })
      .collect(Collectors.toSet());
  }

  @SuppressWarnings("all")
  public Set<Revision> getRevisions(TimePeriods timePeriods) {

    return timePeriods != null
      ? stream().filter(revision -> timePeriods.isDuring(revision.getDate())).collect(Collectors.toSet())
      : Collections.emptySet();
  }

  public Type getType() {
    return this.type.updateAndGet(it -> it != null ? it : WellKnownTypes.from(getFile()));
  }

  @Override
  @SuppressWarnings("all")
  public Iterator<Revision> iterator() {
    return Collections.unmodifiableSet(getRevisions()).iterator();
  }

  public long lineCount() {
    return lineCount(DEFAULT_SKIP_BLANK_LINES);
  }

  public long lineCount(boolean skipBlankLines) {
    return this.lineCount.updateAndGet(count -> count != null && count > 0L ? count : countLines(skipBlankLines));
  }

  @SuppressWarnings("all")
  private long countLines(boolean skipBlankLines) {

    File file = getFile();

    Assert.isTrue(file.isFile(), () -> "File [%s] must exist".formatted(file.getAbsolutePath()));

    int emptyLineCount = 0;

    try {
      try (LineNumberReader fileReader = new LineNumberReader(new FileReader(file))) {
        for (String line = fileReader.readLine(); line != null; line = fileReader.readLine()) {
          if (line.isBlank()) {
            emptyLineCount++;
          }
        }

        int diff = skipBlankLines ? emptyLineCount : 0;
        int lineCount = fileReader.getLineNumber() - diff;

        return lineCount;
      }
    }
    catch (IOException cause) {
      throw new IllegalStateException("Failed to count the number of lines in file [%s]".formatted(file), cause);
    }
  }


  public long size() {
    return getFile().length();
  }

  public Stream<Revision> stream() {
    return Utils.stream(this);
  }

  public boolean wasModifiedBy(Author author) {
    return !getRevisions(author).isEmpty();
  }

  // Evaluate by Author name or email address
  public boolean wasModifiedBy(String author) {
    return !getRevisions(author).isEmpty();
  }

  public boolean wasModifiedDuring(TimePeriods timePeriods) {
    return !getRevisions(timePeriods).isEmpty();
  }

  public SourceFile withRevision(Revision revision) {
    this.revisions.add(Objects.requireNonNull(revision, "Revision is required"));
    return this;
  }

  @Override
  public int compareTo(SourceFile that) {
    return this.getFile().compareTo(that.getFile());
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof SourceFile that)) {
      return false;
    }

    return this.getFile().equals(that.getFile());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFile());
  }

  @Override
  public String toString() {
    return getFile().getAbsolutePath();
  }

  /**
   * Abstract Data Type (ADT) modeling the {@link SourceFile} {@literal Author}.
   *
   * @author John Blum
   * @see java.lang.Comparable
   */
  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor(staticName = "as")
  public static class Author implements Comparable<Author> {

    protected static final String AUTHOR_TO_STRING = "%1$s <%2$s>";

    private final String name;

    @Setter(AccessLevel.PROTECTED)
    private String emailAddress;

    public Author withEmailAddress( String emailAddress) {
      setEmailAddress(emailAddress);
      return this;
    }

    @Override
    public int compareTo(Author that) {
      return this.getName().compareTo(that.getName());
    }

    @Override
    public String toString() {
      return AUTHOR_TO_STRING.formatted(getName(), getEmailAddress());
    }
  }

  /**
   * Abstract Data Type (ADT) modeling the {@link SourceFile} {@literal revision},
   *
   * @author John Blum
   * @see java.lang.Comparable
   */
  @Getter
  @EqualsAndHashCode(of = "id")
  @RequiredArgsConstructor(staticName = "of")
  public static class Revision implements Comparable<Revision> {

    private final Author author;

    private final LocalDateTime dateTime;

    private final String id;

    public LocalDate getDate() {
      return getDateTime().toLocalDate();
    }

    public LocalTime getTime() {
      return getDateTime().toLocalTime();
    }

    @Override
    public int compareTo(Revision that) {
      return this.getDateTime().compareTo(that.getDateTime());
    }

    @Override
    public String toString() {
      return getId();
    }
  }

  /**
   * {@link Enum Enumeration} of {@link SourceFile} {@link Type types}.
   *
   * @author John Blum
   * @see java.lang.FunctionalInterface
   */
  @FunctionalInterface
  public interface Type {
    String getExtension();
  }

  public enum WellKnownTypes implements Type {

    C("c"),
    C_PLUS_PLUS("c++"),
    GROOVY("groovy"),
    JAVA("java"),
    JAVASCRIPT("js"),
    KOTLIN("kt"),
    PROPERTIES("properties"),
    UNKNOWN("");

    public static WellKnownTypes from(File file) {

      if (file != null) {
        String filename = file.getName();
        int index = (int) Math.max(0, Math.min(filename.lastIndexOf("."), file.length()));
        String extension = filename.substring(index);
        return from(extension);
      }

      return WellKnownTypes.UNKNOWN;
    }

    public static WellKnownTypes from(String extension) {

      return Arrays.stream(values())
        .filter(type -> type.getExtension().equalsIgnoreCase(extension))
        .findFirst()
        .orElse(WellKnownTypes.UNKNOWN);
    }

    private final String extension;

    WellKnownTypes(String extension) {
      Assert.hasText(extension, () -> "Extension [%s] is required".formatted(extension));
      this.extension = extension;
    }

    @Override
    public String getExtension() {
      return this.extension;
    }


    @Override
    public String toString() {
      return getExtension();
    }
  }
}
