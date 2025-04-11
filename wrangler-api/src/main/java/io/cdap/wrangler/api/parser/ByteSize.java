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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a byte size value with unit conversion capabilities.
 * Supports units: B, KB, MB, GB, TB, PB
 */
public class ByteSize implements Token {
  private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("(\\d+)([KMGTP]?B)");
  private final long bytes;

  public ByteSize(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Byte size value cannot be null or empty");
    }

    Matcher matcher = BYTE_SIZE_PATTERN.matcher(value.toUpperCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid byte size format: " + value);
    }

    long size = Long.parseLong(matcher.group(1));
    if (size < 0) {
      throw new IllegalArgumentException("Byte size cannot be negative: " + value);
    }

    String unit = matcher.group(2);
    switch (unit) {
      case "B":
        this.bytes = size;
        break;
      case "KB":
        this.bytes = size * 1024L;
        break;
      case "MB":
        this.bytes = size * 1024L * 1024;
        break;
      case "GB":
        this.bytes = size * 1024L * 1024 * 1024;
        break;
      case "TB":
        this.bytes = size * 1024L * 1024 * 1024 * 1024;
        break;
      case "PB":
        this.bytes = size * 1024L * 1024 * 1024 * 1024 * 1024;
        break;
      default:
        throw new IllegalArgumentException("Unsupported byte size unit: " + unit);
    }
  }

  /**
   * Returns the size in bytes.
   */
  public long getBytes() {
    return bytes;
  }

  /**
   * Converts the size to the specified unit.
   * @param unit The target unit (B, KB, MB, GB, TB, PB)
   * @return The size in the specified unit
   */
  public String toUnit(String unit) {
    if (unit == null || unit.trim().isEmpty()) {
      throw new IllegalArgumentException("Unit cannot be null or empty");
    }

    double value;
    switch (unit.toUpperCase()) {
      case "B":
        value = bytes;
        break;
      case "KB":
        value = bytes / 1024.0;
        break;
      case "MB":
        value = bytes / (1024.0 * 1024);
        break;
      case "GB":
        value = bytes / (1024.0 * 1024 * 1024);
        break;
      case "TB":
        value = bytes / (1024.0 * 1024 * 1024 * 1024);
        break;
      case "PB":
        value = bytes / (1024.0 * 1024 * 1024 * 1024 * 1024);
        break;
      default:
        throw new IllegalArgumentException("Unsupported byte size unit: " + unit);
    }
    return String.format("%.2f%s", value, unit);
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public Object value() {
    return bytes;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.BYTE_SIZE.name());
    object.addProperty("value", bytes);
    return object;
  }
} 