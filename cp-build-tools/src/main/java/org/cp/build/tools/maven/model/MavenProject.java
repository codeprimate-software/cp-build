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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.cp.build.tools.core.model.Project;
import org.cp.build.tools.core.support.Utils;
import org.cp.build.tools.maven.support.MavenPomNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * {@link Project} implementation based on Apache Maven.
 *
 * @author John Blum
 * @see java.io.File
 * @see java.net.URI
 * @see org.cp.build.tools.core.model.Project
 * @see org.apache.maven.model.Model
 * @see org.apache.maven.project.MavenProject
 * @since 2.0.0
 */
@Getter(AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class MavenProject extends Project {

  public static final String POM_XML = "pom.xml";

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

      String message =
        String.format("File [%s] must refer to a Maven POM file or directory containing a Maven POM file", location);

      throw new MavenPomNotFoundException(message);
    }

    return location;
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

    MavenXpp3Reader reader = new MavenXpp3Reader();

    try (FileReader pomReader = new FileReader(pom)) {
      Model model = reader.read(pomReader);
      return new MavenProject(new org.apache.maven.project.MavenProject(model));
    }
    catch (Exception cause) {
      String message = String.format("Failed to read Maven POM from file [%s]", pom);
      throw new IllegalStateException(message, cause);
    }
  }

  private static @NonNull File resolvePomXml(@NonNull File file) {
    return Utils.nullSafeIsDirectory(file) ? new File(file, POM_XML) : file;
  }

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
  public Artifact getArtifact() {

    org.apache.maven.project.MavenProject mavenProject = getMavenProject();

    return Artifact.from(this, mavenProject.getArtifactId())
      .withGroupId(mavenProject.getGroupId());
  }

  @Override
  public String getDescription() {
    return getMavenProject().getDescription();
  }

  @Override
  public URI getIssueTracker() {
    return URI.create(getMavenProject().getIssueManagement().getUrl());
  }

  @Override
  public URI getSourceRepository() {
    return URI.create(getMavenProject().getScm().getUrl());
  }

  @Override
  public Version getVersion() {
    return Version.parse(getMavenProject().getVersion());
  }
}
