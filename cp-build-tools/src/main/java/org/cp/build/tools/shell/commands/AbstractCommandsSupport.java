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

import java.util.List;
import java.util.Optional;

import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.shell.jline.Colors;
import org.jline.utils.AttributedStyle;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.shell.command.annotation.Command;

/**
 * Abstract base class for all Spring Shell {@link Command} classes.
 *
 * @author John Blum
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public abstract class AbstractCommandsSupport {

  protected abstract ProjectManager getProjectManager();

  protected boolean isCurrentProject(@Nullable Project project) {

    return currentProject()
      .filter(currentProject -> currentProject.equals(project))
      .isPresent();
  }

  protected boolean isProjectSet() {
    return currentProject().isPresent();
  }

  protected Optional<Project> currentProject() {
    return getProjectManager().getCurrentProject();
  }

  protected List<Project> projects() {
    return getProjectManager().list();
  }

  protected List<ProjectManager.RecentProject> recentProjects() {
    return getProjectManager().recent();
  }

  protected @NonNull Project requireProject() {
    return currentProject().orElseThrow(() -> new IllegalStateException("Project not set"));
  }

  protected @NonNull String indent(@NonNull String content) {

    String[] lines = content.split(Utils.NEW_LINE_REGEX);

    StringBuilder indentedContent = new StringBuilder();

    for (String line : lines) {

      line = Utils.nullSafeTrimmedString(line);

      if (line.isEmpty()) {
        indentedContent.append(Utils.newLine());
      }
      else {
        indentedContent.append(Utils.newLine()).append(Utils.getSpaces(8).concat(line));
      }
    }

    return indentedContent.toString();
  }

  protected AttributedStyle toBoldText(Colors color) {
    return AttributedStyle.BOLD.foreground(color.toJLineAttributeStyleColor());
  }

  protected AttributedStyle toBoldItalicText(Colors color) {
    return toBoldText(color).italic();
  }

  protected AttributedStyle toItalicText(Colors color) {
    return toPlainText(color).italic();
  }

  protected AttributedStyle toPlainText(Colors color) {
    return AttributedStyle.DEFAULT.foreground(color.toJLineAttributeStyleColor()).italicOff();
  }
}
