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

import java.io.File;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotNull;

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
  public static final String SINGLE_SPACE = " ";

  public static <T> T get(@Nullable T value, @NonNull Supplier<T> defaultValue) {
    return value != null ? value : defaultValue.get();
  }

  public static int getInt(int value, @NonNull Supplier<Integer> intSupplier) {
    return value != 0 ? value : intSupplier.get();
  }

  public static int invert(int compareResult) {
    return Integer.compare(0, compareResult);
  }

  public static String newLine() {
    return LINE_SEPARATOR;
  }

  public static boolean nullSafeIsDirectory(@Nullable Object target) {
    return (target instanceof File file) && file.isDirectory();
  }

  public static boolean nullSafeIsFile(@Nullable Object target) {
    return (target instanceof File file) && file.isFile();
  }

  public static @NonNull File[] nullSafeFileArray(@Nullable File[] fileArray) {
    return fileArray != null ? fileArray : EMPTY_FILE_ARRAY;
  }

  public static @NonNull <T> Iterable<T> nullSafeIterable(@Nullable Iterable<T> iterable) {
    return iterable != null ? iterable : Collections::emptyIterator;
  }

  public static @NonNull <T> Predicate<T> nullSafeMatchingPredicate(@Nullable Predicate<T> predicate) {
    return predicate != null ? predicate : argument -> true;
  }

  public static @NonNull <T> Predicate<T> nullSafeNonMatchingPredicate(@Nullable Predicate<T> predicate) {
    return predicate != null ? predicate : argument -> false;
  }

  @SuppressWarnings("all")
  public static @NotNull String nullSafeFormatString(@Nullable String target, int length) {

    String nonNullString = nullSafeTrimmedString(target);

    if (nonNullString.length() < length) {
      for (int size = nonNullString.length(); size < length; size++) {
        nonNullString += SINGLE_SPACE;
      }
    }

    return nonNullString.substring(0, Math.min(nonNullString.length(), length));
  }

  public static @NonNull String nullSafeToString(@Nullable Object target) {
    return target != null ? target.toString() : EMPTY_STRING;
  }

  public static @NonNull String nullSafeTrimmedString(@Nullable String target) {
    return target != null ? target.trim() : EMPTY_STRING;
  }

  public static @NonNull <T> T requireObject(T object, String message, Object... arguments) {
    return requireObject(object, toSupplier(String.format(message, arguments)));
  }

  public static @NonNull <T> T requireObject(T object, Supplier<String> message) {

    if (object == null) {
      throw new IllegalArgumentException(message.get());
    }

    return object;
  }

  public static @NonNull <T> T requireState(T object, String message, Object... arguments) {
    return requireState(object, toSupplier(String.format(message, arguments)));
  }

  public static @NonNull <T> T requireState(T object, Supplier<String> message) {

    if (object == null) {
      throw new IllegalStateException(message.get());
    }

    return object;
  }

  public static @NonNull <T> Supplier<T> toSupplier(@Nullable T target) {
    return () -> target;
  }
}
