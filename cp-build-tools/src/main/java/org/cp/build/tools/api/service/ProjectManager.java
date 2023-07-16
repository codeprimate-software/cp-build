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
package org.cp.build.tools.api.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.model.Session;
import org.cp.build.tools.api.support.Utils;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring {@link Service} bean used to manage {@link Project Projects}
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @see org.cp.build.tools.api.model.Project
 * @see org.cp.build.tools.api.model.Session
 * @see org.springframework.stereotype.Service
 * @since 2.0.0
 */
@Slf4j
@Service
@Getter(AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class ProjectManager implements Iterable<Project> {

  protected static final File CODEPRIMATE_BUILD_TOOLS_DIRECTORY
    = new File(Utils.USER_HOME_DIRECTORY, ".cp-build-tools");

  protected static final File PROJECT_PROPERTIES =
    new File(CODEPRIMATE_BUILD_TOOLS_DIRECTORY, "project.properties");

  private final Session session;

  private final Set<RecentProject> recentProjects = new ConcurrentSkipListSet<>();

  private final Set<Project> projects = new ConcurrentSkipListSet<>();

  /**
   * Constructs a new {@link ProjectManager} initialized with the given, required {@link Session}.
   *
   * @param session {@link Session object} managing the current state of the user's interactive session
   * with the shell.
   * @throws IllegalArgumentException if the given {@link Session} is {@literal null}.
   * @see org.cp.build.tools.api.model.Session
   */
  public ProjectManager(@NonNull Session session) {
    this.session = Utils.requireObject(session, "Session is required");
  }

  /**
   * Sets up the current {@link Project} using the {@link File current working directory}.
   */
  @PostConstruct
  public void onInitialization() {
    activateProjectInWorkingDirectory();
    loadRecentProjects();
  }

  private void activateProjectInWorkingDirectory() {

    try {
      getSession().setProject(resolveByLocation(Utils.WORKING_DIRECTORY));
    }
    catch (IllegalArgumentException cause) {
      getLogger().warn("Failed to set current Project [{}]", cause.getMessage());
    }
  }

  private void loadRecentProjects() {

    if (PROJECT_PROPERTIES.isFile()) {
      try (BufferedReader reader = newBufferedFileReader(PROJECT_PROPERTIES)) {

        Properties projectProperties = new Properties();

        projectProperties.load(reader);

        projectProperties.stringPropertyNames().stream()
          .map(projectName -> RecentProject.of(projectName, new File(projectProperties.getProperty(projectName))))
          .forEach(getRecentProjects()::add);
      }
      catch (IOException cause) {
        getLogger().warn("Failed to load Codeprimate Build Tools project.properties", cause);
      }
    }
  }

  private @NonNull BufferedReader newBufferedFileReader(@NonNull File file) throws FileNotFoundException {
    return new BufferedReader(new FileReader(file));
  }

  /**
   * Save the {@link Project#getName() Project Names} mapped to {@link Project#getDirectory() Project Locations}
   * to a {@link Properties} {@link File} on application shutdown.
   */
  @PreDestroy
  public void onShutdown() {
    saveRecentProjects();
  }

  private void saveRecentProjects() {

    assertOrMakeCodeprimateBuildToolsDirectory();

    try (BufferedWriter writer = newBufferedFileWriter(PROJECT_PROPERTIES)) {

      Properties projectProperties = new Properties();

      getRecentProjects().stream()
        .filter(RecentProject::exists)
        .forEach(recentProject -> projectProperties.setProperty(recentProject.getName(),
          recentProject.getLocation().getAbsolutePath()));

      projectProperties.store(writer, "Codeprimate Build Tools project.properties used to"
        + " record Project Names to Project Locations");
    }
    catch (IOException cause) {
      getLogger().warn("Failed to save Codeprimate Build Tools project.properties", cause);
    }
  }

  private static void assertOrMakeCodeprimateBuildToolsDirectory() {
    Assert.state(CODEPRIMATE_BUILD_TOOLS_DIRECTORY.isDirectory() || CODEPRIMATE_BUILD_TOOLS_DIRECTORY.mkdirs(),
      () -> String.format("Directory [%s] not found", CODEPRIMATE_BUILD_TOOLS_DIRECTORY));
  }

  private @NonNull BufferedWriter newBufferedFileWriter(@NonNull File file) throws IOException {
    return new BufferedWriter(new FileWriter(file, Charset.defaultCharset(), false));
  }

  /**
   * Gets an {@link Optional} reference to the current {@link Project}.
   *
   * @return an {@link Optional} reference to the current {@link Project}.
   * @see org.cp.build.tools.api.model.Session#getProject()
   * @see org.cp.build.tools.api.model.Project
   * @see #getSession()
   */
  public Optional<Project> getCurrentProject() {
    return Optional.ofNullable(getSession().getProject());
  }

  /**
   * Sets a reference to the current (active) {@link Project}.
   *
   * @param project current, activated {@link Project}; must not bw {@literal null}.
   * @return the given {@linbk Project}.
   * @see org.cp.build.tools.api.model.Session#setProject(Project)
   * @see org.cp.build.tools.api.model.Project
   * @see #getSession()
   */
  @SuppressWarnings("all")
  public @NonNull Project setCurrentProject(@NonNull Project project) {

    return getSession()
      .setProject(Utils.requireObject(project, "Project to activate is required"))
      .getProject();
  }

  protected Logger getLogger() {
    return log;
  }

  /**
   * Searches for a {@literal loaded} {@link Project} by {@link Project#getName() name}.
   *
   * @param projectName {@link String name} of the requested {@link Project}.
   * @return an {@link Optional} {@link Project} with the given {@link String name} if loaded.
   * @see org.cp.build.tools.api.model.Project
   * @see java.util.Optional
   */
  public Optional<Project> findByName(String projectName) {

    return stream()
      .filter(project -> project.getName().equalsIgnoreCase(projectName))
      .findFirst();
  }

  @Override
  public Iterator<Project> iterator() {
    return list().iterator();
  }

  /**
   * Lists the currently loaded {@link Project Projects}.
   *
   * @return a {@link List} of the currently loaded {@link Project Projects}.
   * @see org.cp.build.tools.api.model.Project
   * @see java.util.List
   * @see #getProjects()
   * @see #recent()
   */
  public List<Project> list() {
    return getProjects().stream().sorted().toList();
  }

  /**
   * Lists all recently loaded {@link Project Projects}.
   *
   * @return a {@link List} of all recently loaded {@link Project Projects}.
   * @see org.cp.build.tools.api.service.ProjectManager.RecentProject
   * @see #getRecentProjects()
   * @see java.util.List
   * @see #list()
   */
  public List<RecentProject> recent() {
    return getRecentProjects().stream().sorted().toList();
  }

  /**
   * Resolves a {@link Project} from the given {@link }
   *
   * @param location {@link File} referring to the filesystem directory location of the {@link Project}.
   * @return the resolved {@link Project} at {@link File location}.
   * @throws IllegalArgumentException if a {@link Project} cannot be resolved from the given {@link File location}.
   * @see org.cp.build.tools.api.model.Project
   * @see java.io.File
   */
  @Cacheable(cacheNames = "projects", keyGenerator = "projectCacheKeyGenerator", sync = true)
  public @NonNull Project resolveByLocation(File location) {

    Project project = Project.from(location);

    getProjects().add(project);
    getRecentProjects().add(RecentProject.from(project));

    return project;
  }

  /**
   * Streams all the {@link Project Projects} being managed by this {@link ProjectManager}.
   *
   * @return a {@link Stream} of all the {@link Project Projects} being managed by this {@link ProjectManager}.
   * @see org.cp.build.tools.api.model.Project
   * @see java.util.stream.Stream
   * @see #iterator()
   */
  public Stream<Project> stream() {
    return Utils.stream(this);
  }

  @Getter
  public static class CacheKey implements Comparable<CacheKey> {

    public static @NonNull CacheKey of(@NonNull File location) {
      return new CacheKey(null, Utils.requireObject(location, "Project location is required"));
    }

    public static CacheKey of(String name) {
      return new CacheKey(Utils.requireObject(name, "Project name is required"), null);
    }

    protected static final String PROJECT_CACHE_KEY_TO_STRING =
      "{ @type = %1$s, projectName = %2$s, projectLocation = %3$s }";

    private final File projectLocation;

    private final String projectName;

    protected CacheKey(String name, File location) {
      this.projectName = name;
      this.projectLocation = location;
    }

    @Override
    public int compareTo(CacheKey that) {

      int result = this.getProjectName().compareTo(that.getProjectName());

      return result != 0 ? result
        : this.getProjectLocation().compareTo(that.getProjectLocation());
    }

    @Override
    public boolean equals(Object obj) {

      if (this == obj) {
        return true;
      }

      if (!(obj instanceof CacheKey that)) {
        return false;
      }

      return Objects.equals(this.getProjectName(), that.getProjectName())
        && Objects.equals(this.getProjectLocation(), that.getProjectLocation());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getProjectName(), getProjectLocation());
    }

    @Override
    public String toString() {
      return String.format(PROJECT_CACHE_KEY_TO_STRING,
        getClass().getName(), getProjectName(), getProjectLocation());
    }
  }

  @Getter
  @EqualsAndHashCode(of = "name")
  @RequiredArgsConstructor(staticName = "of")
  public static class RecentProject implements Comparable<RecentProject> {

    public static @NonNull RecentProject from(@NonNull Project project) {

      Assert.notNull(project, "Project is required");

      return of(project.getName(), project.getDirectory());
    }

    private final String name;
    private final File location;

    @Override
    public int compareTo(@NonNull RecentProject that) {
      return this.getName().compareTo(that.getName());
    }

    public boolean exists() {
      return getLocation().isDirectory();
    }

    @Override
    public String toString() {
      return getName();
    }
  }
}
