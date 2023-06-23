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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.git.model.CommitHistory;
import org.cp.build.tools.git.model.CommitRecord;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
    return new GitTemplate(Utils.requireObject(gitSupplier, "Supplier for Git is required"));
  }

  @NonNull
  private final Supplier<Git> git;

  @Setter(AccessLevel.PROTECTED)
  private Supplier<File> sourceFilesDirectoryResolver = () -> Utils.WORKING_DIRECTORY;

  protected @NonNull Git git() {
    return getGit().get();
  }

  @SuppressWarnings("all")
  public @NonNull GitTemplate usingSourceFilesDirectoryResolver(@NonNull Supplier<File> sourceFilesDirectoryResolver) {

    if (sourceFilesDirectoryResolver != null) {
      setSourceFilesDirectoryResolver(sourceFilesDirectoryResolver);
    }

    return this;
  }

  public @NonNull CommitHistory getCommitHistory() {

    try (Git git = git()){

      LogCommand logCommand = git.log().all();

      Iterable<RevCommit> commits = logCommand.call();

      Repository repository = logCommand.getRepository();

      Set<CommitRecord> commitRecords = new HashSet<>();

      for (RevCommit commit : commits) {

        CommitRecord commitRecord =
          resolveCommittedSourceFiles(repository, commit, newCommitRecord(commit));

        commitRecords.add(commitRecord);
      }

      return CommitHistory.of(commitRecords);
    }
    catch (Exception cause) {
      throw new GitException("Failed to load commit history", cause);
    }
  }

  private @NonNull CommitRecord newCommitRecord(@NonNull RevCommit commit) {

    CommitterIdentity committerIdentity = resolveCommitAuthor(commit);

    LocalDateTime dateTime = resolveCommitDateTime(commit, committerIdentity);

    String hash = resolveCommitHash(commit);

    CommitRecord.Author author = newCommitRecordAuthor(committerIdentity);

    return CommitRecord.of(author, dateTime, hash)
      .withMessage(commit.getFullMessage());
  }

  private @NonNull CommitRecord.Author newCommitRecordAuthor(@NonNull CommitterIdentity committerIdentity) {
    return CommitRecord.Author.as(committerIdentity.getName()).withEmailAddress(committerIdentity.getEmailAddress());
  }

  private CommitterIdentity resolveCommitAuthor(@NonNull RevCommit commit) {
    return CommitterIdentity.of(commit.getAuthorIdent(), commit.getCommitterIdent());
  }

  private LocalDateTime resolveCommitDateTime(@NonNull RevCommit commit, @NonNull CommitterIdentity committerIdentity) {
    return committerIdentity.resolveCommitDateTime(commit);
  }

  private String resolveCommitHash(@NonNull RevCommit commit) {
    return commit.name();
  }

  private @NonNull CommitRecord resolveCommittedSourceFiles(@NonNull Repository repository, @NonNull RevCommit commit,
      @NonNull CommitRecord commitRecord) throws Exception {

    List<File> sourceFiles = resolveCommittedSourceFilesUsingDiffFormatter(repository, commit);

    commitRecord.add(sourceFiles.toArray(new File[0]));

    return commitRecord;
  }

  // @see https://www.eclipse.org/forums/index.php/t/213979/
  private List<File> resolveCommittedSourceFilesUsingDiffFormatter(Repository repository, RevCommit commit)
      throws Exception {

    DiffFormatter diffFormatter = newDiffFormatter(repository);

    List<DiffEntry> diffs =
      diffFormatter.scan(resolvePreviousCommit(repository, commit).getTree(), commit.getTree());

    List<File> sourceFiles = new ArrayList<>();

    for (DiffEntry diff : diffs) {
      sourceFiles.add(new File(getSourceFilesDirectoryResolver().get(), diff.getNewPath()));
    }

    return sourceFiles;
  }

  private static @NonNull DiffFormatter newDiffFormatter(@NonNull Repository repository) {

    DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);

    diffFormatter.setRepository(repository);
    diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
    diffFormatter.setDetectRenames(true);

    return diffFormatter;
  }

  private static @NonNull RevCommit resolvePreviousCommit(@NonNull Repository repository, @NonNull RevCommit commit)
      throws Exception {

    Supplier<ObjectId> headObjectIdSupplier = () -> {
      try {
        return repository.resolve(Constants.HEAD);
      }
      catch (Exception cause) {
        throw new GitException("Failed to resolve ObjectId for HEAD", cause);
      }
    };

    ObjectId previousCommitId = Utils.get(repository.resolve(commit.name().concat("~1")), headObjectIdSupplier);

    return new RevWalk(repository).parseCommit(previousCommitId);
  }

  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
  static class CommitterIdentity {

    static @NonNull CommitterIdentity of(@Nullable PersonIdent authorIdentity,
        @Nullable PersonIdent committerIdentity) {

      Assert.isTrue(authorIdentity != null || committerIdentity != null,
        () -> String.format("Either Author Identity [%s] or Commit Identity [%s] is required",
          authorIdentity, committerIdentity));

      return new CommitterIdentity(authorIdentity, committerIdentity);
    }

    private final PersonIdent authorIdentity;
    private final PersonIdent committerIdentity;

    protected @NonNull PersonIdent resolveIdentity() {
      return this.committerIdentity != null ? this.committerIdentity : this.authorIdentity;
    }

    public String getEmailAddress() {
      return resolveIdentity().getEmailAddress();
    }

    public String getName() {
      return resolveIdentity().getName();
    }

    public Instant getWhen() {
      return resolveIdentity().getWhenAsInstant();
    }

    public LocalDateTime resolveCommitDateTime(@NonNull RevCommit commit) {

      LocalDateTime authorDateTime = nullSafeIdentityTime(getAuthorIdentity());
      LocalDateTime committerDateTime = nullSafeIdentityTime(getCommitterIdentity());
      LocalDateTime commitDateTime = toLocalDateTime(commit.getCommitTime());

      return earliest(authorDateTime, committerDateTime, commitDateTime);
    }

    private @NonNull LocalDateTime earliest(LocalDateTime... datesTimes) {

      return Arrays.stream(datesTimes)
        .reduce((one, two) -> one.isBefore(two) ? one : two)
        .stream()
        .findFirst()
        .orElseThrow();
    }

    private @NonNull LocalDateTime nullSafeIdentityTime(@Nullable PersonIdent personIdentity) {
      return personIdentity != null ? toLocalDateTime(personIdentity.getWhenAsInstant()) : LocalDateTime.now();
    }

    private @NonNull LocalDateTime toLocalDateTime(@Nullable Instant instant) {
      return instant != null ? toLocalDateTime(instant.getEpochSecond()) : LocalDateTime.now();
    }

    private @NonNull LocalDateTime toLocalDateTime(long seconds) {

      return Instant.ofEpochMilli(TimeUnit.SECONDS.toMillis(seconds))
        .atZone(ZoneOffset.systemDefault())
        .toLocalDateTime();
    }
  }
}
