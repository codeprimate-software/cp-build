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
package org.cp.build.tools.git.support;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.cp.build.tools.core.support.Utils;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.git.model.CommitRecord;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.lang.NonNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Data Access Object (DAO) and Template for {@link Git}.
 *
 * @author John Blum
 * @see org.cp.build.tools.git.model.CommitHistory
 * @see org.cp.build.tools.git.model.CommitRecord
 * @see org.eclipse.jgit.api.Git
 * @since 2.0.0
 */
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class GitTemplate {

  protected static final String MAIN_BRANCH_NAME = "main";
  protected static final String MASTER_BRANCH_NAME = "master";

  public static @NonNull GitTemplate from(@NonNull Supplier<Git> gitSupplier) {

    Utils.requireObject(gitSupplier, "Supplier for Git is required");

    return new GitTemplate(gitSupplier);
  }

  @NonNull
  private final Supplier<Git> git;

  protected @NonNull Git git() {
    return getGit().get();
  }

  public @NonNull CommitHistory getCommitHistory() {

    try (Git git = git()){

      LogCommand logCommand = git.log().all();

      Iterable<RevCommit> commits = logCommand.call();

      Set<CommitRecord> commitRecords = new HashSet<>();

      for (RevCommit commit : commits) {

        PersonIdent commitAuthor = commit.getAuthorIdent();

        CommitRecord.Author author = CommitRecord.Author.as(commitAuthor.getName())
          .withEmail(commitAuthor.getEmailAddress());

        LocalDateTime date = Instant.ofEpochMilli(commit.getCommitTime())
          .atZone(ZoneOffset.systemDefault())
          .toLocalDateTime();

        String hash = commit.name();

        CommitRecord commitRecord = CommitRecord.of(author, date, hash)
          .withMessage(commit.getFullMessage());

        List<File> sourceFiles = new ArrayList<>();

        try (TreeWalk treeWalk = new TreeWalk(logCommand.getRepository())) {
          treeWalk.reset(commit.getTree().getId());
          while (treeWalk.next()) {
            sourceFiles.add(new File(treeWalk.getPathString()));
          }
        }

        commitRecord.add(sourceFiles.toArray(new File[0]));
        commitRecords.add(commitRecord);
      }

      return CommitHistory.of(commitRecords);
    }
    catch (Exception cause) {
      throw new GitException("Failed to load commit history", cause);
    }
  }
}
