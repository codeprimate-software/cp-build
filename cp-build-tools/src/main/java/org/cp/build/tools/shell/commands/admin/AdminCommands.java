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

import java.text.NumberFormat;
import java.util.Arrays;

import org.cp.build.tools.api.support.Utils;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.util.StringUtils;

/**
 * Spring Shell {@link Command} class supplying {@link Command administrative commands} to the shell.
 *
 * @author John Blum
 * @see org.springframework.shell.command.annotation.Command
 * @see org.springframework.shell.command.annotation.Option
 * @see org.springframework.shell.standard.ShellComponent
 * @since 2.0.0
 */
@Command
@SuppressWarnings("unused")
public class AdminCommands {

  @Command(command = "add")
  public int add(@Option(required = true) String numbers) {

    return Arrays.stream(Utils.nullSafeTrimmedString(numbers).split(Utils.COMMA))
      .filter(StringUtils::hasText)
      .map(String::trim)
      .map(Integer::parseInt)
      .reduce(Integer::sum)
      .orElse(0);
  }

  @Command(command = "hello")
  public String hello(@Option String user) {

    String resolvedUser = StringUtils.hasText(user) ? user
      : System.getProperty("user.name");

    return String.format("Hello %s", resolvedUser);
  }

  @Command(command = "percent")
  public String percent(@Option(required = true) String ratio) {

    String[] numbers = ratio.split(Utils.FORWARD_SLASH);

    double number = Double.parseDouble(numbers[0]) / Double.parseDouble(numbers[1]);

    number *= 100;
    number = Math.round(number);

    return NumberFormat.getPercentInstance().format(Double.valueOf(number).longValue());
  }

  @Command
  public String ping() {
    return "PONG";
  }
}
