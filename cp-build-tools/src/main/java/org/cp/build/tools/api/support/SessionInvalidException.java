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

import org.cp.build.tools.api.model.Session;

/**
 * Java {@link RuntimeException} thrown when a {@link Session} is not valid or the user has become inactive
 * and timed-out.
 *
 * @author John Blum
 * @see java.lang.RuntimeException
 * @see org.cp.build.tools.api.model.Session
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class SessionInvalidException extends RuntimeException {

  public SessionInvalidException() { }

  public SessionInvalidException(String message) {
    super(message);
  }

  public SessionInvalidException(Throwable cause) {
    super(cause);
  }

  public SessionInvalidException(String message, Throwable cause) {
    super(message, cause);
  }
}
