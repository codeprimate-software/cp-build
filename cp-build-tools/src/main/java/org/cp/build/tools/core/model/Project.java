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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;

import org.cp.build.tools.core.model.maven.MavenProject;
import org.cp.build.tools.core.support.Utils;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract Data Type (ADT) used to model a [Codeprimate] software project.
 *
 * @author John Blum
 * @since 2.0.0
 */
@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode(of = { "directory", "name" })
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class Project {

  public static Project from(File file) {

    assertThat(file)
      .describedAs("[%s] must be a file", file)
      .isFile();

    if (MavenProject.isMavenPomPresent(file)) {
      return MavenProject.fromMavenPom(file);
    }

    throw new IllegalArgumentException(String.format("Cannot create Project from file [%s]", file));
  }

  public static Project from(String name) {

    assertThat(name)
      .describedAs("Name [%s] of Project is required", name)
      .isNotBlank();

    return new Project(name);
  }

  private Artifact artifact;

  private File directory;

  @lombok.NonNull
  private final String name;

  private String description;

  private URI issueTracker;
  private URI sourceRepository;

  private Version version;

  public boolean isGradle() {
    return false;
  }

  public boolean isMaven() {
    return false;
  }

  public Project describedAs(String description) {
    setDescription(description);
    return this;
  }

  public Project inWorkingDirectory(File directory) {
    assertThat(directory).describedAs("File [%s] must be a directory", directory).isDirectory();
    setDirectory(directory);
    return this;
  }

  public Project producingArtifact(Artifact artifact) {
    setArtifact(artifact);
    return this;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Artifact {

    protected static final String ARTIFACT_COMPONENT_SEPARATOR = ":";

    public static Artifact from(Project project, String id) {

      assertThat(project).describedAs("Project is required").isNotNull();
      assertThat(id).describedAs("Artifact ID [%s] is required", id).isNotBlank();

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

    public Artifact withGroupId(String groupId) {
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

    public static Version of(int major, int minor) {
      return new Version(major, minor, DEFAULT_MAINTENANCE_VERSION);
    }

    public static Version of(int major, int minor, int maintenance) {
      return new Version(major, minor, maintenance);
    }

    public static Version parse(String version) {

      assertThat(version)
        .describedAs("Version string [%s] to parse is required")
        .isNotBlank();

      int versionQualifierIndex = version.indexOf(VERSION_QUALIFIER_SEPARATOR);

      String versionQualifier = null;

      if (versionQualifierIndex > -1) {
        versionQualifier = version.substring(versionQualifierIndex + 1);
        version = version.substring(0, versionQualifierIndex);
      }

      String[] versionNumbers = version.split(VERSION_NUMBER_SEPARATOR);

      assertThat(versionNumbers)
        .describedAs("Version string [%s] must consist of at least a major and minor version numbers")
        .hasSizeGreaterThanOrEqualTo(2);

      switch (versionNumbers.length) {
        case 2 -> {
          return new Version(Integer.parseInt(versionNumbers[0]), Integer.parseInt(versionNumbers[1]),
            DEFAULT_MAINTENANCE_VERSION).withQualifier(versionQualifier);
        }
        case 3 -> {
          return new Version(Integer.parseInt(versionNumbers[0]), Integer.parseInt(versionNumbers[1]),
            Integer.parseInt(versionNumbers[2]));
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

    public Version withQualifier(String qualifier) {
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
