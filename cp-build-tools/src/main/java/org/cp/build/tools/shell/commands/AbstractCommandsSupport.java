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
package org.cp.build.tools.shell.commands;

import org.cp.build.tools.api.support.Utils;
import org.springframework.lang.NonNull;
import org.springframework.shell.command.annotation.Command;

/**
 * Abstract base class for all Spring Shell {@link Command} classes.
 *
 * @author John Blum
 * @since 2.0.0
 */
public abstract class AbstractCommandsSupport {

  protected @NonNull String indent(@NonNull String content) {

    String[] lines = content.split(Utils.NEW_LINE_REGEX);

    StringBuilder indentedContent = new StringBuilder();

    for (String line : lines) {
      line = Utils.nullSafeTrimmedString(line);
      indentedContent.append(Utils.NEW_LINE_TAB).append(line);
    }

    return indentedContent.toString();
  }
}
