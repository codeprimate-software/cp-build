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
package org.cp.build.tools.core.model;

import java.io.File;
import java.net.URI;

import org.cp.build.tools.core.support.Utils;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.maven.model.MavenProject;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract Data Type (ADT) used to model a [Codeprimate] software project.
 *
 * @author John Blum
 * @see java.io.File
 * @see java.lang.Comparable
 * @see java.net.URL
 * @since 2.0.0
 */
@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode(of = { "directory", "name" })
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class Project implements Comparable<Project> {

  /**
   * Factory method used to construct a new {@link Project} from the given {@link File}.
   * <p/>
   * In this case, the {@link File} may either refer to the {@link File working directory} of the {@link Project}
   * or a {@literal Maven POM} specifying the metadata for the {@link Project}.
   *
   * @param file {@link File} referring to the {@link File working directory} containing {@link File source files}
   * for the {@link Project} or a {@literal Maven POM} specifying the {@link Project} makeup and metadata.
   * @return a new {@link Project}.
   * @throws IllegalArgumentException if a {@link Project} cannot be created from the given {@link File}.
   * @see org.cp.build.tools.maven.model.MavenProject
   * @see java.io.File
   */
  public static @NonNull Project from(@NonNull File file) {

    if (MavenProject.isMavenPomPresent(file)) {
      return MavenProject.fromMavenPom(file);
    }

    throw new IllegalArgumentException(String.format("Cannot create Project from file [%s]", file));
  }

  /**
   * Factory method used to construct a new {@link Project} with the given, required {@link String name}.
   *
   * @param name {@link String} specifying the {@literal name} of the new {@link Project}.
   * @return a new {@link Project} with the given {@link String name}.
   * @throws IllegalArgumentException if the given {@link String name} is {@literal null} or {@literal empty}.
   */
  public static @NonNull Project from(@NonNull String name) {

    Assert.hasText(name, () -> String.format("Name [%s] of Project is required", name));

    return new Project(name);
  }

  private Artifact artifact;

  private CommitHistory commitHistory;

  private File directory;

  @lombok.NonNull
  private final String name;

  private String description;

  private URI issueTracker;
  private URI sourceRepository;

  private Version version;

  /**
   * Determines whether this is a {@literal Gradle} {@link Project}.
   *
   * @return a boolean value indicating whether this is a {@literal Gradle} {@link Project}.
   * @see #isMaven()
   */
  public boolean isGradle() {
    return false;
  }

  /**
   * Determines whether this is a {@literal Maven} {@link Project}.
   *
   * @return a boolean value indicating whether this is a {@literal Maven} {@link Project}.
   * @see #isGradle()
   */
  public boolean isMaven() {
    return false;
  }

  /**
   * Gets the {@link CommitHistory revision history} for this {@link Project}.
   *
   * @return the {@link CommitHistory revision history} for this {@link Project}.
   * @see org.cp.build.tools.git.model.CommitHistory
   */
  public @NonNull CommitHistory getCommitHistory() {
    return Utils.get(this.commitHistory, CommitHistory::empty);
  }

  @Override
  public int compareTo(@NonNull Project project) {
    return getName().compareTo(project.getName());
  }

  @SuppressWarnings("unchecked")
  public @NonNull <T extends Project> T buildsArtifact(@Nullable Artifact artifact) {
    setArtifact(artifact);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public @NonNull <T extends Project> T describedAs(@NonNull String description) {
    Assert.hasText(description, () -> String.format("Description for Project [%s] is required", getName()));
    setDescription(description);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public @NonNull <T extends Project> T inWorkingDirectory(@NonNull File directory) {
    setDirectory(Utils.requireObject(directory, "File [%s] must be a directory", directory));
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public @NonNull <T extends Project> T withCommitHistory(@Nullable CommitHistory commitHistory) {
    setCommitHistory(commitHistory);
    return (T) this;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Artifact {

    protected static final String ARTIFACT_COMPONENT_SEPARATOR = ":";

    public static @NonNull Artifact from(@NonNull Project project, @NonNull String id) {

      Assert.notNull(project, "Project is required");
      Assert.hasText(id, () -> String.format("Artifact ID [%s] is required", id));

      return new Artifact(project, id);
    }

    @Setter(AccessLevel.PROTECTED)
    private String groupId;

    @lombok.NonNull
    private final Project project;

    @lombok.NonNull
    private final String id;

    public boolean isGroupIdSet() {
      String groupId = getGroupId();
      return !(groupId == null || groupId.isBlank());
    }

    public Version getVersion() {
      return getProject().getVersion();
    }

    public @NonNull Artifact withGroupId(@Nullable String groupId) {
      setGroupId(groupId);
      return this;
    }

    @Override
    public String toString() {

      return Utils.nullSafeTrimmedString(getGroupId())
        .concat(isGroupIdSet() ? ARTIFACT_COMPONENT_SEPARATOR : Utils.EMPTY_STRING)
        .concat(getId())
        .concat(ARTIFACT_COMPONENT_SEPARATOR)
        .concat(getVersion().toString());
    }
  }

  @Getter
  public static class Version implements Cloneable, Comparable<Version> {

    protected static final int DEFAULT_MAJOR_VERSION = 0;
    protected static final int DEFAULT_MINOR_VERSION = 0;
    protected static final int DEFAULT_MAINTENANCE_VERSION = 0;

    protected static final String MILESTONE = "M";
    protected static final String SNAPSHOT = "snapshot";
    protected static final String VERSION_NUMBER_SEPARATOR = "\\.";
    protected static final String VERSION_QUALIFIER_SEPARATOR = "-";

    public static @NonNull Version of(int major, int minor) {
      return new Version(major, minor, DEFAULT_MAINTENANCE_VERSION);
    }

    public static @NonNull Version of(int major, int minor, int maintenance) {
      return new Version(major, minor, maintenance);
    }

    public static @NonNull Version parse(@NonNull String version) {

      String originalVersion = version;

      Assert.hasText(version, () -> String.format("Version string [%s] is required", originalVersion));

      int versionQualifierIndex = version.indexOf(VERSION_QUALIFIER_SEPARATOR);

      String versionQualifier = null;

      if (versionQualifierIndex > -1) {
        versionQualifier = version.substring(versionQualifierIndex + 1);
        version = version.substring(0, versionQualifierIndex);
      }

      String[] versionNumbers = version.split(VERSION_NUMBER_SEPARATOR);

      Assert.isTrue(versionNumbers.length >= 2,
        () -> String.format("Version string [%s] must consist of at least major and minor version numbers",
          originalVersion));

      switch (versionNumbers.length) {
        case 2 -> {
          return new Version(Integer.parseInt(versionNumbers[0]), Integer.parseInt(versionNumbers[1]),
            DEFAULT_MAINTENANCE_VERSION).withQualifier(versionQualifier);
        }
        case 3 -> {
          return new Version(Integer.parseInt(versionNumbers[0]), Integer.parseInt(versionNumbers[1]),
            Integer.parseInt(versionNumbers[2])).withQualifier(versionQualifier);
        }
      }

      throw new IllegalArgumentException(String.format("Version string [%s] to parse is not valid", version));
    }

    private final int major;
    private final int minor;
    private final int maintenance;

    @Setter(AccessLevel.PROTECTED)
    private String qualifier;

    private Version(int major, int minor, int maintenance) {

      this.major = Math.max(major, DEFAULT_MAJOR_VERSION);
      this.minor = Math.max(minor, DEFAULT_MINOR_VERSION);
      this.maintenance = Math.max(maintenance, DEFAULT_MAINTENANCE_VERSION);
    }

    public boolean isQualifierPresent() {
      String qualifier = getQualifier();
      return !(qualifier == null || qualifier.isBlank());
    }

    public boolean isMilestone() {
      return String.valueOf(getQualifier()).toUpperCase().startsWith(MILESTONE);
    }

    public boolean isRelease() {
      return !(isSnapshot() || isMilestone());
    }

    public boolean isSnapshot() {
      return SNAPSHOT.equalsIgnoreCase(getQualifier());
    }

    public @NonNull Version withQualifier(@Nullable String qualifier) {
      setQualifier(qualifier);
      return this;
    }

    @Override
    @SuppressWarnings("all")
    public Object clone() {
      return Version.of(this.getMajor(), this.getMinor(), this.getMaintenance());
    }

    @Override
    public int compareTo(Project.Version that) {

      int result = Integer.compare(this.getMajor(), that.getMajor());

      result = result != 0 ? result : Integer.compare(this.getMinor(), that.getMinor());
      result = result != 0 ? result : Integer.compare(this.getMaintenance(), that.getMaintenance());

      return result;
    }

    @Override
    public String toString() {

      String versionString = String.format("%1$d.%2$d.%3$d", getMajor(), getMinor(), getMaintenance());

      return isQualifierPresent()
        ? versionString.concat(VERSION_QUALIFIER_SEPARATOR).concat(getQualifier())
        : versionString;
    }
  }
}
