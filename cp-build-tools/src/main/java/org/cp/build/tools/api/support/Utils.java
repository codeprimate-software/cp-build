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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.StringUtils;

/**
 * Abstract utility class containing common functions used by the Maven-based Codeprimate Build project.
 *
 * @author John Blum
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public abstract class Utils {

  public static final File USER_HOME_DIRECTORY = new File(System.getProperty("user.home"));
  public static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));

  public static final File[] EMPTY_FILE_ARRAY = new File[0];

  public static final String COMMA = ",";
  public static final String DASH = "-";
  public static final String DOUBLE_DASH = "--";
  public static final String EMPTY_STRING = "";
  public static final String FORWARD_SLASH = "/";
  public static final String LINE_SEPARATOR = System.lineSeparator();
  public static final String NEW_LINE = "\n";
  public static final String NEW_LINE_REGEX = "\\n";
  public static final String PERCENT = "%";
  public static final String PERIOD = ".";
  public static final String PIPE_SEPARATOR = "|";
  public static final String PIPE_SEPARATOR_REGEX = "\\|";
  public static final String SINGLE_SPACE = " ";
  public static final String TAB = "\t";
  public static final String NEW_LINE_TAB = LINE_SEPARATOR.concat(TAB);

  public static final String[] SPACES = {
    " ",
    "  ",
    "   ",
    "    ",
    "     ",
    "      ",
    "       ",
    "        ",
    "         ",
    "          ",
  };

  public static LocalDateTime atEpoch() {
    return LocalDateTime.of(1970, 1, 1 , 0, 0, 0);
  }

  public static <T> T get( T value, Supplier<T> defaultValue) {
    return value != null ? value : defaultValue.get();
  }

  public static int getInt(int value, Supplier<Integer> intSupplier) {
    return value != 0 ? value : intSupplier.get();
  }

  public static String getString(String value, Supplier<String> stringSupplier) {
    return StringUtils.hasText(value) ? value : stringSupplier.get();
  }

  public static String getSpaces(int length) {

    length = Math.abs(length);

    if (length < 1) {
      return EMPTY_STRING;
    }
    else if (length <= 10) {
      return SPACES[length - 1];
    }
    else {

      StringBuilder spaces = new StringBuilder();

      for (int count = length, amount = SPACES.length; count > 0; count -= amount, amount = Math.min(SPACES.length, count)) {
        int index = amount - 1;
        spaces.append(SPACES[index]);
      }

      return spaces.toString();
    }
  }

  public static boolean isSet(String... values) {
    return Arrays.stream(values).anyMatch(StringUtils::hasText);
  }

  public static boolean isNotSet(String... values) {
    return !isSet(values);
  }

  public static int negate(int compareResult) {
    return Integer.compare(0, compareResult);
  }

  public static String newLine() {
    return LINE_SEPARATOR;
  }

  public static String newLineAfter( String text) {
    return nullSafeTrimmedString(text).concat(newLine());
  }

  public static String newLineBefore( String text) {
    return newLine().concat(nullSafeTrimmedString(text));
  }

  public static String newLineBeforeAfter( String text) {
    return newLine().concat(newLineAfter(text));
  }

  public static boolean nullSafeIsDirectory( Object target) {
    return (target instanceof File file) && file.isDirectory();
  }

  public static boolean nullSafeIsFile( Object target) {
    return (target instanceof File file) && file.isFile();
  }

  public static File[] nullSafeFileArray( File[] fileArray) {
    return fileArray != null ? fileArray : EMPTY_FILE_ARRAY;
  }

  public static <T> Iterable<T> nullSafeIterable( Iterable<T> iterable) {
    return iterable != null ? iterable : Collections::emptyIterator;
  }

  public static <T> Predicate<T> nullSafeMatchingPredicate( Predicate<T> predicate) {
    return predicate != null ? predicate : argument -> true;
  }

  public static <T> Predicate<T> nullSafeNonMatchingPredicate( Predicate<T> predicate) {
    return predicate != null ? predicate : argument -> false;
  }

  @SuppressWarnings("all")
  public static String nullSafeFormatString( String target, int length) {

    String nonNullString = nullSafeTrimmedString(target);

    if (nonNullString.length() < length) {
      for (int size = nonNullString.length(); size < length; size++) {
        nonNullString += SINGLE_SPACE;
      }
    }

    return nonNullString.substring(0, Math.min(nonNullString.length(), length));
  }

  public static String nullSafeToString( Object target) {
    return target != null ? target.toString() : EMPTY_STRING;
  }

  public static String nullSafeTrimmedString( String target) {
    return target != null ? target.trim() : EMPTY_STRING;
  }

  public static String padRight(String value, int totalLength) {
    int valueLength = value != null ? value.length() : 0;
    String paddedValue = String.valueOf(value).concat(" ".repeat(Math.max(totalLength - valueLength, 0)));
    return paddedValue.substring(0, Math.min(paddedValue.length(), totalLength));
  }

  public static <T> T requireObject(T object, String message, Object... arguments) {
    return requireObject(object, toSupplier(message.formatted(arguments)));
  }

  public static <T> T requireObject(T object, Supplier<String> message) {

    if (object == null) {
      throw new IllegalArgumentException(message.get());
    }

    return object;
  }

  public static <T> T requireState(T object, String message, Object... arguments) {
    return requireState(object, toSupplier(message.formatted(arguments)));
  }

  public static <T> T requireState(T object, Supplier<String> message) {

    if (object == null) {
      throw new IllegalStateException(message.get());
    }

    return object;
  }

  public static <T> Stream<T> stream( Iterable<T> iterable) {
    return StreamSupport.stream(nullSafeIterable(iterable).spliterator(), false);
  }

  public static LocalDateTime today() {
    return LocalDateTime.now();
  }

  public static LocalDateTime tomorrow() {
    return LocalDateTime.now().plusDays(1L);
  }

  public static <T> Supplier<T> toSupplier( T target) {
    return () -> target;
  }
}
