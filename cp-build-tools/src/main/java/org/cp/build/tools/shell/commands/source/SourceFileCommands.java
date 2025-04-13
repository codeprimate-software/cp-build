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
package org.cp.build.tools.shell.commands.source;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.cp.build.tools.api.model.FileTree;
import org.cp.build.tools.api.model.FileTree.FileNode;
import org.cp.build.tools.api.model.Project;
import org.cp.build.tools.api.model.SourceFile;
import org.cp.build.tools.api.service.ProjectManager;
import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.shell.commands.AbstractCommandsSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.shell.Availability;
import org.springframework.shell.AvailabilityProvider;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;
import org.springframework.shell.command.annotation.Option;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Shell {@link Command Commands} for {@link Project} {@link File source files}.
 *
 * @author John Blum
 * @see java.io.File
 * @see org.cp.build.tools.api.model.FileTree
 * @see org.cp.build.tools.api.model.Project
 * @see org.cp.build.tools.api.service.ProjectManager
 * @see org.cp.build.tools.shell.commands.AbstractCommandsSupport
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.shell.command.annotation.Command
 * @since 2.0.0
 */
@Command(command = "source", group = "source file commands")
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SourceFileCommands extends AbstractCommandsSupport {

  protected static final String NEW_LINE = "\n";
  protected static final String SOURCE_DIRECTORY_NAME = "src";

  @Getter(AccessLevel.PROTECTED)
  private final ProjectManager projectManager;

  @Command(command = "count")
  @CommandAvailability(provider = "sourceCommandsAvailability")
  public int count(
      @Option(longNames = "exclude-filter") String excludeFilter,
      @Option(longNames = "include-filter") String includeFilter,
      @Option(longNames = "location", shortNames = 'l') String location,
      @Option(longNames = "main") boolean main,
      @Option(longNames = "test") boolean test) {

    String sourceDirectoryName =  main ? SOURCE_DIRECTORY_NAME.concat(File.separator).concat("main")
      : test ? SOURCE_DIRECTORY_NAME.concat(File.separator).concat("test")
      : SOURCE_DIRECTORY_NAME;

    String resolvedSourceDirectoryName = StringUtils.hasText(location)
      ? sourceDirectoryName.concat(File.separator).concat(location)
      : sourceDirectoryName;

    Predicate<FileNode> fileFilterPredicate = FileNode::isDirectory;

    fileFilterPredicate = fileFilterPredicate.or(FileNode::isFile);

    if (StringUtils.hasText(includeFilter)) {
      fileFilterPredicate = fileFilterPredicate.and(fileNode -> fileNode.getName().contains(includeFilter));
    }

    if (StringUtils.hasText(excludeFilter)) {
      fileFilterPredicate = fileFilterPredicate.and(fileNode -> !fileNode.getName().contains(excludeFilter));
    }

    Project project = requireProject();

    return FileTree.scan(Optional.of(project.getDirectory())
        .map(projectDirectory -> new File(projectDirectory, resolvedSourceDirectoryName))
        .filter(File::exists)
        .orElseThrow(() ->
          new IllegalStateException("Failed to resolve source directory [%1$s] from project [%2$s]"
            .formatted(resolvedSourceDirectoryName, project.getName()))))
      .findBy(fileFilterPredicate)
      .size();
  }

  @Command(command = "line-count")
  @SuppressWarnings("all")
  public String lineCount(
      @Option(longNames = "count", shortNames = 'c') boolean count,
      @Option(longNames = "gt", defaultValue = "0") long minimumLineCount,
      @Option(longNames = "list", shortNames = 'l') boolean list,
      @Option(longNames = "main", shortNames = 'm') boolean main,
      @Option(longNames = "test", shortNames = 't') boolean test) {

    Project project = requireProject();

    String sourceDirectoryName =  main ? SOURCE_DIRECTORY_NAME.join(File.separator, "main")
      : test ? SOURCE_DIRECTORY_NAME.join(File.separator, "test")
      : SOURCE_DIRECTORY_NAME;

    File sourceDirectory = new File(project.getDirectory(), sourceDirectoryName);

    Set<SourceFile> sourceFiles = FileTree.scan(sourceDirectory)
      .findBy(FileNode::isFile)
      .stream()
      .map(FileNode::getFile)
      .map(SourceFile::from)
      .collect(Collectors.toSet());

    Comparator<SourceFile> sourceFileOrder = Comparator.<SourceFile, Long>comparing(SourceFile::lineCount).reversed()
      .thenComparing(SourceFile::getPackage)
      .thenComparing(SourceFile::getName);

    if (list) {
      List<String> sourceFilesPlusLineCount = sourceFiles.parallelStream()
        .filter(sourceFile -> sourceFile.lineCount() > minimumLineCount)
        .sorted(sourceFileOrder)
        .map(sourceFile -> "%d: %s".formatted(sourceFile.lineCount(), sourceFile.getRelativePath()))
        .toList();

        return String.join(NEW_LINE, sourceFilesPlusLineCount.toArray(String[]::new));
    }
    else if (count) {
      long fileCount = sourceFiles.parallelStream()
        .map(SourceFile::lineCount)
        .filter(sourceFileLineCount -> sourceFileLineCount > minimumLineCount)
        .count();

      return String.valueOf(fileCount);
    }
    else  {
      long lineCount = sourceFiles.parallelStream()
        .map(SourceFile::lineCount)
        .filter(sourceFileLineCount -> sourceFileLineCount > minimumLineCount)
        .reduce(Long::sum)
        .orElse(0L);

      return String.valueOf(lineCount);
    }
  }

  @Command(command = "list")
  @CommandAvailability(provider = "sourceCommandsAvailability")
  public @NonNull String list(@Option(longNames = "location", shortNames = 'l') String location) {

    String sourceDirectoryName = SOURCE_DIRECTORY_NAME;

    String resolvedSourceDirectoryName = StringUtils.hasText(location)
      ? sourceDirectoryName.concat(File.separator).concat(location)
      : sourceDirectoryName;

    Project project = requireProject();

    List<File> files = FileTree.scan(Optional.of(project.getDirectory())
      .map(projectDirectory -> new File(projectDirectory, resolvedSourceDirectoryName))
      .filter(File::exists)
      .orElseThrow(() ->
        new IllegalStateException("Failed to resolve source directory [%1$s] from project [%2$s]"
          .formatted(resolvedSourceDirectoryName, project.getName()))))
      .fileStream()
      .toList();

    StringBuilder output = new StringBuilder();

    for (File file : files) {
      output.append(Utils.newLineBefore(file.getAbsolutePath()));
    }

    output.append(Utils.newLineBeforeAfter("Count: ".concat(String.valueOf(files.size()))));

    return output.toString();
  }

  @Command(command = "tree")
  @CommandAvailability(provider = "sourceCommandsAvailability")
  public @NonNull String tree(
      @Option(longNames = "location", shortNames = 'l') String location,
      @Option(longNames = "main") boolean main,
      @Option(longNames = "test") boolean test) {

    String sourceDirectoryName =  main ? SOURCE_DIRECTORY_NAME.concat(File.separator).concat("main")
      : test ? SOURCE_DIRECTORY_NAME.concat(File.separator).concat("test")
      : SOURCE_DIRECTORY_NAME;

    String resolvedSourceDirectoryName = StringUtils.hasText(location)
      ? sourceDirectoryName.concat(File.separator).concat(location)
      : sourceDirectoryName;

    Project project = requireProject();

    FileTree fileTree = FileTree.scan(Optional.of(project.getDirectory())
      .map(projectDirectory -> new File(projectDirectory, resolvedSourceDirectoryName))
      .filter(File::exists)
      .orElseThrow(() ->
        new IllegalStateException("Failed to resource source directory [%1$s] from project [%2$s]"
          .formatted(resolvedSourceDirectoryName, project.getName()))));

    return fileTree.render();
  }

  @NonNull @Bean
  AvailabilityProvider sourceCommandsAvailability() {

    return isProjectSet() ? Availability::available
      : () -> Availability.unavailable("the current project is not set;"
      + " please call 'project load <location>' or 'project use <name>'");
  }
}
