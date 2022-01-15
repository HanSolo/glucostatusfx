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

import eu.hansolo.fx.glucostatus.Records.DataPoint;
import eu.hansolo.fx.glucostatus.Records.GlucoEntry;
import eu.hansolo.fx.glucostatus.i18n.I18nKeys;
import eu.hansolo.fx.glucostatus.i18n.Translator;
import eu.hansolo.toolbox.tuples.Pair;
import javafx.animation.Transition;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Statistics {
    public enum StatisticRange {
        MIN_MAX,
        TEN_TO_NINETY,
        TWENTY_FIVE_TO_SEVENTY_FIVE
    }
    public enum StatisticCalculation {
        AVERAGE,
        MEDIAN
    }

    public  static final DateTimeFormatter TF         = DateTimeFormatter.ofPattern("HH:mm");
    private static final Translator        translator = new Translator(I18nKeys.RESOURCE_NAME);


    public static Map<LocalTime, DataPoint> analyze(final List<GlucoEntry> entries) {
        if (entries.isEmpty()) { return new HashMap<>(); }

        Map<LocalTime, DataPoint> dataMap  = new HashMap<>();
        for (int hour = 0 ; hour < 24 ; hour++) {
            for (int minute = 0 ; minute < 60 ; minute += 10) {
                final int h = hour;
                final int m = minute;
                List<GlucoEntry> bucketEntries = entries.stream()
                                                        .filter(entry -> ZonedDateTime.ofInstant(entry.date(), ZoneId.systemDefault()).getHour() == h)
                                                        .filter(entry -> ZonedDateTime.ofInstant(entry.date(), ZoneId.systemDefault()).getMinute() <= m)
                                                        .collect(Collectors.toList());

                if (bucketEntries.isEmpty()) { continue; }

                double minBucketValue = getMin(bucketEntries);
                double maxBucketValue = getMax(bucketEntries);
                double avgBucketValue = minBucketValue + (maxBucketValue - minBucketValue) / 2.0;
                double percentile10   = getPercentile(bucketEntries, 10.0);
                double percentile25   = getPercentile(bucketEntries, 25.0);
                double percentile75   = getPercentile(bucketEntries, 75.0);
                double percentile90   = getPercentile(bucketEntries, 90.0);
                double median         = getMedian(bucketEntries);

                LocalTime key   = LocalTime.of(hour, minute);
                DataPoint value = new DataPoint(minBucketValue, maxBucketValue, avgBucketValue, percentile10, percentile25, percentile75, percentile90, median);
                dataMap.put(key, value);
            }
        }
        return dataMap;
    }

    public static Pair<List<String>, List<String>> findTimesWithLowAndHighValues(final Map<LocalTime, DataPoint> dataMap, final double minThreshold, final double maxThreshold) {
        List<LocalTime> lowDates  = new ArrayList<>();
        List<LocalTime> highDates = new ArrayList<>();

        List<LocalTime> sortedKeys = dataMap.keySet().stream().sorted().collect(Collectors.toList());
        for (LocalTime key : sortedKeys) {
            double value = dataMap.get(key).median();
            if (value < minThreshold) { lowDates.add(key); }
            if (value > maxThreshold) { highDates.add(key); }
        }

        LocalTime lowDatesLast  = lowDates.isEmpty()  ? LocalTime.MIDNIGHT                 : lowDates.get(lowDates.size() - 1);
        LocalTime highDatesLast = highDates.isEmpty() ? LocalTime.MIDNIGHT.minusSeconds(1) : highDates.get(highDates.size() - 1);

        List<String> lowZones = new ArrayList<>();
        if (lowDates.size() > 2) {
            String zoneText = "";
            LocalTime lowStart = lowDates.get(0);
            for (int i = 0 ; i < lowDates.size() - 2 ; i++) {
                LocalTime date     = lowDates.get(i);
                LocalTime nextDate = lowDates.get(i + 1);

                if ((nextDate.getHour() * 3600 + nextDate.getMinute() * 60 + nextDate.getSecond()) - (date.getHour() * 3600 + date.getMinute() * 60 + date.getSecond()) > 600) {
                    int startHour = lowStart.getHour();
                    int endHour   = date.getHour();

                    // Only add zone if zone time at least 30 min
                    if ((date.getHour() * 3600 + date.getMinute() * 60 + date.getSecond()) - (lowStart.getHour() * 3600 + lowStart.getMinute() * 60 + lowStart.getSecond()) >= 1800) {
                        zoneText = getZoneText(true, startHour, endHour);
                        lowZones.add(new StringBuilder().append(zoneText).append("\n").append(TF.format(lowStart)).append(" - ").append(TF.format(date)).toString());
                    }
                    lowStart = nextDate;
                }
            }
            int startHour = lowStart.getHour();
            int endHour   = lowDatesLast.getHour();

            // Only add zone if zone time at least 30 min
            if ((lowDatesLast.getHour() * 3600 + lowDatesLast.getMinute() * 60 + lowDatesLast.getSecond()) - (lowStart.getHour() * 3600 + lowStart.getMinute() * 60 + lowStart.getSecond()) >= 1800) {
                zoneText = getZoneText(true, startHour, endHour);
                lowZones.add(new StringBuilder().append(zoneText).append("\n").append(TF.format(lowStart)).append(" - ").append(TF.format(lowDatesLast)).toString());
            }
        }
        if (lowZones.size() > 12) {
            lowZones.clear();
            lowZones.add("Generally too low");
        }

        List<String> highZones = new ArrayList<>();
        if (highDates.size() > 2) {
            String    zoneText  = "";
            LocalTime highStart = highDates.get(0);
            for (int i = 0 ; i < highDates.size() - 2 ; i++) {
                LocalTime date     = highDates.get(i);
                LocalTime nextDate = highDates.get(i + 1);

                if ((nextDate.getHour() * 3600 + nextDate.getMinute() * 60 + nextDate.getSecond()) - (date.getHour() * 3600 + date.getMinute() * 60 + date.getSecond()) > 600) {
                    int startHour = highStart.getHour();
                    int endHour   = date.getHour();

                    // Only add zone if zone time at least 30 min
                    if ((date.getHour() * 3600 + date.getMinute() * 60 + date.getSecond()) - (highStart.getHour() * 3600 + highStart.getMinute() * 60 + highStart.getSecond()) >= 1800) {
                        zoneText = getZoneText(false, startHour, endHour);
                        highZones.add(new StringBuilder().append(zoneText).append("\n").append(TF.format(highStart)).append(" - ").append(TF.format(date)).toString());
                    }
                    highStart = nextDate;
                }
            }
            int startHour = highStart.getHour();
            int endHour   = highDatesLast.getHour();

            // Only add zone if zone time at least 30 min
            if ((highDatesLast.getHour() * 3600 + highDatesLast.getMinute() * 60 + highDatesLast.getSecond()) - (highStart.getHour() * 3600 + highStart.getMinute() * 60 + highStart.getSecond()) >= 1800) {
                zoneText = getZoneText(false, startHour, endHour);
                highZones.add(new StringBuilder().append(zoneText).append("\n").append(TF.format(highStart)).append(" - ").append(TF.format(highDatesLast)).toString());
            }
        }
        if (highZones.size() > 12) {
            highZones.clear();
            highZones.add("Generally too high");
        }

        return new Pair<>(lowZones, highZones);
    }

    private static boolean isDay(final int hour) { return hour >= 6 && hour <= 20; }

    private static boolean isNight(final int hour) { return hour >= 20 && hour < 24 || hour >= 0 && hour < 6; }

    private static boolean isMorning(final int hour) { return hour > 5 && hour < 11; }

    private static boolean isAfternoon(final int hour) { return hour >= 14 && hour <= 18; }

    private static boolean isLunchtime(final int hour) { return hour >= 11 && hour <= 14; }

    private static String getZoneText(final boolean lowZone, final int startHour, final int endHour) {
        boolean nightStart     = isNight(startHour);
        boolean dayStart       = isDay(startHour);
        boolean nightEnd       = isNight(endHour);
        boolean dayEnd         = isDay(endHour);
        boolean morningStart   = isMorning(startHour);
        boolean morningEnd     = isMorning(endHour);
        boolean lunchtimeStart = isLunchtime(startHour);
        boolean lunchtimeEnd   = isLunchtime(endHour);
        boolean afterNoonStart = isAfternoon(startHour);
        boolean afterNoonEnd   = isAfternoon(endHour);

        var zoneText = "";
        if (lowZone) {
            if (morningStart && morningEnd) {             // Morning
                zoneText = translator.get(I18nKeys.STATISTICS_LOW_ZONE) + translator.get(I18nKeys.STATISTICS_MORNING);
            } else if (lunchtimeStart && lunchtimeEnd) {  // Lunchtime
                zoneText = translator.get(I18nKeys.STATISTICS_LOW_ZONE) + translator.get(I18nKeys.STATISTICS_LUNCHTIME);
            } else if (afterNoonStart && afterNoonEnd) {  // Afternoon
                zoneText = translator.get(I18nKeys.STATISTICS_LOW_ZONE) + translator.get(I18nKeys.STATISTICS_AFTERNOON);
            } else if (nightStart && nightEnd) {          // Night
                zoneText = translator.get(I18nKeys.STATISTICS_LOW_ZONE) + translator.get(I18nKeys.STATISTICS_NIGHT);
            } else if (nightStart && dayEnd) {            // Morning
                zoneText = translator.get(I18nKeys.STATISTICS_LOW_ZONE) + translator.get(I18nKeys.STATISTICS_MORNING);
            } else if (dayStart && dayEnd) {              // Day
                zoneText = translator.get(I18nKeys.STATISTICS_LOW_ZONE) + translator.get(I18nKeys.STATISTICS_DAY);
            } else if (dayStart && nightEnd) {            // Evening
                zoneText = translator.get(I18nKeys.STATISTICS_LOW_ZONE) + translator.get(I18nKeys.STATISTICS_EVENING);
            }
        } else {
            if (morningStart && morningEnd) {             // Morning
                zoneText = translator.get(I18nKeys.STATISTICS_HIGH_ZONE) + translator.get(I18nKeys.STATISTICS_MORNING);
            } else if (lunchtimeStart && lunchtimeEnd) {  // Lunchtime
                zoneText = translator.get(I18nKeys.STATISTICS_HIGH_ZONE) + translator.get(I18nKeys.STATISTICS_LUNCHTIME);
            } else if (afterNoonStart && afterNoonEnd) {  // Afternoon
                zoneText = translator.get(I18nKeys.STATISTICS_HIGH_ZONE) + translator.get(I18nKeys.STATISTICS_AFTERNOON);
            } else if (nightStart && nightEnd) {          // Night
                zoneText = translator.get(I18nKeys.STATISTICS_HIGH_ZONE) + translator.get(I18nKeys.STATISTICS_NIGHT);
            } else if (nightStart && dayEnd) {            // Morning
                zoneText = translator.get(I18nKeys.STATISTICS_HIGH_ZONE) + translator.get(I18nKeys.STATISTICS_MORNING);
            } else if (dayStart && dayEnd) {              // Day
                zoneText = translator.get(I18nKeys.STATISTICS_HIGH_ZONE) + translator.get(I18nKeys.STATISTICS_DAY);
            } else if (dayStart && nightEnd) {            // Evening
                zoneText = translator.get(I18nKeys.STATISTICS_HIGH_ZONE) + translator.get(I18nKeys.STATISTICS_EVENING);
            }
        }
        return zoneText;
    }

    public static double getMean(final List<GlucoEntry> entries) {
        if (entries.isEmpty()) { return 0; }
        double min  = getMin(entries);
        double max  = getMax(entries);
        double mean = (min + max) / 2.0;
        return mean;
    }

    public static double getVariance(final List<GlucoEntry> entries) {
        double average = getMean(entries);
        double temp    = 0.0;
        for (GlucoEntry entry : entries) {
            temp += ((entry.sgv() - average) * (entry.sgv() - average));
        }
        return (temp / entries.size());
    }

    public static double getStdDev(final List<GlucoEntry> entries) {
        return Math.sqrt(getVariance(entries));
    }

    public static double getMedian(final List<GlucoEntry> entries) {
        if (entries.isEmpty()) { return 0; }
        int              noOfEntries   = entries.size();
        List<GlucoEntry> sortedEntries = entries.stream().sorted(Comparator.comparingDouble(GlucoEntry::sgv)).collect(Collectors.toList());
        return noOfEntries % 2 == 0 ? (sortedEntries.get(noOfEntries / 2 - 1).sgv() + sortedEntries.get(noOfEntries / 2).sgv()) / 2.0 : sortedEntries.get(noOfEntries / 2).sgv();
    }

    public static double getMin(final List<GlucoEntry> entries) {
        if (entries.isEmpty()) { return 0; }
        return entries.stream().min(Comparator.comparingDouble(GlucoEntry::sgv)).get().sgv();
    }

    public static double getMax(final List<GlucoEntry> entries) {
        if (entries.isEmpty()) { return 0; }
        return entries.stream().max(Comparator.comparingDouble(GlucoEntry::sgv)).get().sgv();
    }

    public static double getAverage(final List<GlucoEntry> entries) {
        if (entries.isEmpty()) { return 0; }
        return entries.stream().map(entry -> entry.sgv()).reduce(0.0, Double::sum).doubleValue() / entries.size();
    }

    public static double getPercentile(final List<GlucoEntry> entries, final double percentile) {
        int              noOfEntries   = entries.size();
        List<GlucoEntry> sortedEntries = entries.stream().sorted(Comparator.comparingDouble(GlucoEntry::sgv)).collect(Collectors.toList());
        int              index         = eu.hansolo.toolbox.Helper.clamp(0, noOfEntries - 1, (int)(Math.ceil(percentile / 100.0 * (double) noOfEntries)));
        return sortedEntries.get(index).sgv();
    }
}
