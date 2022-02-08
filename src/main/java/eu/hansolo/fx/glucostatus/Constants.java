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

import javafx.scene.paint.Color;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class Constants {
    public static final String            HOME_FOLDER        = new StringBuilder(System.getProperty("user.home")).append(File.separator).toString();
    public static final String            URL_API            = "/api/v1/entries.json";
    public static final String            URL_PARAM_COUNT_1  = "?count=1";
    public static final long              TIMEOUT_IN_SECONDS = 480;
    public static final DateTimeFormatter DTF                = DateTimeFormatter.ofPattern("dd/MM/YY HH:mm");

    public static final String FIELD_ID                                               = "_id";
    public static final String FIELD_SGV                                              = "sgv";
    public static final String FIELD_DATE                                             = "date";
    public static final String FIELD_DATE_STRING                                      = "dateString";
    public static final String FIELD_TREND                                            = "trend";
    public static final String FIELD_DIRECTION                                        = "direction";
    public static final String FIELD_DEVICE                                           = "device";
    public static final String FIELD_TYPE                                             = "type";
    public static final String FIELD_UTC_OFFSET                                       = "utcOffset";
    public static final String FIELD_NOISE                                            = "noise";
    public static final String FIELD_FILTERED                                         = "filtered";
    public static final String FIELD_UNFILTERED                                       = "unfiltered";
    public static final String FIELD_RSSI                                             = "rssi";
    public static final String FIELD_DELTA                                            = "delta";
    public static final String FIELD_SYS_TIME                                         = "sysTime";

    public static final String PROPERTIES_NIGHTSCOUT_URL                              = "NIGHTSCOUT_URL";
    public static final String PROPERTIES_UNIT_MG                                     = "UNIT_MG";
    public static final String PROPERTIES_SHOW_DELTA_CHART                            = "SHOW_DELTA_CHART";
    public static final String PROPERTIES_TOO_LOW_INTERVAL                            = "TOO_LOW_INTERVAL";
    public static final String PROPERTIES_TOO_HIGH_INTERVAL                           = "TOO_HIGH_INTERVAL";
    public static final String PROPERTIES_MIN_ACCEPTABLE_MIN                          = "MIN_ACCEPTABLE_MIN";
    public static final String PROPERTIES_MIN_ACCEPTABLE_MAX                          = "MIN_ACCEPTABLE_MAX";
    public static final String PROPERTIES_MIN_NORMAL_MIN                              = "MIN_NORMAL_MIN";
    public static final String PROPERTIES_MIN_NORMAL_MAX                              = "MIN_NORMAL_MAX";
    public static final String PROPERTIES_MAX_NORMAL_MIN                              = "MAX_NORMAL_MIN";
    public static final String PROPERTIES_MAX_NORMAL_MAX                              = "MAX_NORMAL_MAX";
    public static final String PROPERTIES_MAX_ACCEPTABLE_MIN                          = "MAX_ACCEPTABLE_MIN";
    public static final String PROPERTIES_MAX_ACCEPTABLE_MAX                          = "MAX_ACCEPTABLE_MAX";
    public static final String PROPERTIES_MIN_VALUE                                   = "MIN_VALUE";
    public static final String PROPERTIES_MAX_VALUE                                   = "MAX_VALUE";
    public static final String PROPERTIES_MIN_CRITICAL                                = "MIN_CRITICAL";
    public static final String PROPERTIES_MIN_ACCEPTABLE                              = "MIN_ACCEPTABLE";
    public static final String PROPERTIES_MIN_NORMAL                                  = "MIN_NORMAL";
    public static final String PROPERTIES_MAX_NORMAL                                  = "MAX_NORMAL";
    public static final String PROPERTIES_MAX_ACCEPTABLE                              = "MAX_ACCEPTABLE";
    public static final String PROPERTIES_MAX_CRITICAL                                = "MAX_CRITICAL";
    public static final String PROPERTIES_SHOW_HIGH_VALUE_NOTIFICATION                = "SHOW_HIGH_VALUE_NOTIFICATION";
    public static final String PROPERTIES_SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION     = "SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION";
    public static final String PROPERTIES_SHOW_LOW_VALUE_NOTIFICATION                 = "SHOW_LOW_VALUE_NOTIFICATION";
    public static final String PROPERTIES_SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION      = "SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION";
    public static final String PROPERTIES_PLAY_SOUND_FOR_TOO_LOW_NOTIFICATION         = "PLAY_SOUND_FOR_TOO_LOW_NOTIFICATION";
    public static final String PROPERTIES_PLAY_SOUND_FOR_LOW_NOTIFICATION             = "PLAY_SOUND_FOR_LOW_NOTIFICATION";
    public static final String PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_LOW_NOTIFICATION  = "PLAY_SOUND_FOR_ACCEPTABLE_LOW_NOTIFICATION";
    public static final String PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_HIGH_NOTIFICATION = "PLAY_SOUND_FOR_ACCEPTABLE_HIGH_NOTIFICATION";
    public static final String PROPERTIES_PLAY_SOUND_FOR_HIGH_NOTIFICATION            = "PLAY_SOUND_FOR_HIGH_NOTIFICATION";
    public static final String PROPERTIES_PLAY_SOUND_FOR_TOO_HIGH_NOTIFICATION        = "PLAY_SOUND_FOR_TOO_HIGH_NOTIFICATION";
    public static final String PROPERTIES_CRITICAL_MAX_NOTIFICATION_INTERVAL          = "CRITICAL_MAX_NOTIFICATION_INTERVAL";
    public static final String PROPERTIES_CRITICAL_MIN_NOTIFICATION_INTERVAL          = "CRITICAL_MIN_NOTIFICATION_INTERVAL";


    public static final Color  GRAY                                                   = Color.color(0.5, 0.5, 0.5);
    public static final Color  RED                                                    = Color.color(0.94, 0.11, 0.13);
    public static final Color  ORANGE                                                 = Color.color(0.93, 0.43, 0.00);
    public static final Color  YELLOW                                                 = Color.color(1.00, 0.74, 0.01);
    public static final Color  GREEN                                                  = Color.color(0.57, 0.79, 0.23);
    public static final Color  DARK_GREEN                                             = Color.color(0.0, 0.5, 0.13);
    public static final Color  LIGHT_BLUE                                             = Color.color(0.01, 0.6, 0.93);
    public static final Color  BLUE                                                   = Color.color(0.00, 0.43, 1.00);

    public static final Color  DARK_BACKGROUND                                        = Color.rgb(33, 28, 29);
    public static final Color  BRIGHT_BACKGROUND                                      = Color.rgb(236, 235, 235);
    public static final Color  BRIGHT_TEXT                                            = Color.rgb(255, 246, 245);
    public static final Color  DARK_TEXT                                              = Color.rgb(20, 20, 20);

    public static final double DELTA_CHART_WIDTH                                      = 115;
    public static final double DELTA_CHART_HEIGHT                                     = 50;

    public static final double DEFAULT_SOON_TOO_LOW                                   = 80.0;
    public static final double DEFAULT_SOON_TOO_HIGH                                  = 350.0;

    public static final double DEFAULT_MIN_VALUE                                      = 0.0;
    public static final double DEFAULT_MAX_VALUE                                      = 400.0;
    public static final double DEFAULT_GLUCO_RANGE                                    = DEFAULT_MAX_VALUE - DEFAULT_MIN_VALUE;
    public static final double DEFAULT_MIN_CRITICAL                                   = 55.0;
    public static final double DEFAULT_MIN_ACCEPTABLE                                 = 65.0;
    public static final double DEFAULT_MIN_NORMAL                                     = 70.0;
    public static final double DEFAULT_MAX_NORMAL                                     = 140.0;
    public static final double DEFAULT_MAX_ACCEPTABLE                                 = 180.0;
    public static final double DEFAULT_MAX_CRITICAL                                   = 350.0;

    public static final double DEFAULT_MIN_CRITICAL_FACTOR                            = 1.0 - DEFAULT_MIN_CRITICAL   / DEFAULT_GLUCO_RANGE;
    public static final double DEFAULT_MAX_CRITICAL_FACTOR                            = 1.0 - DEFAULT_MAX_CRITICAL   / DEFAULT_GLUCO_RANGE;

    public static final double SETTINGS_MIN_ACCEPTABLE_MIN                            = 60;
    public static final double SETTINGS_MIN_ACCEPTABLE_MAX                            = 70;
    public static final double SETTINGS_MIN_NORMAL_MIN                                = 70;
    public static final double SETTINGS_MIN_NORMAL_MAX                                = 80;
    public static final double SETTINGS_MAX_NORMAL_MIN                                = 120;
    public static final double SETTINGS_MAX_NORMAL_MAX                                = 160;
    public static final double SETTINGS_MAX_ACCEPTABLE_MIN                            = 120;
    public static final double SETTINGS_MAX_ACCEPTABLE_MAX                            = 250;

    public static final List<String> yAxisLabelsMgPerDeciliter = List.of("40", "80", "120", "160", "200", "240", "280", "320", "360", "400");
    public static final List<String> yAxisLabelsMmolPerLiter   = List.of("2.2", "4.4", "6.7", "8.9", "11.1", "13.3", "15.5", "17.8", "20.0", "22.2");
}
