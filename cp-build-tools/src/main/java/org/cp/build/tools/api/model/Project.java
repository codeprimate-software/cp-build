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
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.cp.build.tools.api.support.ComparableComparator;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.maven.model.MavenProject;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract Data Type (ADT) used to model a [Codeprimate] software project.
 *
 * @author John Blum
 * @see java.lang.Comparable
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
   * <p>
   * In this case, the {@link File} may either refer to the {@link File working directory} of the {@link Project}
   * or a {@link File Maven POM} specifying the metadata for the {@link Project}.
   *
   * @param file {@link File} referring to the {@link File working directory} containing {@link File source files}
   * for the {@link Project} or a {@link File Maven POM} specifying the {@link Project} metadata.
   * @return a new {@link Project} from the given {@link File}.
   * @throws IllegalArgumentException if a {@link Project} cannot be created from the given {@link File}.
   * @see org.cp.build.tools.maven.model.MavenProject
   * @see java.io.File
   */
  public static Project from(File file) {

    if (MavenProject.isMavenPomPresent(file)) {
      return MavenProject.fromMavenPom(file);
    }

    throw new IllegalArgumentException("Cannot create project from file [%s]".formatted(file));
  }

  /**
   * Factory method used to construct a new {@link Project} with the given, required {@link String name}.
   *
   * @param name {@link String} specifying the {@literal name} for the new {@link Project}.
   * @return a new {@link Project} with the given {@link String name}.
   * @throws IllegalArgumentException if the given {@link String name} is {@literal null} or {@literal empty}.
   */
  public static Project from(String name) {

    Assert.hasText(name, () -> "Name [%s] for project is required".formatted(name));

    return new Project(name);
  }

  private Artifact artifact;

  private CommitHistory commitHistory;

  private final Developers developers = new Developers();

  private File directory;

  private final Licenses licenses = new Licenses();

  private final String name;

  private String description;

  private Organization organization;

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
   * Gets the {@link File directory location} in the filesystem for this {@link Project}.
   *
   * @return the {@link File directory location} in the filesystem for this {@link Project}.
   * @see java.io.File
   */
  @SuppressWarnings("all")
  public File getDirectory() {
    return directory;
  }

  /**
   * Gets the {@link String name} of this {@link Project}.
   *
   * @return the {@link String name} of this {@link Project}.
   */
  @SuppressWarnings("all")
  public String getName() {
    return this.name;
  }

  /**
   * Builder method used to configure the {@link Artifact} built by this {@link Project}.
   *
   * @param <T> {@link Class concrete type} of {@link Project}.
   * @param artifact {@link Artifact} produced by this {@link Project}.
   * @return this {@link Project}.
   * @see org.cp.build.tools.api.model.Project.Artifact
   */
  @SuppressWarnings("unchecked")
  public <T extends Project> T buildsArtifact( Artifact artifact) {
    setArtifact(artifact);
    return (T) this;
  }

  /**
   * Builder method used to configure a {@link String description} of this {@link Project}.
   *
   * @param <T> {@link Class concrete type} of {@link Project}.
   * @param description {@link String} used to describe this {@link Project};
   * must not be {@literal null} or {@literal blank}.
   * @return this {@link Project}.
   * @throws IllegalArgumentException if the {@link String description} is {@literal null} or {@literal blank}.
   */
  @SuppressWarnings("unchecked")
  public <T extends Project> T describedAs(String description) {
    Assert.hasText(description, () -> "Description for Project [%s] is required".formatted(getName()));
    setDescription(description);
    return (T) this;
  }

  /**
   * Builder method used to configure {@link Developers} who developed this {@link Project}.
   *
   * @param <T> {@link Class concrete type} of {@link Project}.
   * @param developer {@link Developer} who engineered and developed this {@link Project}.
   * @return this {@link Project}.
   * @see org.cp.build.tools.api.model.Project.Developers
   * @see org.cp.build.tools.api.model.Project.Developer
   */
  @SuppressWarnings("unchecked")
  public <T extends Project> T developedBy(Developer developer) {
    getDevelopers().add(developer);
    return (T) this;
  }

  /**
   * Builder method used to configure the {@link File local working directory} containing the source files
   * for this {@link Project}.
   *
   * @param <T> {@link Class concrete type} of {@link Project}.
   * @param directory {@link File} referring to the {@literal working directory} containing the source files
   * for this {@link Project}; must be an existing, valid {@link File#isDirectory() directory}.
   * @return this {@link Project}.
   * @throws IllegalArgumentException if the {@link File} is not an existing,
   * valid {@link File#isDirectory() directory}.
   * @see java.io.File
   */
  @SuppressWarnings("unchecked")
  public <T extends Project> T inDirectory(File directory) {
    Assert.isTrue(Utils.nullSafeIsDirectory(directory), "[%s] must be an existing, valid directory");
    setDirectory(directory);
    return (T) this;
  }

  /**
   * Builder method used to configure the {@link CommitHistory} for this {@link Project}.
   *
   * @param <T> {@link Class concrete type} of {@link Project}.
   * @param commitHistory {@link CommitHistory} capturing all the {@literal [git] commits}
   * applied to this {@link Project} in reverse chronological order.
   * @return this {@link Project}.
   * @see org.cp.build.tools.git.model.CommitHistory
   */
  @SuppressWarnings("unchecked")
  public <T extends Project> T withCommitHistory( CommitHistory commitHistory) {
    setCommitHistory(commitHistory);
    return (T) this;
  }

  /**
   * Builder method used to configure {@link Licenses} used to {@literal copyright} this {@link Project}.
   *
   * @param <T> {@link Class concrete type} of {@link Project}.
   * @param license {@link License} used to {@literal copyright} this {@link Project}.
   * @return this {@link Project}.
   * @see org.cp.build.tools.api.model.Project.Licenses
   * @see org.cp.build.tools.api.model.Project.License
   */
  @SuppressWarnings("unchecked")
  public <T extends Project> T withLicense(License license) {
    getLicenses().add(license);
    return (T) this;
  }

  /**
   * Builder method used to configure the {@link Organization} governing this {@link Project}.
   *
   * @param <T> {@link Class concrete type} of {@link Project}.
   * @param organization {@link Organization} that governs and owns this {@link Project}.
   * @return this {@link Project}.
   * @see org.cp.build.tools.api.model.Project.Organization
   */
  @SuppressWarnings("unchecked")
  public <T extends Project> T withOrganization( Organization organization) {
    setOrganization(organization);
    return (T) this;
  }

  /**
   * Compares and sorts a collection of {@link Project Projects} by {@link #name} in ascending order.
   *
   * @param project {@link Project} compared for order with this {@link Project}; must not be null.
   * @return an {@link Integer value} specifying relative ordering between this {@link Project}
   * and the given {@link Project}.
   * @see #name
   */
  @Override
  public int compareTo(Project project) {
    return getName().compareTo(project.getName());
  }

  /**
   * Returns the {@link #name} of this {@link Project}.
   *
   * @return the {@link #name} of this {@link Project}.
   * @see #name
   */
  @Override
  public String toString() {
    return getName();
  }

  /**
   * Abstract Data Type (ADT) modeling a {@literal software artifact}.
   *
   * @author John Blum
   * @see java.lang.Comparable
   */
  @Getter
  public static class Artifact implements Comparable<Artifact> {

    protected static final String ARTIFACT_COMPONENT_SEPARATOR = ":";

    public static Artifact from(Project project, String id) {
      return new Artifact(project, id);
    }

    @Setter(AccessLevel.PROTECTED)
    private String groupId;

    private final Project project;

    private final String id;

    protected Artifact(Project project, String id) {

      Assert.hasText(id, () -> "Artifact ID [%s] is required".formatted(id));

      this.project = Utils.requireObject(project, "Project is required");
      this.id = id;
    }

    public boolean isGroupIdSet() {
      String groupId = getGroupId();
      return !(groupId == null || groupId.isBlank());
    }

    public boolean isVersionSet() {
      return getVersion() != null;
    }

    public  Version getVersion() {
      return getProject().getVersion();
    }

    public Artifact withGroupId( String groupId) {
      setGroupId(groupId);
      return this;
    }

    @Override
    public int compareTo(Artifact artifact) {

      int result = Utils.getInt(ComparableComparator.INSTANCE.compare(this.getGroupId(), artifact.getGroupId()),
        () -> this.getId().compareTo(artifact.getId()));

      return Utils.getInt(result,
        () -> ComparableComparator.INSTANCE.compare(this.getVersion(), artifact.getVersion()));

    }

    @Override
    public boolean equals(Object obj) {

      if (this == obj) {
        return true;
      }

      if (!(obj instanceof Artifact that)) {
        return false;
      }

      return this.getId().equals(that.getId())
        && Objects.equals(this.getGroupId(), that.getGroupId())
        && Objects.equals(this.getVersion(), that.getVersion());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getGroupId(), getId(), getVersion());
    }

    @Override
    public String toString() {

      return Utils.nullSafeTrimmedString(getGroupId())
        .concat(isGroupIdSet() ? ARTIFACT_COMPONENT_SEPARATOR : Utils.EMPTY_STRING)
        .concat(getId())
        .concat(isVersionSet() ? ARTIFACT_COMPONENT_SEPARATOR : Utils.EMPTY_STRING)
        .concat(Utils.nullSafeToString(getVersion()));
    }
  }

  /**
   * Abstract Data Type (ADT) modeling a {@literal software license}.
   *
   * @author John Blum
   */
  @Getter
  @EqualsAndHashCode(of = "name")
  @RequiredArgsConstructor(staticName = "from")
  public static class License {

    private final String name;

    @Setter(AccessLevel.PROTECTED)
    private URI uri;

    public License withUri( URI uri) {
      setUri(uri);
      return this;
    }

    @Override
    public String toString() {

      String uriString = Optional.ofNullable(getUri())
        .map(Object::toString)
        .map(" (%s)"::formatted)
        .orElse(Utils.EMPTY_STRING);

      return String.valueOf(getName()).concat(uriString);
    }
  }

  /**
   * Abstract Data Type (ADT) modeling a collection of {@literal software licenses}.
   *
   * @author John Blum
   * @see java.lang.Iterable
   * @see License
   */
  @Getter(AccessLevel.PROTECTED)
  public static class Licenses implements Iterable<License> {

    private final Set<License> licenses = new HashSet<>();

    @SuppressWarnings("all")
    public boolean add(License license) {
      return license != null && getLicenses().add(license);
    }

    public boolean contains( License license) {
      return license != null && getLicenses().contains(license);
    }

    @Override
    @SuppressWarnings("all")
    public Iterator<License> iterator() {
      return Collections.unmodifiableSet(this.licenses).iterator();
    }

    public Stream<License> stream() {
      return Utils.stream(this);
    }

    @Override
    public String toString() {
      return getLicenses().toString();
    }
  }

  /**
   * Abstract Data Type (ADT) modeling a {@literal software developer}.
   *
   * @author John Blum
   * @see java.lang.Comparable
   */
  @Getter
  @Setter(AccessLevel.PROTECTED)
  @EqualsAndHashCode(of = "name")
  @RequiredArgsConstructor(staticName = "as")
  public static class Developer implements Comparable<Developer> {

    private Organization organization;

    private final String name;

    private String emailAddress;
    private String id;

    private URI uri;

    public Developer identifiedBy( String id) {
      setId(id);
      return this;
    }

    public Developer withEmailAddress( String emailAddress) {
      setEmailAddress(emailAddress);
      return this;
    }

    public Developer withOrganization( Organization organization) {
      setOrganization(organization);
      return this;
    }

    public Developer withUri( URI uri) {
      setUri(uri);
      return this;
    }

    @Override
    public int compareTo(Developer developer) {
      return this.getName().compareTo(developer.getName());
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  /**
   * Abstract Data Type (ADT) modeling a collection of {@literal software developers}.
   *
   * @author John Blum
   * @see java.lang.Iterable
   * @see Developer
   */
  @Getter(AccessLevel.PROTECTED)
  public static class Developers implements Iterable<Developer> {

    private final Set<Developer> developers = new HashSet<>();

    @SuppressWarnings("all")
    public boolean add(Developer developer) {
      return developer != null && getDevelopers().add(developer);
    }

    public boolean contains( Developer developer) {
      return developer != null && getDevelopers().contains(developer);
    }

    @Override
    @SuppressWarnings("all")
    public Iterator<Developer> iterator() {
      return Collections.unmodifiableSet(this.developers).iterator();
    }

    public Stream<Developer> stream() {
      return Utils.stream(this);
    }

    @Override
    public String toString() {
      return getDevelopers().stream()
        .sorted().toList().toString();
    }
  }

  /**
   * Abstract Data Type (ADT) modeling a {@literal software orgnization}.
   *
   * @author John Blum
   * @see java.lang.Comparable
   */
  @Getter
  @EqualsAndHashCode(of = "name")
  @RequiredArgsConstructor(staticName = "as")
  public static class Organization implements Comparable<Organization> {

    private final String name;

    @Setter(AccessLevel.PROTECTED)
    private URI uri;

    public Organization withUri( URI uri) {
      setUri(uri);
      return this;
    }

    @Override
    public int compareTo(Organization organization) {
      return this.getName().compareTo(organization.getName());
    }

    @Override
    public String toString() {

      String uriString = Optional.ofNullable(getUri())
        .map(Object::toString)
        .map(" (%s)"::formatted)
        .orElse(Utils.EMPTY_STRING);

      return String.valueOf(getName()).concat(uriString);
    }
  }

  /**
   * Abstract Data Type (ADT) modeling a {@literal software version}.
   *
   * @author John Blum
   * @see java.lang.Comparable
   */
  @Getter
  public static class Version implements Comparable<Version> {

    protected static final int DEFAULT_MAJOR_VERSION = 0;
    protected static final int DEFAULT_MINOR_VERSION = 0;
    protected static final int DEFAULT_MAINTENANCE_VERSION = 0;

    protected static final String MILESTONE = "M";
    protected static final String RELEASE_CANDIDATE = "RC";
    protected static final String SNAPSHOT = "snapshot";
    protected static final String VERSION_NUMBER_SEPARATOR = "\\.";
    protected static final String VERSION_QUALIFIER_SEPARATOR = "-";

    private static final String VERSION_TO_STRING = "%1$d.%2$d.%3$d";

    public static Version of(int major, int minor) {
      return new Version(major, minor, DEFAULT_MAINTENANCE_VERSION);
    }

    public static Version of(int major, int minor, int maintenance) {
      return new Version(major, minor, maintenance);
    }

    public static Version parse(String version) {

      String originalVersion = version;

      Assert.hasText(version, () -> "Version string [%s] is required".formatted(originalVersion));

      int versionQualifierIndex = version.indexOf(VERSION_QUALIFIER_SEPARATOR);

      String versionQualifier = null;

      if (versionQualifierIndex > -1) {
        versionQualifier = version.substring(versionQualifierIndex + 1);
        version = version.substring(0, versionQualifierIndex);
      }

      String[] versionNumbers = version.split(VERSION_NUMBER_SEPARATOR);

      Assert.isTrue(versionNumbers.length >= 2,
        () -> "Version string [%s] must consist of at least major and minor version numbers"
          .formatted(originalVersion));

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

      throw new IllegalArgumentException("Version string [%s] to parse is not valid".formatted(version));
    }

    private final int major;
    private final int minor;
    private final int maintenance;

    @Setter(AccessLevel.PROTECTED)
    private String qualifier;

    protected Version(int major, int minor, int maintenance) {

      this.major = Math.max(major, DEFAULT_MAJOR_VERSION);
      this.minor = Math.max(minor, DEFAULT_MINOR_VERSION);
      this.maintenance = Math.max(maintenance, DEFAULT_MAINTENANCE_VERSION);
    }

    public boolean isQualifierPresent() {
      return !Utils.nullSafeTrimmedString(getQualifier()).isEmpty();
    }

    public boolean isMilestone() {
      return String.valueOf(getQualifier()).toUpperCase().startsWith(MILESTONE);
    }

    public boolean isRelease() {
      return !(isSnapshot() || isMilestone() || isReleaseCandidate());
    }

    public boolean isReleaseCandidate() {
      return String.valueOf(getQualifier()).toUpperCase().startsWith(RELEASE_CANDIDATE);
    }

    public boolean isSnapshot() {
      return SNAPSHOT.equalsIgnoreCase(getQualifier());
    }

    public Version withQualifier( String qualifier) {
      setQualifier(qualifier);
      return this;
    }

    @Override
    public int compareTo(Project.Version version) {

      // Compare Major, Minor & Maintenance Version Numbers
      int result = Utils.getInt(Integer.compare(this.getMajor(), version.getMajor()),
        () -> Utils.getInt(Integer.compare(this.getMinor(), version.getMinor()),
          () -> Integer.compare(this.getMaintenance(), version.getMaintenance())));

      // Compare Qualifier
      result = Utils.getInt(result,
        () -> Utils.getInt(Integer.compare(quantifyQualifier(this), quantifyQualifier(version)),
          () -> Integer.compare(resolveQualifierNumber(this), resolveQualifierNumber(version))));

      return Utils.negate(result);
    }

    private int quantifyQualifier(Version version) {
      return quantifyQualifier(version.getQualifier());
    }

    private int quantifyQualifier( String qualifier) {

      String nonNullQualifier = Utils.nullSafeTrimmedString(qualifier);

      return nonNullQualifier.isEmpty() ? 4
        : nonNullQualifier.startsWith(RELEASE_CANDIDATE) ? 3
        : nonNullQualifier.startsWith(MILESTONE) ? 2
        : nonNullQualifier.startsWith(SNAPSHOT) ? 1
        : 0;
    }

    private int resolveQualifierNumber(Version version) {
      return resolveQualifierNumber(version.getQualifier());
    }

    @SuppressWarnings("all")
    private int resolveQualifierNumber(String qualifier) {

      String number = "";

      for (char x : Utils.nullSafeTrimmedString(qualifier).toCharArray()) {
        if (Character.isDigit(x)) {
          number += x;
        }
      }

      return StringUtils.hasText(number) ? Integer.parseInt(number) : 0;
    }

    @Override
    public boolean equals(Object obj) {

      if (this == obj) {
        return true;
      }

      if (!(obj instanceof Version that)) {
        return false;
      }

      return this.getMajor() == that.getMajor()
        && this.getMinor() == that.getMinor()
        && this.getMaintenance() == that.getMaintenance()
        && Objects.equals(this.getQualifier(), that.getQualifier());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getMajor(), getMinor(), getMaintenance(), getQualifier());
    }

    @Override
    public String toString() {

      String versionString = VERSION_TO_STRING.formatted(getMajor(), getMinor(), getMaintenance());

      return isQualifierPresent()
        ? versionString.concat(VERSION_QUALIFIER_SEPARATOR).concat(getQualifier())
        : versionString;
    }
  }
}
