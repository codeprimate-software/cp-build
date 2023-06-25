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
package org.cp.build.tools.api.support;

import org.cp.build.tools.api.model.Project;

/**
 * Java {@link RuntimeException} thrown when a {@link Project} is not found.
 *
 * @author John Blum
 * @see java.lang.RuntimeException
 * @see org.cp.build.tools.api.model.Project
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class ProjectNotFoundException extends RuntimeException {

  public ProjectNotFoundException() { }

  public ProjectNotFoundException(String message) {
    super(message);
  }

  public ProjectNotFoundException(Throwable cause) {
    super(cause);
  }

  public ProjectNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
