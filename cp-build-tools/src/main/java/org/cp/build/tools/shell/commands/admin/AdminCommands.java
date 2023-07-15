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
import org.springframework.shell.command.CommandRegistration.OptionArity;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.util.StringUtils;

/**
 * Spring Shell {@link Command} class supplying {@link Command administrative commands} to the shell.
 *
 * @author John Blum
 * @see org.cp.build.tools.shell.commands.AbstractCommandsSupport
 * @see org.springframework.shell.command.annotation.Command
 * @see org.springframework.shell.command.annotation.Option
 * @see org.springframework.shell.standard.ShellComponent
 * @since 2.0.0
 */
@Command
@SuppressWarnings("unused")
public class AdminCommands extends AbstractCommandsSupport {

  @Override
  protected ProjectManager getProjectManager() {
    return null;
  }

  @Command(command = "add")
  public String add(@Option(arity = OptionArity.ONE_OR_MORE, required = true) int... numbers) {

    return new AttributedStringBuilder()
      .style(toBoldText(Colors.GREEN))
      .append(String.valueOf(Arrays.stream(numbers)
        .reduce(Integer::sum)
        .orElse(0)))
      .toAnsi();
  }

  @Command(command = "hello")
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

  @Command(command = "percent")
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
