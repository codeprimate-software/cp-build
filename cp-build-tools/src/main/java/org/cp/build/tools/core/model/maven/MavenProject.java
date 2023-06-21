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
package org.cp.build.tools.core.model.maven;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.Arrays;

import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingException;
import org.cp.build.tools.core.model.Project;
import org.cp.build.tools.core.support.Utils;
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
   * @see #assertPomXml(File)
   * @see java.io.File
   */
  public static @NonNull MavenProject fromMavenPom(@NonNull File pom) {

    assertPomXml(pom);

    try {
      org.apache.maven.project.MavenProject mavenProject = new DefaultProjectBuilder()
        .build(pom, new DefaultProjectBuildingRequest())
        .getProject();

      return (MavenProject) new MavenProject(mavenProject).inWorkingDirectory(pom);
    }
    catch (ProjectBuildingException cause) {
      String message = String.format("Failed to build Project from Maven POM [%s]", pom);
      throw new RuntimeException(message, cause);
    }
  }

  private static @NonNull File assertPomXml(@NonNull File pom) {

    if (!isPomXml(pom)) {
      throw new IllegalArgumentException(String.format("File [%s] must refer to a Maven POM", pom));
    }

    return pom;
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

    org.apache.maven.artifact.Artifact artifact = getMavenProject().getArtifact();

    return Artifact.from(this, artifact.getId())
      .withGroupId(artifact.getGroupId());
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
