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
 * @see <a href="https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/Colors.java">JLine Colors.java</a>
 * @see <a href="https://github.com/jline/jline3/blob/master/terminal/src/main/resources/org/jline/utils/colors.txt">JLine colors.txt</a>
 */
@SuppressWarnings("unused")
public enum Colors {

  AQUA(org.jline.utils.Colors.rgbColor("aqua")),
  BLACK(org.jline.utils.Colors.rgbColor("black")),
  BLUE(org.jline.utils.Colors.rgbColor("blue")),
  BROWN(org.jline.utils.Colors.rgbColor("sandybrown")),
  CYAN(org.jline.utils.Colors.rgbColor("cyan")),
  GOLD(org.jline.utils.Colors.rgbColor("gold1")),
  GREY(org.jline.utils.Colors.rgbColor("grey")),
  GREEN(org.jline.utils.Colors.rgbColor("green")),
  LIGHT_GREEN(org.jline.utils.Colors.rgbColor("lightgreen")),
  LIME(org.jline.utils.Colors.rgbColor("lime")),
  MAGENTA(org.jline.utils.Colors.rgbColor("magenta1")),
  OLIVE(org.jline.utils.Colors.rgbColor("olive")),
  ORANGE(org.jline.utils.Colors.rgbColor("orange1")),
  PINK(org.jline.utils.Colors.rgbColor("hotpink")),
  PLUM(org.jline.utils.Colors.rgbColor("plum1")),
  PURPLE(org.jline.utils.Colors.rgbColor("purple")),
  NAVY(org.jline.utils.Colors.rgbColor("navy")),
  RED(org.jline.utils.Colors.rgbColor("red")),
  SILVER(org.jline.utils.Colors.rgbColor("silver")),
  TAN(org.jline.utils.Colors.rgbColor("tan")),
  WHITE(org.jline.utils.Colors.rgbColor("white")),
  YELLOW(org.jline.utils.Colors.rgbColor("yellow")),
  DEFAULT(BLACK.value);

  private final int value;

  Colors(int value) {
    this.value = value;
  }

  public int toJLineAttributeStyleColor() {
    return this.value;
  }
}
