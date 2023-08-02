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

import eu.hansolo.jdktools.versioning.VersionNumber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


public enum PropertyManager {
    INSTANCE;

    public  static final String     VERSION_PROPERTIES         = "version.properties";
    public  static final String     GLUCO_STATUS_FX_PROPERTIES = "glucostatusfx.properties";
    public  static final String     VERSION                    = "version";
    private              Properties properties;
    private              Properties versionProperties;


    // ******************** Constructors **************************************
    PropertyManager() {
        properties = new Properties();
        // Load properties
        final String homeFilePath = new StringBuilder(Constants.HOME_FOLDER).append(GLUCO_STATUS_FX_PROPERTIES).toString();

        // Create properties file if not exists
        Path path = Paths.get(homeFilePath);
        if (!Files.exists(path)) { createProperties(properties); }

        // Load properties file
        try (FileInputStream propertiesFile = new FileInputStream(homeFilePath)) {
            properties.load(propertiesFile);
        } catch (IOException ex) {
            System.out.println("Error reading Gluco Status FX properties file. " + ex);
        }

        // If properties empty, fill with default values
        if (properties.isEmpty()) {
            createProperties(properties);
        }

        // Version number properties
        versionProperties = new Properties();
        try {
            versionProperties.load(Main.class.getResourceAsStream(VERSION_PROPERTIES));
        } catch (IOException ex) {
            System.out.println("Error reading version properties file. " + ex);
        }
    }


    // ******************** Methods *******************************************
    public Properties getProperties() { return properties; }

    public Object get(final String KEY) { return properties.getOrDefault(KEY, ""); }
    public void set(final String KEY, final String VALUE) {
        properties.setProperty(KEY, VALUE);
        try {
            properties.store(new FileOutputStream(String.join(File.separator, System.getProperty("user.dir"), GLUCO_STATUS_FX_PROPERTIES)), null);
        } catch (IOException exception) {
            System.out.println("Error writing properties file: " + exception);
        }
    }

    public String getString(final String key) { return getString(key, ""); }
    public String getString(final String key, final String defaultValue) { return properties.getOrDefault(key, defaultValue).toString(); }
    public void setString(final String key, final String value) { properties.setProperty(key, value); }

    public double getDouble(final String key) { return getDouble(key, 0); }
    public double getDouble(final String key, final double defaultValue) { return Double.parseDouble(properties.getOrDefault(key, Double.toString(defaultValue)).toString()); }
    public void setDouble(final String key, final double value) { properties.setProperty(key, Double.toString(value)); }

    public float getFloat(final String key) { return getFloat(key, 0); }
    public float getFloat(final String key, final float defaultValue) { return Float.parseFloat(properties.getOrDefault(key, Float.toString(defaultValue)).toString()); }
    public void setFloat(final String key, final float value) { properties.setProperty(key, Float.toString(value)); }

    public int getInt(final String key) { return getInt(key, 0); }
    public int getInt(final String key, final int defaultValue) { return Integer.parseInt(properties.getOrDefault(key, Integer.toString(defaultValue)).toString()); }
    public void setInt(final String key, final int value) { properties.setProperty(key, Integer.toString(value)); }

    public long getLong(final String key) { return getLong(key, 0); }
    public long getLong(final String key, final long defaultValue) { return Long.parseLong(properties.getOrDefault(key, Long.toString(defaultValue)).toString()); }
    public void setLong(final String key, final long value) { properties.setProperty(key, Long.toString(value)); }

    public boolean getBoolean(final String key) { return getBoolean(key, false); }
    public boolean getBoolean(final String key, final boolean defaultValue) { return Boolean.parseBoolean(properties.getOrDefault(key, Boolean.toString(defaultValue)).toString()); }
    public void setBoolean(final String key, final boolean value) { properties.setProperty(key, Boolean.toString(value)); }

    public boolean hasKey(final String key) { return properties.containsKey(key); }


    public void storeProperties() {
        if (null == properties) { return; }
        final String propFilePath = new StringBuilder(Constants.HOME_FOLDER).append(GLUCO_STATUS_FX_PROPERTIES).toString();
        try (OutputStream output = new FileOutputStream(propFilePath)) {
            properties.store(output, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public VersionNumber getVersionNumber() {
        return VersionNumber.fromText(versionProperties.getProperty(VERSION));
    }


    // ******************** Properties ****************************************
    private void createProperties(Properties properties) {
        final String propFilePath = new StringBuilder(Constants.HOME_FOLDER).append(GLUCO_STATUS_FX_PROPERTIES).toString();
        try (OutputStream output = new FileOutputStream(propFilePath)) {
            properties.put(Constants.PROPERTIES_NIGHTSCOUT_URL, "");
            properties.put(Constants.PROPERTIES_API_SECRET, "");
            properties.put(Constants.PROPERTIES_NIGHTSCOUT_TOKEN, "");
            properties.put(Constants.PROPERTIES_UNIT_MG, "TRUE");
            properties.put(Constants.PROPERTIES_SHOW_DELTA_CHART, "TRUE");
            properties.put(Constants.PROPERTIES_VOICE_OUTPUT, "FALSE");
            properties.put(Constants.PROPERTIES_VOICE_OUTPUT_INTERVAL, "5");
            properties.put(Constants.PROPERTIES_MIN_ACCEPTABLE_MIN, "60.0");
            properties.put(Constants.PROPERTIES_MIN_ACCEPTABLE_MAX, "70.0");
            properties.put(Constants.PROPERTIES_MIN_NORMAL_MIN, "70.0");
            properties.put(Constants.PROPERTIES_MIN_NORMAL_MAX, "80.0");
            properties.put(Constants.PROPERTIES_MAX_NORMAL_MIN, "120.0");
            properties.put(Constants.PROPERTIES_MAX_NORMAL_MAX, "160.0");
            properties.put(Constants.PROPERTIES_MAX_ACCEPTABLE_MIN, "120.0");
            properties.put(Constants.PROPERTIES_MAX_ACCEPTABLE_MAX, "250.0");
            properties.put(Constants.PROPERTIES_MIN_VALUE, "0.0");
            properties.put(Constants.PROPERTIES_MAX_VALUE, "400.0");
            properties.put(Constants.PROPERTIES_MIN_CRITICAL, "55.0");
            properties.put(Constants.PROPERTIES_MIN_ACCEPTABLE, "65.0");
            properties.put(Constants.PROPERTIES_MIN_NORMAL, "70.0");
            properties.put(Constants.PROPERTIES_MAX_NORMAL, "140.0");
            properties.put(Constants.PROPERTIES_MAX_ACCEPTABLE, "180.0");
            properties.put(Constants.PROPERTIES_MAX_CRITICAL, "350.0");
            properties.put(Constants.PROPERTIES_SHOW_LOW_VALUE_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_SHOW_HIGH_VALUE_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_LOW_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_SPEAK_TOO_LOW_NOTIFICATION, "FALSE");
            properties.put(Constants.PROPERTIES_PLAY_SOUND_FOR_LOW_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_SPEAK_LOW_NOTIFICATION, "FALSE");
            properties.put(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_LOW_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_HIGH_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_PLAY_SOUND_FOR_HIGH_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_HIGH_NOTIFICATION, "TRUE");
            properties.put(Constants.PROPERTIES_CRITICAL_MAX_NOTIFICATION_INTERVAL, "5");
            properties.put(Constants.PROPERTIES_CRITICAL_MIN_NOTIFICATION_INTERVAL, "5");
            properties.put(Constants.PROPERTIES_DARK_MODE, "TRUE");

            properties.store(output, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
