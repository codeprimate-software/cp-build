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
package org.cp.build.tools.shell.commands.admin;

import java.util.Arrays;

import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.shell.commands.AbstractCommandsSupport;
import org.cp.build.tools.shell.jline.Colors;
import org.jline.utils.AttributedStringBuilder;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spring Shell {@link Command} class supplying {@link CommandGroup administrative commands} to the shell.
 *
 * @author John Blum
 * @see org.cp.build.tools.shell.commands.AbstractCommandsSupport
 * @see org.springframework.shell.core.command.annotation.Command
 * @see org.springframework.shell.core.command.annotation.CommandGroup
 * @see org.springframework.stereotype.Component
 * @since 2.0.0
 */
@Component
@CommandGroup(name = "admin commands")
@SuppressWarnings("unused")
public class AdminCommands extends AbstractCommandsSupport {

  @Override
  protected ProjectManager getProjectManager() {
    return null;
  }

  @Command(name = "add")
  public String add(@Option(required = true) int... numbers) {

    int sum = Arrays.stream(numbers)
      .reduce(Integer::sum)
      .orElse(0);

    Colors color = sum >= 0 ? Colors.GREEN : Colors.RED;

    return new AttributedStringBuilder()
      .style(toBoldText(color))
      .append(String.valueOf(sum))
      .toAnsi();
  }

  @Command(name = "hello")
  public String hello(@Option String user) {

    String resolvedUser = StringUtils.hasText(user) ? user
      : System.getProperty("user.name");

    return new AttributedStringBuilder()
      .style(toBoldText(Colors.WHITE))
      .append("Hello ")
      .style(toBoldText(Colors.YELLOW))
      .append(resolvedUser)
      .toAnsi();
  }

  @Command(name = "percent")
  public String percent(@Option(required = true) String ratio) {

    String[] numbers = ratio.split(Utils.FORWARD_SLASH);

    double number = Math.round(Double.parseDouble(numbers[0]) / Double.parseDouble(numbers[1]) * 100);

    return new AttributedStringBuilder()
      .style(toBoldText(Colors.GREEN))
      .append(String.valueOf(Double.valueOf(number).intValue()))
      .append(Utils.PERCENT)
      .toAnsi();
  }

  @Command
  public String ping() {

    return new AttributedStringBuilder()
      .style(toBoldText(Colors.GREEN))
      .append("PONG")
      .toAnsi();
  }
}
