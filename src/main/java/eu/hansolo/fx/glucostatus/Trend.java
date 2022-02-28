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

import java.util.Arrays;
import java.util.Optional;


public enum Trend {
    FLAT("Flat", 0, "\u2192"),
    SINGLE_UP("SingleUp", 1, "\u2191"),
    DOUBLE_UP("DoubleUp", 2, "\u2191\u2191"),
    DOUBLE_DOWN("DoubleDown", 3, "\u2193\u2193"),
    SINGLE_DOWN("SingleDown", 4, "\u2193"),
    FORTY_FIVE_DOWN("FortyFiveDown", 5, "\u2198"),
    FORTY_FIVE_UP("FortyFiveUp", 6, "\u2197"),
    NONE("", 7, "");

    private final String textKey;
    private final int    key;
    private final String symbol;


    // ******************** Constructors **************************************
    Trend(final String textKey, final int key, final String symbol) {
        this.textKey = textKey;
        this.key     = key;
        this.symbol  = symbol;
    }


    // ******************** Methods *******************************************
    public String getTextKey() { return textKey; }

    public int getKey() { return key; }

    public String getSymbol() { return symbol; }


    public static Trend getFromText(final String text) {
        Optional<Trend> optTrend = Arrays.stream(Trend.values()).filter(trend -> trend.getTextKey().toLowerCase().equals(text.toLowerCase())).findFirst();
        if (optTrend.isPresent()) {
            return optTrend.get();
        } else {
            optTrend = Arrays.stream(Trend.values()).filter(trend -> Integer.toString(trend.getKey()).equals(text)).findFirst();
            if (optTrend.isPresent()) {
                return optTrend.get();
            } else {
                return NONE;
            }
        }
    }
}
