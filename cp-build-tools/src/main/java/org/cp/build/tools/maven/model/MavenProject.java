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
package org.cp.build.tools.maven.model;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.maven.support.MavenPomNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * {@link Project} implementation based on Apache Maven.
 *
 * @author John Blum
 * @see java.io.File
 * @see java.net.URI
 * @see org.cp.build.tools.api.model.Project
 * @see org.apache.maven.model.Model
 * @see org.apache.maven.project.MavenProject
 * @since 2.0.0
 */
@Getter(AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class MavenProject extends Project {

  protected static final String POM_XML = "pom.xml";

  protected static final FileFilter POM_XML_FILE_FILTER = file ->
    Utils.nullSafeIsFile(file) && file.getAbsolutePath().endsWith(POM_XML);

  /**
   * Assertion used to require the presence of a {@literal Maven POM} relative to the given {@link File} reference.
   *
   * @param location {@link File} used to locate and resolve the {@literal Maven POM}
   * from which a {@link MavenProject} can be constructed.
   * @return the given {@link File}.
   * @throws MavenPomNotFoundException if a {@literal Maven POM} {@link File} cannot be found
   * relative to the given {@link File}.
   * @see #isMavenPomPresent(File)
   * @see java.io.File
   */
  public static @NonNull File assertMavenPomIsPresent(@NonNull File location) {

    if (!isMavenPomPresent(location)) {

      String message = "File [%s] must refer to a Maven POM file or directory containing a Maven POM file"
        .formatted(location);

      throw new MavenPomNotFoundException(message);
    }

    return location;
  }

  /**
   * Determines whether the given {@link File} is a {@literal Maven pom.xml file}.
   *
   * @param pom {@link File} to evaluate as a {@literal Maven pom.xml file}.
   * @return a boolean value indicating whether the given {@link File} is a {@literal Maven pom.xml file}.
   * @see #isPomXml(File)
   * @see java.io.File
   */
  public static boolean isMavenPom(@Nullable File pom) {
    return isPomXml(pom);
  }

  /**
   * Determine whether the given {@link File} refers to a {@link File#isDirectory() directory} containing a
   * {@link File#isFile() Maven POM file} or is a {@link File#isFile() Maven POM file}.
   *
   * @param location {@link File} to evaluate.
   * @return a boolean value indicating whether the given {@link File} refers to a {@link File#isDirectory() directory}
   * containing a {@link File#isFile() Maven POM file} or is a {@link File#isFile() Maven POM file}.
   * @see #containsPomXml(File)
   * @see #isPomXml(File)
   * @see java.io.File
   */
  public static boolean isMavenPomPresent(@Nullable File location) {
    return containsPomXml(location) || isPomXml(location);
  }

  private static boolean containsPomXml(@Nullable File location) {

    return Utils.nullSafeIsDirectory(location)
      && Arrays.stream(Utils.nullSafeFileArray(location.listFiles(POM_XML_FILE_FILTER)))
        .findFirst()
        .isPresent();
  }

  private static boolean isPomXml(@Nullable File file) {
    return POM_XML_FILE_FILTER.accept(file);
  }

  /**
   * Factory method used to construct a new {@link MavenProject} from the given, required {@link File Maven POM}.
   *
   * @param pom {@link File} referring to a {@literal Maven POM}.
   * @return a new {@link MavenProject} initialized from the given {@link File Maven POM}.
   * @see #assertMavenPomIsPresent(File)
   * @see java.io.File
   */
  public static @NonNull MavenProject fromMavenPom(@NonNull File pom) {

    pom = resolvePomXml(assertMavenPomIsPresent(pom));

    try (FileReader pomFileReader = new FileReader(pom)) {

      Model model = new MavenXpp3Reader().read(pomFileReader);

      org.apache.maven.project.MavenProject mavenProject = new org.apache.maven.project.MavenProject(model);

      return new MavenProject(mavenProject)
        .inDirectory(pom.getParentFile());
    }
    catch (Exception cause) {
      String message = "Failed to read Maven POM from file [%s]".formatted(pom);
      throw new IllegalStateException(message, cause);
    }
  }

  private static @NonNull File resolvePomXml(@NonNull File file) {
    return isPomXml(file) ? file : new File(file, POM_XML);
  }

  private final AtomicReference<Artifact> artifactReference = new AtomicReference<>(null);
  private final AtomicReference<Developers> developersReference = new AtomicReference<>(null);
  private final AtomicReference<Licenses> licensesReference = new AtomicReference<>(null);
  private final AtomicReference<Organization> organizationReference = new AtomicReference<>(null);

  private final org.apache.maven.project.MavenProject mavenProject;

  /**
   * Constructs a new {@link MavenProject} initialize with the given,
   * required {@link org.apache.maven.project.MavenProject}.
   *
   * @param mavenProject {@link org.apache.maven.project.MavenProject} backing this {@link Project}.
   * @see org.apache.maven.project.MavenProject
   */
  private MavenProject(@NonNull org.apache.maven.project.MavenProject mavenProject) {
    super(mavenProject.getName());
    this.mavenProject = mavenProject;
  }

  @Override
  public boolean isMaven() {
    return true;
  }

  @Override
  public @NonNull Artifact getArtifact() {
    return this.artifactReference.updateAndGet(artifact -> artifact != null ? artifact : buildArtifact());
  }

  private @NonNull Artifact buildArtifact() {

    org.apache.maven.project.MavenProject mavenProject = getMavenProject();

    return Artifact.from(this, mavenProject.getArtifactId())
      .withGroupId(mavenProject.getGroupId());
  }

  @Override
  public @NonNull Developers getDevelopers() {
    return this.developersReference.updateAndGet(developers -> developers != null ? developers : buildDevelopers());
  }

  private @NonNull Developers buildDevelopers() {

    org.apache.maven.project.MavenProject mavenProject = getMavenProject();

    Developers developers = new Developers();

    mavenProject.getDevelopers().stream()
      .filter(Objects::nonNull)
      .map(developer -> Developer.as(developer.getName())
        .identifiedBy(developer.getId())
        .withEmailAddress(developer.getEmail())
        .withOrganization(buildDeveloperOrganization(developer))
        .withUri(nullSafeUriCreate(developer.getUrl())))
      .forEach(developers::add);

    return developers;
  }

  private @Nullable Organization buildDeveloperOrganization(org.apache.maven.model.Developer developer) {

    String developerOrganization = developer.getOrganization();

    return StringUtils.hasText(developerOrganization)
      ? Organization.as(developerOrganization).withUri(nullSafeUriCreate(developer.getOrganizationUrl()))
      : null;
  }

  @Override
  public @Nullable String getDescription() {
    return getMavenProject().getDescription();
  }

  @Override
  public @Nullable URI getIssueTracker() {

    return Optional.ofNullable(getMavenProject())
      .map(org.apache.maven.project.MavenProject::getIssueManagement)
      .map(IssueManagement::getUrl)
      .map(URI::create)
      .orElse(null);
  }

  @Override
  public Licenses getLicenses() {
    return this.licensesReference.updateAndGet(licenses -> licenses != null ? licenses : buildLicenses());
  }

  private @NonNull Licenses buildLicenses() {

    org.apache.maven.project.MavenProject mavenProject = getMavenProject();

    Licenses licenses = new Licenses();

    mavenProject.getLicenses().stream()
      .filter(Objects::nonNull)
      .map(license -> License.from(license.getName())
        .withUri(nullSafeUriCreate(license.getUrl())))
      .forEach(licenses::add);

    return licenses;
  }

  @Override
  public @Nullable Organization getOrganization() {
    return this.organizationReference.updateAndGet(organization -> organization != null ? organization
      : buildOrganization());
  }

  private @Nullable Organization buildOrganization() {

    org.apache.maven.project.MavenProject mavenProject = getMavenProject();

    return Optional.ofNullable(mavenProject.getOrganization())
      .map(org -> Project.Organization.as(org.getName())
        .withUri(nullSafeUriCreate(org.getUrl())))
      .orElse(null);
  }

  @Override
  public @Nullable URI getSourceRepository() {

    return Optional.ofNullable(getMavenProject())
      .map(org.apache.maven.project.MavenProject::getScm)
      .map(Scm::getUrl)
      .map(URI::create)
      .orElse(null);
  }

  @Override
  public @NonNull Version getVersion() {
    return Version.parse(getMavenProject().getVersion());
  }

  private @Nullable URI nullSafeUriCreate(@Nullable String url) {
    return StringUtils.hasText(url) ? URI.create(url) : null;
  }
}
