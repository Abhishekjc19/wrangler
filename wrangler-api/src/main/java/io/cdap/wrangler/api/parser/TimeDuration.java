/*
 * Copyright © 2024 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a time duration value with unit conversion capabilities.
 * Supports units: ms (milliseconds), s (seconds), m (minutes), h (hours), d (days)
 */
public class TimeDuration implements Token {
  private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)(ms|[smhd])");
  private final Duration duration;

  public TimeDuration(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Time duration value cannot be null or empty");
    }

    Matcher matcher = DURATION_PATTERN.matcher(value.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid duration format: " + value);
    }

    long amount = Long.parseLong(matcher.group(1));
    if (amount < 0) {
      throw new IllegalArgumentException("Duration cannot be negative: " + value);
    }

    String unit = matcher.group(2);
    switch (unit) {
      case "ms":
        this.duration = Duration.ofMillis(amount);
        break;
      case "s":
        this.duration = Duration.ofSeconds(amount);
        break;
      case "m":
        this.duration = Duration.ofMinutes(amount);
        break;
      case "h":
        this.duration = Duration.ofHours(amount);
        break;
      case "d":
        this.duration = Duration.ofDays(amount);
        break;
      default:
        throw new IllegalArgumentException("Unsupported duration unit: " + unit);
    }
  }

  /**
   * Returns the duration in milliseconds.
   */
  public long toMillis() {
    return duration.toMillis();
  }

  /**
   * Returns the duration in seconds.
   */
  public long toSeconds() {
    return duration.getSeconds();
  }

  /**
   * Returns the duration in minutes.
   */
  public long toMinutes() {
    return duration.toMinutes();
  }

  /**
   * Returns the duration in hours.
   */
  public long toHours() {
    return duration.toHours();
  }

  /**
   * Returns the duration in days.
   */
  public long toDays() {
    return duration.toDays();
  }

  /**
   * Converts the duration to a string representation in the specified unit.
   * @param unit The target unit (ms, s, m, h, d)
   * @return The duration in the specified unit
   */
  public String toUnit(String unit) {
    if (unit == null || unit.trim().isEmpty()) {
      throw new IllegalArgumentException("Unit cannot be null or empty");
    }

    double value;
    switch (unit.toLowerCase()) {
      case "ms":
        value = toMillis();
        break;
      case "s":
        value = toSeconds();
        break;
      case "m":
        value = toMinutes();
        break;
      case "h":
        value = toHours();
        break;
      case "d":
        value = toDays();
        break;
      default:
        throw new IllegalArgumentException("Unsupported duration unit: " + unit);
    }
    return String.format("%.2f%s", value, unit);
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public Object value() {
    return duration;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.TIME_DURATION.name());
    object.addProperty("value", duration.toMillis());
    return object;
  }
} 