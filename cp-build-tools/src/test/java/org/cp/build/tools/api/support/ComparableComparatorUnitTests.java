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
package org.cp.build.tools.api.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.lang.NonNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Unit Tests for {@link ComparableComparator}.
 *
 * @author John Blum
 * @see org.junit.jupiter.api.Test
 * @see org.cp.build.tools.api.support.ComparableComparator
 * @since 2.0.0
 */
public class ComparableComparatorUnitTests {

  @Test
  @SuppressWarnings("all")
  public void compareToSameComparableObject() {

    Integer ONE = 1;

    assertThat(ComparableComparator.INSTANCE.compare(ONE, ONE)).isZero();
    assertThat(ComparableComparator.INSTANCE.compare("test", "test")).isZero();
  }

  @Test
  public void compareWithNullFirstArgument() {
    assertThat(ComparableComparator.INSTANCE.compare(null, "test")).isGreaterThan(0);
  }

  @Test
  public void compareWithNullSecondArgument() {
    assertThat(ComparableComparator.INSTANCE.compare("test", null)).isLessThan(0);
  }

  @Test
  public void compareWithEqualObjects() {
    assertThat(ComparableComparator.INSTANCE.compare(User.as("Jon Doe"), User.as("Jon Doe"))).isZero();
  }

  @Test
  public void compareWithUnequalObjects() {

    User janeDoe = User.as("Jane Doe");
    User jonDoe = User.as("Jon Doe");
    User pieDoe = User.as("Pie Doe");

    assertThat(ComparableComparator.INSTANCE.compare(jonDoe, janeDoe)).isGreaterThan(0);
    assertThat(ComparableComparator.INSTANCE.compare(jonDoe, pieDoe)).isLessThan(0);
  }

  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor(staticName = "as")
  static class User implements Comparable<User> {

    private final String name;

    @Override
    public int compareTo(@NonNull User user) {
      return this.getName().compareTo(user.getName());
    }

    @Override
    public String toString() {
      return getName();
    }
  }
}
