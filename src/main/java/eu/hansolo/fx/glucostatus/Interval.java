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


public enum Interval {
    LAST_720_HOURS(8640, 720, 2_592_000, DateTimeFormatter.ofPattern("DD")),
    LAST_336_HOURS(4032, 336, 1_209_000, DateTimeFormatter.ofPattern("DD")),
    LAST_168_HOURS(2016, 168, 604800, DateTimeFormatter.ofPattern("DD")),
    LAST_72_HOURS(864, 72, 259200, DateTimeFormatter.ofPattern("HH")),
    LAST_48_HOURS(576, 48, 172800, DateTimeFormatter.ofPattern("HH")),
    LAST_24_HOURS(288, 24, 86400, DateTimeFormatter.ofPattern("HH")),
    LAST_12_HOURS(144, 12, 43200, DateTimeFormatter.ofPattern("HH")),
    LAST_6_HOURS(72, 6, 21600, DateTimeFormatter.ofPattern("HH:mm")),
    LAST_3_HOURS(36, 3, 10800, DateTimeFormatter.ofPattern("HH:mm"));

    private final int               noOfEntries;
    private final int               hours;
    private final long              seconds;
    private final DateTimeFormatter formatter;


    // ******************** Constructors **************************************
    Interval(final int noOfEntries, final int hours, final int seconds, final DateTimeFormatter formatter) {
        this.noOfEntries = noOfEntries;
        this.hours       = hours;
        this.seconds     = seconds;
        this.formatter   = formatter;
    }


    // ******************** Methods *******************************************
    public String getUiString() {
        switch(this) {
            case LAST_720_HOURS: { return "30 d";  }
            case LAST_336_HOURS: { return "14 d";  }
            case LAST_168_HOURS: { return "7 d";   }
            case LAST_72_HOURS : { return "72 h";  }
            case LAST_48_HOURS : { return "48 h";  }
            case LAST_24_HOURS : { return "24 h";  }
            case LAST_12_HOURS : { return "12 h";  }
            case LAST_6_HOURS  : { return "6 h";   }
            case LAST_3_HOURS  : { return "3 h";   }
            default            : { return ""; }
        }
    }

    public int getNoOfEntries() { return noOfEntries; }

    public int getHours() { return hours; }

    public long getSeconds() { return seconds; }

    public DateTimeFormatter getFormatter() { return formatter; }
}
