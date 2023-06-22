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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * Unit Tests for {@link Utils}.
 *
 * @author John Blum
 * @see org.junit.jupiter.api.Test
 * @see org.mockito.Mockito
 * @see org.cp.build.tools.core.support.Utils
 * @since 2.0.0
 */
public class UtilsUnitTests {

  @Test
  @SuppressWarnings("unchecked")
  public void getReturnsNonNullValue() {

    Supplier<Object> mockSupplier = mock(Supplier.class);

    assertThat(Utils.get("test", mockSupplier)).isEqualTo("test");

    verifyNoInteractions(mockSupplier);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void getReturnsSuppliedValue() {

    Supplier<Object> mockSupplier = mock(Supplier.class);

    doReturn("mock").when(mockSupplier).get();

    assertThat(Utils.get(null, mockSupplier)).isEqualTo("mock");

    verify(mockSupplier, times(1)).get();
    verifyNoMoreInteractions(mockSupplier);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void getIntReturnsValue() {

    Supplier<Integer> mockSupplier = mock(Supplier.class);

    assertThat(Utils.getInt(1, mockSupplier)).isOne();
    assertThat(Utils.getInt(-1, mockSupplier)).isEqualTo(-1);
    assertThat(Utils.getInt(10, mockSupplier)).isEqualTo(10);

    verifyNoInteractions(mockSupplier);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void getIntReturnsSuppliedInt() {

    Supplier<Integer> mockSupplier = mock(Supplier.class);

    doReturn(101).when(mockSupplier).get();

    assertThat(Utils.getInt(0, mockSupplier)).isEqualTo(101);

    verify(mockSupplier, times(1)).get();
    verifyNoInteractions(mockSupplier);
  }

  @Test
  public void invertIsCorrect() {

    assertThat(Utils.invert(1)).isLessThan(0);
    assertThat(Utils.invert(-1)).isGreaterThan(0);
    assertThat(Utils.invert(0)).isZero();
  }

  @Test
  public void nullSafeIsDirectoryWithDirectory() {

    File mockDirectory = mock(File.class);

    doReturn(true).when(mockDirectory).isDirectory();

    assertThat(Utils.nullSafeIsDirectory(mockDirectory)).isTrue();

    verify(mockDirectory, times(1)).isDirectory();
    verifyNoMoreInteractions(mockDirectory);
  }

  @Test
  public void nullSafeIsDirectoryWithFile() {

    File mockDirectory = mock(File.class);

    doReturn(false).when(mockDirectory).isDirectory();

    assertThat(Utils.nullSafeIsDirectory(mockDirectory)).isFalse();

    verify(mockDirectory, times(1)).isDirectory();
    verifyNoMoreInteractions(mockDirectory);
  }

  @Test
  public void nullSafeIsDirectoryWithNull() {
    assertThat(Utils.nullSafeIsDirectory("/path/to//directory")).isFalse();
  }

  @Test
  public void nullSafeIsDirectoryWithObject() {
    assertThat(Utils.nullSafeIsDirectory("/path/to//directory")).isFalse();
  }

  @Test
  public void nullSafeIsFileWithFile() {

    File mockFile = mock(File.class);

    doReturn(true).when(mockFile).isFile();

    assertThat(Utils.nullSafeIsFile(mockFile)).isTrue();

    verify(mockFile, times(1)).isFile();
    verifyNoMoreInteractions(mockFile);
  }

  @Test
  public void nullSafeIsFileWithNonFile() {

    File mockFile = mock(File.class);

    doReturn(false).when(mockFile).isFile();

    assertThat(Utils.nullSafeIsFile(mockFile)).isFalse();

    verify(mockFile, times(1)).isFile();
    verifyNoMoreInteractions(mockFile);
  }

  @Test
  public void nullSafeIsFileWithNull() {
    assertThat(Utils.nullSafeIsFile(null)).isFalse();
  }

  @Test
  public void nullSafeIsFileWithObject() {
    assertThat(Utils.nullSafeIsFile(new Object())).isFalse();
  }

  @Test
  public void nullSafeFileArrayWithFileArray() {

    File[] array = new File[0];

    assertThat(Utils.nullSafeFileArray(array)).isSameAs(array);
  }

  @Test
  public void nullSafeFileArrayWithNullFileArray() {
    assertThat(Utils.nullSafeFileArray(null)).isNotNull().isEmpty();
  }

  @Test
  public void nullSafeIterableWithIterable() {

    Iterable<?> mockIterable = mock(Iterable.class);

    assertThat(Utils.nullSafeIterable(mockIterable)).isSameAs(mockIterable);

    verifyNoInteractions(mockIterable);
  }

  @Test
  public void nullSafeIterableWithNullIterable() {
    assertThat(Utils.nullSafeIterable(null)).isNotNull().isEmpty();
  }

  @Test
  public void nullSafeMatchingPredicateWithPredicate() {

    Predicate<?> mockPredicate = mock(Predicate.class);

    assertThat(Utils.nullSafeMatchingPredicate(mockPredicate)).isSameAs(mockPredicate);

    verifyNoInteractions(mockPredicate);
  }

  @Test
  public void nullSafeMatchingPredicateWithNullPredicate() {

    Predicate<Object> predicate = Utils.nullSafeMatchingPredicate(null);

    assertThat(predicate).isNotNull();
    assertThat(predicate.test("mock")).isTrue();
  }

  @Test
  public void nullSafeNonMatchingPredicateWithPredicate() {

    Predicate<?> mockPredicate = mock(Predicate.class);

    assertThat(Utils.nullSafeNonMatchingPredicate(mockPredicate)).isSameAs(mockPredicate);

    verifyNoInteractions(mockPredicate);
  }

  @Test
  public void nullSafeNonMatchingPredicateWithNullPredicate() {

    Predicate<Object> predicate = Utils.nullSafeNonMatchingPredicate(null);

    assertThat(predicate).isNotNull();
    assertThat(predicate.test("mock")).isFalse();
  }

  @Test
  public void nullSafeFormatStringWithNonNullStringDoesNotAlterString() {
    assertThat(Utils.nullSafeFormatString("test", 4)).isEqualTo("test");
  }

  @Test
  public void nullSafeFormatStringWithNonNullStringPadsString() {
    assertThat(Utils.nullSafeFormatString("test", 10)).isEqualTo("test      ");
  }

  @Test
  public void nullSafeFormatStringWithNonNullStringTruncatesString() {
    assertThat(Utils.nullSafeFormatString("testing", 4)).isEqualTo("test");
  }

  @Test
  public void nullSafeFormatStringWithNullString() {
    assertThat(Utils.nullSafeFormatString(null, 2)).isEqualTo("  ");
  }

  @Test
  public void nullSafeToStringWithNonNullObject() {

    assertThat(Utils.nullSafeToString("test")).isEqualTo("test");
    assertThat(Utils.nullSafeToString(" mock  ")).isEqualTo(" mock  ");
    assertThat(Utils.nullSafeToString(true)).isEqualTo("true");
    assertThat(Utils.nullSafeToString(1)).isEqualTo("1");
    assertThat(Utils.nullSafeToString(3.14159)).isEqualTo("3.14159");
  }

  @Test
  public void nullSafeToStringWithNullObject() {
    assertThat(Utils.nullSafeToString(null)).isEqualTo(Utils.EMPTY_STRING);
  }

  @Test
  public void nullSafeTrimmedStringWithNonNullStrings() {

    assertThat(Utils.nullSafeTrimmedString("test")).isEqualTo("test");
    assertThat(Utils.nullSafeTrimmedString("  test")).isEqualTo("test");
    assertThat(Utils.nullSafeTrimmedString("test  ")).isEqualTo("test");
    assertThat(Utils.nullSafeTrimmedString(" test  ")).isEqualTo("test");
    assertThat(Utils.nullSafeTrimmedString(" m o c k  ")).isEqualTo("m o c k");
  }

  @Test
  public void nullSafeTrimmedStringWithNullString() {
    assertThat(Utils.nullSafeTrimmedString(null)).isNotNull().isEmpty();
  }

  @Test
  public void requireObjectWithNonNullObject() {
    assertThat(Utils.requireObject("test", "Object is required")).isEqualTo("test");
  }

  @Test
  @SuppressWarnings("all")
  public void requireObjectWithNullObject() {

    assertThatIllegalArgumentException()
      .isThrownBy(() -> Utils.requireObject(null, "Object is %s", "required"))
      .withMessage("Object is required")
      .withNoCause();
  }

  @Test
  public void requireStateWithState() {
    assertThat(Utils.requireState("test", "No State")).isEqualTo("test");
  }

  @Test
  @SuppressWarnings("all")
  public void requireSateWithNoState() {

    assertThatIllegalStateException()
      .isThrownBy(() -> Utils.requireState(null, "%s state present", "No"))
      .withMessage("No state present")
      .withNoCause();
  }

  @Test
  public void toSupplierWithValue() {

    assertThat(Utils.toSupplier("test")).isNotNull()
      .extracting(Supplier::get)
      .isEqualTo("test");
  }

  @Test
  public void toSupplierWithNullValue() {

    assertThat(Utils.toSupplier(null)).isNotNull()
      .extracting(Supplier::get)
      .isNull();
  }
}
