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


public enum TimeInterval {
    LAST_720_HOURS(8640, 720, 2592000, DateTimeFormatter.ofPattern("HH")),
    LAST_168_HOURS(2016, 168, 604800, DateTimeFormatter.ofPattern("HH")),
    LAST_72_HOURS(864, 72, 259200, DateTimeFormatter.ofPattern("HH")),
    LAST_48_HOURS(576, 48, 172800, DateTimeFormatter.ofPattern("HH")),
    LAST_24_HOURS(288, 24, 86400, DateTimeFormatter.ofPattern("HH")),
    LAST_12_HOURS(144, 12, 43200, DateTimeFormatter.ofPattern("HH")),
    LAST_6_HOURS(72, 6, 21600, DateTimeFormatter.ofPattern("HH:mm")),
    LAST_3_HOURS(36, 3, 10800, DateTimeFormatter.ofPattern("HH:mm"));

    private final Translator        translator = new Translator(I18nKeys.RESOURCE_NAME);
    private final int               noOfEntries;
    private final int               hours;
    private final long              seconds;
    private final DateTimeFormatter formatter;


    // ******************** Constructors **************************************
    TimeInterval(final int noOfEntries, final int hours, final int seconds, final DateTimeFormatter formatter) {
        this.noOfEntries = noOfEntries;
        this.hours       = hours;
        this.seconds     = seconds;
        this.formatter   = formatter;
    }


    // ******************** Methods *******************************************
    public String getUiString() {
        switch(this) {
            case LAST_720_HOURS -> { return translator.get(I18nKeys.TIME_RANGE_720_HOURS); }
            case LAST_168_HOURS -> { return translator.get(I18nKeys.TIME_RANGE_168_HOURS); }
            case LAST_72_HOURS  -> { return translator.get(I18nKeys.TIME_RANGE_72_HOURS);  }
            case LAST_48_HOURS  -> { return translator.get(I18nKeys.TIME_RANGE_48_HOURS);  }
            case LAST_24_HOURS  -> { return translator.get(I18nKeys.TIME_RANGE_24_HOURS);  }
            case LAST_12_HOURS  -> { return translator.get(I18nKeys.TIME_RANGE_12_HOURS);  }
            case LAST_6_HOURS   -> { return translator.get(I18nKeys.TIME_RANGE_6_HOURS);   }
            case LAST_3_HOURS   -> { return translator.get(I18nKeys.TIME_RANGE_3_HOURS);   }
            default             -> { return ""; }
        }
    }

    public int getNoOfEntries() { return noOfEntries; }

    public int getHours() { return hours; }

    public long getSeconds() { return seconds; }

    public DateTimeFormatter getFormatter() { return formatter; }
}
