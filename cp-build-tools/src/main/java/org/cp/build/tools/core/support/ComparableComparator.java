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
package org.cp.build.tools.core.support;

import java.util.Comparator;

/**
 * Null-safe {@link Comparator} implementation that compares {@link Comparable objects}.
 *
 * @author John Blum
 * @see java.lang.Comparable
 * @see java.util.Comparator
 * @since 2.0.0
 */
@SuppressWarnings("rawtypes")
public class ComparableComparator implements Comparator<Comparable>  {

  public static ComparableComparator INSTANCE = new ComparableComparator();

  @Override
  @SuppressWarnings("unchecked")
  public int compare(Comparable one, Comparable two) {

    return one == two ? 0
      : one == null ? 1
      : two == null ? -1
      : one.compareTo(two);
  }
}
