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
package org.cp.build.tools;

import org.cp.build.tools.shell.commands.NonCommandType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

/**
 * {@link SpringBootApplication} for Codeprimate Build Tools.
 *
 * @author John Blum
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.shell.command.annotation.CommandScan
 * @since 2.0.0
 */
@SpringBootApplication
@CommandScan(basePackageClasses = NonCommandType.class)
public class CodeprimateBuildToolsApplication {

  public static void main(String[] args) {
    SpringApplication.run(CodeprimateBuildToolsApplication.class, args);
  }
}
