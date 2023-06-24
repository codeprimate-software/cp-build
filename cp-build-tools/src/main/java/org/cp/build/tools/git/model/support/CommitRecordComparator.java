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
package org.cp.build.tools.git.model.support;

import java.util.Comparator;

import org.cp.build.tools.git.model.CommitRecord;
import org.springframework.lang.NonNull;

/**
 * Java {@link Comparator} implementation used to compare {@link CommitRecord CommitRecords}.
 *
 * @author John Blum
 * @see java.util.Comparator
 * @see org.cp.build.tools.git.model.CommitRecord
 * @since 2.0.0
 */
public class CommitRecordComparator implements Comparator<CommitRecord> {

  public static final CommitRecordComparator INSTANCE = new CommitRecordComparator();

  @Override
  public int compare(@NonNull CommitRecord one, @NonNull CommitRecord two) {
    return one.getDateTime().compareTo(two.getDateTime());
  }
}
