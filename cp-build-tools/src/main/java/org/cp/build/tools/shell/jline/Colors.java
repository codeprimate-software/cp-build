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
package org.cp.build.tools.shell.jline;

/**
 * Java {@link Enum Enumeration} of colors supported by {@literal JLine}.
 *
 * @author John Blum
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public enum Colors {

  BLACK(0),
  RED(1),
  GREEN(2),
  YELLOW(3),
  BLUE(4),
  MAGENTA(5),
  CYAN(6),
  WHITE(7),
  BRIGHT(8),
  DEFAULT(BLACK.value);

  private final int value;

  Colors(int value) {
    this.value = value;
  }

  public int toJLineAttributeStyleColor() {
    return this.value;
  }
}
