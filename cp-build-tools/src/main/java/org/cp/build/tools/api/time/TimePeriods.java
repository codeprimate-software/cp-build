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
package org.cp.build.tools.api.time;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cp.build.tools.api.support.Utils;
import org.cp.build.tools.api.time.TimePeriods.DateRange;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Abstract Data Type (ADT) modeling {@literal periods of time}.
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @see java.time.LocalDate
 * @since 2.0.0
 */
@Getter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class TimePeriods implements Iterable<DateRange> {

  public static @NonNull TimePeriods empty() {
    return new TimePeriods();
  }

  public static @NonNull TimePeriods of(DateRange... dateRanges) {

    return of(Arrays.stream(dateRanges)
      .filter(Objects::nonNull)
      .collect(Collectors.toList()));
  }

  public static @NonNull TimePeriods of(@NonNull Iterable<DateRange> dateRanges) {

    TimePeriods timePeriods = new TimePeriods();

    Utils.stream(dateRanges)
      .filter(Objects::nonNull)
      .forEach(timePeriods.getDateRanges()::add);

    return timePeriods;
  }

  // Useful for Holidays (for example: Thanksgiving, Christmas, etc)
  public static @NonNull TimePeriods ofSingleDates(LocalDate... dates) {

    TimePeriods timePeriods = new TimePeriods();

    Arrays.stream(dates)
      .filter(Objects::nonNull)
      .map(DateRange::forSingleDate)
      .forEach(timePeriods.getDateRanges()::add);

    return timePeriods;
  }

  public static @NonNull TimePeriods parse(@NonNull String dates) {

    String[] splitDates = Utils.nullSafeTrimmedString(dates).split(Utils.COMMA);

    return of(Arrays.stream(splitDates)
      .filter(StringUtils::hasText)
      .map(DateRange::parse)
      .toList());
  }

  private final Set<DateRange> dateRanges = new HashSet<>();

  @SuppressWarnings("all")
  public boolean isDuring(@NonNull LocalDate date) {
    return date != null && stream().anyMatch(dateRange -> dateRange.isDuring(date));
  }

  public @NonNull Predicate<LocalDate> asPredicate() {
    return this::isDuring;
  }

  @Override
  public @NonNull Iterator<DateRange> iterator() {
    return Collections.unmodifiableSet(getDateRanges()).iterator();
  }

  public @NonNull Stream<DateRange> stream() {
    return Utils.stream(this);
  }

  @Getter
  protected static class DateRange {

    protected static final String DATE_PATTERN = "yyyy-MM-dd";

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    protected static @NonNull DateRange.Builder from(@NonNull LocalDate start) {
      return new DateRange.Builder(start);
    }

    protected static @NonNull DateRange forSingleDate(@NonNull LocalDate localDate) {
      return from(localDate).build();
    }

    protected static @NonNull DateRange parse(@NonNull String date) {

      String nonNullTrimmedDate = Utils.nullSafeTrimmedString(date);

      try {

        String[] dates = nonNullTrimmedDate.split(Utils.DOUBLE_DASH);

        Assert.notEmpty(dates, "Date(s) [%s] to parse are required");

        LocalDate start = LocalDate.parse(dates[0], DATE_FORMATTER);
        LocalDate end = dates.length > 1 ? LocalDate.parse(dates[1], DATE_FORMATTER) : null;

        return from(start).to(end).build();
      }
      catch (DateTimeParseException cause) {
        throw new IllegalArgumentException("Date(s) [%s] are not valid".formatted(date));
      }
    }

    private final LocalDate start;
    private final LocalDate end;

    protected DateRange(@NonNull LocalDate start, @NonNull LocalDate end) {

      Assert.notNull(start, "Start of DateRange is required");
      Assert.notNull(end, "End of DateRange is required");

      Assert.isTrue(!start.isAfter(end), () -> "Start Date [%s] must be on or before End Date [%s]"
        .formatted(start.format(DATE_FORMATTER), end.format(DATE_FORMATTER)));

      this.start = start;
      this.end = end;
    }

    @SuppressWarnings("all")
    protected boolean isDuring(@NonNull LocalDate date) {
      return date != null && !(date.isBefore(getStart()) || date.isAfter(getEnd()));
    }

    @Getter(AccessLevel.PROTECTED)
    protected static class Builder {

      private final LocalDate start;

      private LocalDate end;

      protected Builder(@NonNull LocalDate start) {
        this.start = start;
      }

      protected Builder to(@Nullable LocalDate end) {
        this.end = end;
        return this;
      }

      protected @NonNull DateRange build() {

        LocalDate start = getStart();
        LocalDate end = getEnd();

        return end != null ? new DateRange(start, end) : new DateRange(start, start);
      }
    }
  }
}
