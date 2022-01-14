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

import java.time.format.DateTimeFormatter;


public enum TimeInterval {
    LAST_720_HOURS("30 Days", 8640, 720, 2592000, DateTimeFormatter.ofPattern("HH")),
    LAST_168_HOURS("7 Days", 2016, 168, 604800, DateTimeFormatter.ofPattern("HH")),
    LAST_72_HOURS("72 Hours", 864, 72, 259200, DateTimeFormatter.ofPattern("HH")),
    LAST_48_HOURS("48 Hours", 576, 48, 172800, DateTimeFormatter.ofPattern("HH")),
    LAST_24_HOURS("24 Hours", 288, 24, 86400, DateTimeFormatter.ofPattern("HH")),
    LAST_12_HOURS("12 Hours", 144, 12, 43200, DateTimeFormatter.ofPattern("HH")),
    LAST_6_HOURS("6 Hours", 72, 6, 21600, DateTimeFormatter.ofPattern("HH:mm")),
    LAST_3_HOURS("3 Hours", 36, 3, 10800, DateTimeFormatter.ofPattern("HH:mm"));

    private final String            uiString;
    private final int               noOfEntries;
    private final int               hours;
    private final long              seconds;
    private final DateTimeFormatter formatter;


    // ******************** Constructors **************************************
    TimeInterval(final String uiString, final int noOfEntries, final int hours, final int seconds, final DateTimeFormatter formatter) {
        this.uiString    = uiString;
        this.noOfEntries = noOfEntries;
        this.hours       = hours;
        this.seconds     = seconds;
        this.formatter   = formatter;
    }


    // ******************** Methods *******************************************
    public String getUiString() { return uiString; }

    public int getNoOfEntries() { return noOfEntries; }

    public int getHours() { return hours; }

    public long getSeconds() { return seconds; }

    public DateTimeFormatter getFormatter() { return formatter; }
}
