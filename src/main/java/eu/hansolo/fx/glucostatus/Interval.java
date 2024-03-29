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

import eu.hansolo.fx.glucostatus.i18n.I18nKeys;
import eu.hansolo.fx.glucostatus.i18n.Translator;

import java.time.format.DateTimeFormatter;


public enum Interval {
    LAST_2160_HOURS(25_920, 2_160, 7_776_000, DateTimeFormatter.ofPattern("DD"), 0.75), // 90 Days
    LAST_720_HOURS(8_640, 720, 2_592_000, DateTimeFormatter.ofPattern("DD"), 0.75),     // 30 Days
    LAST_336_HOURS(4_032, 336, 1_209_600, DateTimeFormatter.ofPattern("DD"), 1.0),      // 14 Days
    LAST_168_HOURS(2_016, 168, 604_800, DateTimeFormatter.ofPattern("DD"), 1.0),        //  7 Days
    LAST_72_HOURS(864, 72, 259_200, DateTimeFormatter.ofPattern("HH"), 1.5),            //  3 Days
    LAST_48_HOURS(576, 48, 172_800, DateTimeFormatter.ofPattern("HH"), 1.5),            //  2 Days
    LAST_24_HOURS(288, 24, 86_400, DateTimeFormatter.ofPattern("HH"), 1.5),             //  1 Day
    LAST_12_HOURS(144, 12, 43_200, DateTimeFormatter.ofPattern("HH"), 2.0),
    LAST_6_HOURS(72, 6, 21_600, DateTimeFormatter.ofPattern("HH:mm"), 2.0),
    LAST_3_HOURS(36, 3, 10_800, DateTimeFormatter.ofPattern("HH:mm"), 2.0);

    private final Translator        translator = new Translator(I18nKeys.RESOURCE_NAME);
    private final int               noOfEntries;
    private final int               hours;
    private final long              seconds;
    private final DateTimeFormatter formatter;
    private final double            lineWidth;


    // ******************** Constructors **************************************
    Interval(final int noOfEntries, final int hours, final int seconds, final DateTimeFormatter formatter, final double lineWidth) {
        this.noOfEntries = noOfEntries;
        this.hours       = hours;
        this.seconds     = seconds;
        this.formatter   = formatter;
        this.lineWidth   = lineWidth;
    }


    // ******************** Methods *******************************************
    public String getUiString() {
        switch(this) {
            case LAST_2160_HOURS -> { return translator.get(I18nKeys.TIME_RANGE_2160_HOURS); }
            case LAST_720_HOURS  -> { return translator.get(I18nKeys.TIME_RANGE_720_HOURS); }
            case LAST_336_HOURS  -> { return translator.get(I18nKeys.TIME_RANGE_336_HOURS); }
            case LAST_168_HOURS  -> { return translator.get(I18nKeys.TIME_RANGE_168_HOURS); }
            case LAST_72_HOURS   -> { return translator.get(I18nKeys.TIME_RANGE_72_HOURS);  }
            case LAST_48_HOURS   -> { return translator.get(I18nKeys.TIME_RANGE_48_HOURS);  }
            case LAST_24_HOURS   -> { return translator.get(I18nKeys.TIME_RANGE_24_HOURS);  }
            case LAST_12_HOURS   -> { return translator.get(I18nKeys.TIME_RANGE_12_HOURS);  }
            case LAST_6_HOURS    -> { return translator.get(I18nKeys.TIME_RANGE_6_HOURS);   }
            case LAST_3_HOURS    -> { return translator.get(I18nKeys.TIME_RANGE_3_HOURS);   }
            default              -> { return ""; }
        }
    }

    public int getNoOfEntries() { return noOfEntries; }

    public int getHours() { return hours; }

    public long getSeconds() { return seconds; }

    public DateTimeFormatter getFormatter() { return formatter; }

    public double getLineWidth() { return lineWidth; }
}
