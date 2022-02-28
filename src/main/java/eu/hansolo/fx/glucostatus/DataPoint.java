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

public class DataPoint {
    private final double minValue;
    private final double maxValue;
    private final double avgValue;
    private final double percentile10;
    private final double percentile25;
    private final double percentile75;
    private final double percentile90;
    private final double median;


    public DataPoint(final double minValue, final double maxValue, final double avgValue, final double percentile10, final double percentile25, final double percentile75, final double percentile90, final double median) {
        this.minValue     = minValue;
        this.maxValue     = maxValue;
        this.avgValue     = avgValue;
        this.percentile10 = percentile10;
        this.percentile25 = percentile25;
        this.percentile75 = percentile75;
        this.percentile90 = percentile90;
        this.median       = median;
    }


    public double minValue() { return minValue; }

    public double maxValue() { return maxValue; }

    public double avgValue() { return avgValue; }

    public double percentile10() { return percentile10; }

    public double percentile25() { return percentile25; }

    public double percentile75() { return percentile75; }

    public double percentile90() { return percentile90; }

    public double median() { return median; }
}
