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
package org.cp.build.tools.core.service;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.cp.build.tools.core.model.Project;
import org.cp.build.tools.core.model.Session;
import org.cp.build.tools.core.support.Utils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring {@link Service} bean used to manage {@link Project Projects}
 *
 * @author John Blum
 * @see org.cp.build.tools.core.model.Project
 * @see org.cp.build.tools.core.model.Session
 * @see org.springframework.stereotype.Service
 * @since 2.0.0
 */
@Service
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ProjectManager implements Iterable<Project> {

  private final Session session;

  private final Set<Project> projects = new ConcurrentSkipListSet<>();

  public Optional<Project> findByName(String projectName) {

    return stream()
      .filter(project -> project.getName().equalsIgnoreCase(projectName))
      .findFirst();
  }

  public Optional<Project> getCurrentProject() {
    return Optional.ofNullable(getSession().getCurrentProject());
  }

  @SuppressWarnings("all")
  public @NonNull Project setCurrentProject(@NonNull Project project) {

    return getSession()
      .setProject(Utils.requireObject(project, "Project to activate is required"))
      .getCurrentProject();
  }

  @Override
  public Iterator<Project> iterator() {
    return list().iterator();
  }

  public List<Project> list() {
    return getProjects().stream().sorted().toList();
  }

  @Cacheable(cacheNames = "projects", keyGenerator = "projectCacheKeyGenerator", sync = true)
  public @NonNull Project resolveByLocation(File location) {

    Project project = Project.from(location);

    getProjects().add(project);

    return project;
  }

  public Stream<Project> stream() {
    return StreamSupport.stream(this.spliterator(), false);
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
}
