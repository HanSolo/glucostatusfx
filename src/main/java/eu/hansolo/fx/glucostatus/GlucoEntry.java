/*
 * Copyright (c) 2022 by Gerrit Grunwald
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

package eu.hansolo.fx.glucostatus;

import java.time.OffsetDateTime;
import java.util.Objects;


public class GlucoEntry implements Comparable<GlucoEntry> {
    private final String         id;
    private final double         sgv;
    private final long           datelong;
    private final OffsetDateTime date;
    private final String         dateString;
    private final Trend          trend;
    private final String         direction;
    private final String         device;
    private final String         type;
    private final int            utcOffset;
    private final int            noise;
    private final double         filtered;
    private final double         unfiltered;
    private final int            rssi;
    private final double         delta;
    private final String         sysTime;


    public GlucoEntry(final String id, final double sgv, final long datelong, final OffsetDateTime date, final String dateString, final Trend trend, final String direction, final String device, final String type, final int utcOffset, final int noise, final double filtered, final double unfiltered, final int rssi, final double delta, final String sysTime) {
        this.id         = id;
        this.sgv        = sgv;
        this.datelong   = datelong;
        this.date       = date;
        this.dateString = dateString;
        this.trend      = trend;
        this.direction  = direction;
        this.device     = device;
        this.type       = type;
        this.utcOffset  = utcOffset;
        this.noise      = noise;
        this.filtered   = filtered;
        this.unfiltered = unfiltered;
        this.rssi       = rssi;
        this.delta      = delta;
        this.sysTime    = sysTime;
    }


    public String id() { return id; }

    public double sgv() { return sgv; }

    public long datelong() { return datelong; }

    public OffsetDateTime date() { return date; }

    public String dateString() { return dateString; }

    public Trend trend() { return trend; }

    public String direction() { return direction; }

    public String device() { return device; }

    public String type() { return type; }

    public int utcOffset() { return utcOffset; }

    public int noise() { return noise; }

    public double filtered() { return filtered; }

    public double unfiltered() { return unfiltered; }

    public int rssi() { return rssi; }

    public double delta() { return delta; }

    public String sysTime() { return sysTime; }


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
