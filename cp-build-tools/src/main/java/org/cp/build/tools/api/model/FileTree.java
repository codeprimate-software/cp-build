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
package org.cp.build.tools.api.model;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.cp.build.tools.api.model.FileTree.FileNode;
import org.cp.build.tools.api.support.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Abstract Data Type (ADT) modeling a filesystem directory structure with directories and files.
 *
 * @author John Blum
 * @see java.io.File
 * @since 0.1.0
 */
@Getter
@EqualsAndHashCode
@SuppressWarnings("unused")
public class FileTree implements Iterable<FileNode> {

  protected static final Predicate<FileNode> ALL_FILES_QUERY_PREDICATE = fileNode -> true;

  protected static final String DIRECTORY_PREFIX = "|--";
  protected static final String FILE_PREFIX = "-";

  public static FileTree scan(@NonNull File location) {

    Assert.notNull(location, "Location is required");
    Assert.isTrue(location.exists(), () -> "Location [%s] must exist".formatted(location));

    File directory = location.isDirectory() ? location : location.getParentFile();

    DirectoryNode directoryNode = DirectoryNode.from(directory);

    return new FileTree(buildTree(directoryNode));
  }

  private static @NonNull DirectoryNode buildTree(@NonNull DirectoryNode directoryNode) {

    File[] files = Utils.nullSafeFileArray(directoryNode.getFile().listFiles());

    Arrays.sort(files, Comparator.naturalOrder());

    for (File file : files) {
      if (Utils.nullSafeIsDirectory(file)) {
        DirectoryNode subdirectoryNode = DirectoryNode.from(file);
        directoryNode.add(subdirectoryNode);
        buildTree(subdirectoryNode);
      }
      else if (Utils.nullSafeIsFile(file)) {
        FileNode fileNode = FileNode.as(file);
        directoryNode.add(fileNode);
      }
    }

    return directoryNode;
  }

  private final DirectoryNode root;

  protected FileTree(@NonNull DirectoryNode directory) {
    this.root = Utils.requireObject(directory, "Directory is required");
  }

  public boolean isEmpty() {
    return size() < 1;
  }

  public boolean contains(@NonNull File file) {
    return !findBy(fileNode -> fileNode.getFile().equals(file)).isEmpty();
  }

  public int countBy(@NonNull Predicate<FileNode> queryPredicate) {
    return findBy(queryPredicate).size();
  }

  public List<FileNode> findBy(@NonNull Predicate<FileNode> queryPredicate) {

    Assert.notNull(queryPredicate, "Query Predicate is required");

    List<FileNode> fileNodes = new ArrayList<>();

    for (FileNode fileNode : getRoot()) {
      if (fileNode instanceof DirectoryNode directory) {
        fileNodes.addAll(new FileTree(directory).findBy(queryPredicate));
      }
      else if (queryPredicate.test(fileNode)) {
        fileNodes.add(fileNode);
      }
    }

    return fileNodes;
  }

  public Iterator<File> fileIterator() {
    return stream().map(FileNode::getFile).toList().iterator();
  }

  public Stream<File> fileStream() {
    return Utils.stream(this::fileIterator);
  }

  @Override
  public Iterator<FileNode> iterator() {
    return findBy(ALL_FILES_QUERY_PREDICATE).iterator();
  }

  public int size() {
    return countBy(ALL_FILES_QUERY_PREDICATE);
  }

  public Stream<FileNode> stream() {
    return Utils.stream(this);
  }

  public @NonNull String render() {
    return render(getRoot(), new StringWriter(), DIRECTORY_PREFIX).toString();
  }

  protected @NonNull StringWriter render(@NonNull DirectoryNode directoryNode,
      @NonNull StringWriter writer, @NonNull String prefix) {

    writer.append(Utils.newLineBefore(prefix)).append(directoryNode.getName());

    for (FileNode fileNode : directoryNode) {
      if (fileNode instanceof DirectoryNode directory) {
        render(directory, writer, prefix.concat(DIRECTORY_PREFIX));
      }
      else {
        writer.append(Utils.newLineBefore(prefix)).append(FILE_PREFIX).append(fileNode.getName());
      }
    }

    return writer;
  }

  @Override
  public String toString() {
    return getRoot().getPath();
  }

  @Getter
  public static class DirectoryNode extends FileNode implements Iterable<FileNode> {

    public static DirectoryNode from(@NonNull File directory) {

      Assert.notNull(directory, "Directory is required");
      Assert.isTrue(directory.isDirectory(), () -> "File [%s] must be a directory".formatted(directory));

      return new DirectoryNode(directory);
    }

    private final Set<FileNode> files = new TreeSet<>();

    public DirectoryNode(@NonNull File file) {
      super(file);
    }

    @Override
    public @NonNull String getPath() {
      return getFile().getAbsolutePath();
    }

    @SuppressWarnings("all")
    public DirectoryNode add(@NonNull FileNode fileNode) {

      if (fileNode != null) {
        this.files.add(fileNode);
      }

      return this;
    }

    @Override
    public Iterator<FileNode> iterator() {
      return Collections.unmodifiableSet(getFiles()).iterator();
    }

    public Stream<FileNode> stream() {
      return Utils.stream(this);
    }
  }

  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor(staticName = "as")
  @SuppressWarnings("unused")
  public static class FileNode implements Comparable<FileNode> {

    protected static final String EXTENSION_DELIMITER = ".";

    @Setter(AccessLevel.PROTECTED)
    private DirectoryNode directory;

    private final File file;

    public boolean isDirectory() {
      return getFile().isDirectory();
    }

    public boolean isFile() {
      return getFile().isFile();
    }

    public @NonNull String getExtension() {

      String name = getName();

      int extensionIndex = name.indexOf(EXTENSION_DELIMITER);

      return extensionIndex > -1
        ? name.substring(extensionIndex + 1)
        : Utils.EMPTY_STRING;
    }

    public @NonNull String getName() {
      return getFile().getName();
    }

    public @NonNull String getPath() {
      return getFile().getParentFile().getAbsolutePath();
    }

    public @NonNull FileNode in(@Nullable DirectoryNode directory) {
      setDirectory(directory);
      return this;
    }

    @Override
    public int compareTo(@NonNull FileNode that) {

      File thisFile = this.getFile();
      File thatFile = that.getFile();

      return thisFile.isDirectory() && !thatFile.isDirectory() ? -1
        : !thisFile.isDirectory() && thatFile.isDirectory() ? 1
        : thisFile.compareTo(thatFile);
    }

    @Override
    public String toString() {
      return getName();
    }
  }
}
