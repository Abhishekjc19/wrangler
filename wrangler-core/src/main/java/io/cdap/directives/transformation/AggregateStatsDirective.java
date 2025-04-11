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

package io.cdap.directives.transformation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.lineage.Lineage;
import io.cdap.wrangler.api.lineage.Mutation;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * A directive for aggregating byte sizes and time durations.
 * Supports byte size units: B, KB, MB, GB, TB, PB
 * Supports time duration units: ms, s, m, h, d
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Categories(categories = {"transformation"})
@Description("Aggregates byte sizes and time durations from source columns into target columns.")
public class AggregateStatsDirective implements Directive, Lineage {
  public static final String NAME = "aggregate-stats";

  private String sourceSizeColumn;
  private String sourceTimeColumn;
  private String targetSizeColumn;
  private String targetTimeColumn;
  private String sizeUnit;
  private String timeUnit;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("source-size-column", TokenType.COLUMN_NAME);
    builder.define("source-time-column", TokenType.COLUMN_NAME);
    builder.define("target-size-column", TokenType.COLUMN_NAME);
    builder.define("target-time-column", TokenType.COLUMN_NAME);
    builder.define("size-unit", TokenType.TEXT, Optional.TRUE);
    builder.define("time-unit", TokenType.TEXT, Optional.TRUE);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.sourceSizeColumn = ((ColumnName) args.value("source-size-column")).value();
    this.sourceTimeColumn = ((ColumnName) args.value("source-time-column")).value();
    this.targetSizeColumn = ((ColumnName) args.value("target-size-column")).value();
    this.targetTimeColumn = ((ColumnName) args.value("target-time-column")).value();
    
    if (args.contains("size-unit")) {
      this.sizeUnit = ((Text) args.value("size-unit")).value();
      validateByteSizeUnit(this.sizeUnit);
    } else {
      this.sizeUnit = "MB";
    }
    
    if (args.contains("time-unit")) {
      this.timeUnit = ((Text) args.value("time-unit")).value();
      validateTimeUnit(this.timeUnit);
    } else {
      this.timeUnit = "s";
    }
  }

  private void validateByteSizeUnit(String unit) throws DirectiveParseException {
    if (unit == null || unit.trim().isEmpty()) {
      throw new DirectiveParseException("Byte size unit cannot be null or empty");
    }
    String upperUnit = unit.toUpperCase();
    if (!upperUnit.matches("B|KB|MB|GB|TB|PB")) {
      throw new DirectiveParseException("Invalid byte size unit: " + unit + 
        ". Supported units are: B, KB, MB, GB, TB, PB");
    }
  }

  private void validateTimeUnit(String unit) throws DirectiveParseException {
    if (unit == null || unit.trim().isEmpty()) {
      throw new DirectiveParseException("Time unit cannot be null or empty");
    }
    String lowerUnit = unit.toLowerCase();
    if (!lowerUnit.matches("ms|s|m|h|d")) {
      throw new DirectiveParseException("Invalid time unit: " + unit + 
        ". Supported units are: ms, s, m, h, d");
    }
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    if (rows == null || rows.isEmpty()) {
      throw new DirectiveExecutionException("No input rows provided");
    }

    long totalBytes = 0;
    long totalMilliseconds = 0;
    int count = 0;

    for (Row row : rows) {
      Object sizeValue = row.getValue(sourceSizeColumn);
      Object timeValue = row.getValue(sourceTimeColumn);

      if (sizeValue != null) {
        try {
          ByteSize byteSize = new ByteSize(sizeValue.toString());
          totalBytes += byteSize.getBytes();
        } catch (IllegalArgumentException e) {
          throw new DirectiveExecutionException(
            String.format("Invalid byte size value in column '%s': %s", sourceSizeColumn, sizeValue), e);
        }
      }

      if (timeValue != null) {
        try {
          TimeDuration timeDuration = new TimeDuration(timeValue.toString());
          totalMilliseconds += timeDuration.toMillis();
        } catch (IllegalArgumentException e) {
          throw new DirectiveExecutionException(
            String.format("Invalid time duration value in column '%s': %s", sourceTimeColumn, timeValue), e);
        }
      }

      count++;
    }

    // Create a new row with aggregated values
    Row result = new Row();
    if (count > 0) {
      try {
        ByteSize totalSize = new ByteSize(totalBytes + "B");
        TimeDuration totalTime = new TimeDuration(totalMilliseconds + "ms");
        
        result.add(targetSizeColumn, totalSize.toUnit(sizeUnit));
        result.add(targetTimeColumn, totalTime.toUnit(timeUnit));
      } catch (IllegalArgumentException e) {
        throw new DirectiveExecutionException("Error converting aggregated values to specified units", e);
      }
    }

    List<Row> results = new ArrayList<>();
    results.add(result);
    return results;
  }

  @Override
  public Mutation lineage() {
    return Mutation.builder()
      .readable("Aggregated byte sizes and time durations from columns '%s' and '%s' into '%s' and '%s'",
        sourceSizeColumn, sourceTimeColumn, targetSizeColumn, targetTimeColumn)
      .build();
  }
} 