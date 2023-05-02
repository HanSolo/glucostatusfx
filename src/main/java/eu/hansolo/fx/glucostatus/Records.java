/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Gerrit Grunwald.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.glucostatus;

import eu.hansolo.toolboxfx.geom.Rectangle;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

import static eu.hansolo.fx.glucostatus.Constants.FIELD_DATE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DATE_STRING;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DELTA;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DEVICE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DIRECTION;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_FILTERED;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_ID;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_NOISE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_RSSI;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_SGV;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_SYS_TIME;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_TREND;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_TYPE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_UNFILTERED;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_UTC_OFFSET;


public class Records {

    public record GlucoEntryDto(String _id, double sgv, long date, String dateString, int trend, String direction, String device, String type, int utcOffset, String sysTime) {
        public GlucoEntry getGlucoEntry() {
            return new GlucoEntry(this._id, this.sgv, this.date, OffsetDateTime.ofInstant(Instant.ofEpochSecond(this.date), ZoneId.systemDefault()), dateString, Trend.getFromText(Integer.toString(this.trend)), direction, device, type, this.utcOffset, 0, 0, 0, 0, 0, this.sysTime);
        }
    }

    public record GlucoEntry(String id, double sgv, long datelong, OffsetDateTime date, String dateString, Trend trend, String direction, String device, String type, int utcOffset, int noise, double filtered, double unfiltered, int rssi, double delta, String sysTime) implements Comparable<GlucoEntry> {

        @Override public int compareTo(final GlucoEntry other) { return Long.compare(datelong, other.datelong()); }

        @Override public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            GlucoEntry that = (GlucoEntry) o;
            return Double.compare(that.sgv, sgv) == 0 && datelong == that.datelong && utcOffset == that.utcOffset && noise == that.noise && Double.compare(that.filtered, filtered) == 0 && Double.compare(that.unfiltered, unfiltered) == 0 &&
                   rssi == that.rssi && Double.compare(that.delta, delta) == 0 && id.equals(that.id) && Objects.equals(date, that.date) && Objects.equals(dateString, that.dateString) && trend == that.trend &&
                   Objects.equals(direction, that.direction) && Objects.equals(device, that.device) && Objects.equals(type, that.type) && Objects.equals(sysTime, that.sysTime);
        }

        @Override public int hashCode() {
            return Objects.hash(id, sgv, datelong, date, dateString, trend, direction, device, type, utcOffset, noise, filtered, unfiltered, rssi, delta, sysTime);
        }

        @Override public String toString() {
            return new StringBuilder().append("{")
                                      .append("\"date\":\"").append(Constants.DTF.format(date)).append("\",")
                                      .append("\"sgv\":").append(sgv)
                                      .append("}")
                                      .toString();
        }
    }

    public record DataPoint(double minValue, double maxValue, double avgValue, double percentile10, double percentile25, double percentile75, double percentile90, double median){}

    public record ButtonShape(int daysToShow, Rectangle shape) {}

    public record DayShape(DayOfWeek day, Rectangle shape) {}
}
