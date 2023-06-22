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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit Tests for {@link Project.Version}
 *
 * @author John Blum
 * @see org.junit.jupiter.api.Test
 * @see org.cp.build.tools.api.model.Project.Version
 * @since 2.0.0
 */
public class ProjectVersionUnitTests {

  @Test
  public void compareToIsCorrect() {

    Project.Version oneZeroZeroRelease = Project.Version.of(1, 0, 0);
    Project.Version oneZeroZeroMilestoneOne = Project.Version.of(1, 0, 0).withQualifier("M1");
    Project.Version oneZeroZeroMilestoneTwo = Project.Version.of(1, 0, 0).withQualifier("M2");
    Project.Version oneZeroZeroReleaseCandidateOne = Project.Version.of(1, 0, 0).withQualifier("RC1");
    Project.Version oneZeroZeroSnapshot = Project.Version.of(1, 0, 0).withQualifier("SNAPSHOT");
    Project.Version oneOneZeroMilestoneOne = Project.Version.of(1, 1, 0).withQualifier("M1");
    Project.Version twoZeroOneReleaseCandidateOne = Project.Version.of(2, 0, 1).withQualifier("RC1");
    Project.Version threeZeroZeroSnapshot = Project.Version.of(3, 0, 0).withQualifier("SNAPSHOT");

    List<Project.Version> versions = Arrays.asList(
      oneOneZeroMilestoneOne,
      oneZeroZeroMilestoneOne,
      oneZeroZeroMilestoneTwo,
      oneZeroZeroRelease,
      oneZeroZeroReleaseCandidateOne,
      twoZeroOneReleaseCandidateOne,
      oneZeroZeroSnapshot,
      threeZeroZeroSnapshot
    );

    assertThat(versions.stream().sorted().toList()).containsExactly(
      threeZeroZeroSnapshot,
      twoZeroOneReleaseCandidateOne,
      oneOneZeroMilestoneOne,
      oneZeroZeroRelease,
      oneZeroZeroReleaseCandidateOne,
      oneZeroZeroMilestoneTwo,
      oneZeroZeroMilestoneOne,
      oneZeroZeroSnapshot
    );
  }
}
