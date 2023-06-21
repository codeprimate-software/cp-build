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

import java.io.File;

/**
 * Abstract utility class containing common functions.
 *
 * @author John Blum
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public abstract class Utils {

  public static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));

  public static final File[] EMPTY_FILE_ARRAY = new File[0];

  public static final String EMPTY_STRING = "";
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  public static String newLine() {
    return LINE_SEPARATOR;
  }

  public static File[] nullSafeFileArray(File[] fileArray) {
    return fileArray != null ? fileArray : EMPTY_FILE_ARRAY;
  }

  public static boolean nullSafeIsDirectory(Object target) {
    return (target instanceof File file) && file.isDirectory();
  }

  public static boolean nullSafeIsFile(Object target) {
    return (target instanceof File file) && file.isFile();
  }

  public static String nullSafeTrimmedString(String target) {
    return target != null ? target.trim() : EMPTY_STRING;
  }
}
