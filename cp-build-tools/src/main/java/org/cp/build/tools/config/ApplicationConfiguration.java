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
package org.cp.build.tools.config;

import java.io.File;
import java.util.Arrays;

import org.cp.build.tools.api.model.Session;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.git.model.GitProject;
import org.cp.build.tools.git.support.GitTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Spring {@link Configuration} class used to configure and enable additional application services.
 *
 * @author John Blum
 * @see org.springframework.cache.annotation.EnableCaching
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @since 2.0.0
 */
@Configuration
@EnableCaching
@SuppressWarnings("unused")
public class ApplicationConfiguration {

  @Bean
  CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("projects");
  }

  @Bean
  GitTemplate gitTemplate(Session session) {
    return GitTemplate.from(() -> GitProject.from(session.requireProject()).getGit());
  }

  @Bean
  KeyGenerator projectCacheKeyGenerator() {

    return (target, method, arguments) -> {

      Assert.isTrue(arguments.length > 0,
        () -> String.format("Expected @Cacheable method [%1$s] on target [%2$s] to have at least 1 argument",
          method.getName(), target.getClass().getName()));

      Object firstArgument = arguments[0];

      if (firstArgument instanceof File file) {
        return ProjectManager.CacheKey.of(file);
      }
      else if (firstArgument instanceof String name) {
        return ProjectManager.CacheKey.of(name);
      }

      throw new IllegalArgumentException(String.format("Cannot generate project cache key from arguments [%s]",
        Arrays.toString(arguments)));
    };
  }
}
