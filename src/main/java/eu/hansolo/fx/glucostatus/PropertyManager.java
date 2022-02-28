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

import com.gluonhq.attach.settings.SettingsService;

import java.util.HashMap;
import java.util.Map;


public enum PropertyManager {
    INSTANCE;

    private final Map<String, String> properties = new HashMap<>();


    // ******************** Constructors **************************************
    PropertyManager() {
        createProperties();
        loadProperties();
    }


    // ******************** Methods *******************************************
    public Map<String, String> getProperties() { return properties; }

    public Object get(final String KEY) { return properties.getOrDefault(KEY, ""); }
    public void set(final String KEY, final String VALUE) {
        properties.put(KEY, VALUE);
        storeProperties();
    }

    public String getString(final String key) { return getString(key, ""); }
    public String getString(final String key, final String defaultValue) { return properties.getOrDefault(key, defaultValue).toString(); }
    public void setString(final String key, final String value) { properties.put(key, value); }

    public double getDouble(final String key) { return getDouble(key, 0); }
    public double getDouble(final String key, final double defaultValue) { return Double.parseDouble(properties.getOrDefault(key, Double.toString(defaultValue)).toString()); }
    public void setDouble(final String key, final double value) { properties.put(key, Double.toString(value)); }

    public float getFloat(final String key) { return getFloat(key, 0); }
    public float getFloat(final String key, final float defaultValue) { return Float.parseFloat(properties.getOrDefault(key, Float.toString(defaultValue)).toString()); }
    public void setFloat(final String key, final float value) { properties.put(key, Float.toString(value)); }

    public int getInt(final String key) { return getInt(key, 0); }
    public int getInt(final String key, final int defaultValue) { return Integer.parseInt(properties.getOrDefault(key, Integer.toString(defaultValue)).toString()); }
    public void setInt(final String key, final int value) { properties.put(key, Integer.toString(value)); }

    public long getLong(final String key) { return getLong(key, 0); }
    public long getLong(final String key, final long defaultValue) { return Long.parseLong(properties.getOrDefault(key, Long.toString(defaultValue)).toString()); }
    public void setLong(final String key, final long value) { properties.put(key, Long.toString(value)); }

    public boolean getBoolean(final String key) { return getBoolean(key, false); }
    public boolean getBoolean(final String key, final boolean defaultValue) { return Boolean.parseBoolean(properties.getOrDefault(key, Boolean.toString(defaultValue)).toString()); }
    public void setBoolean(final String key, final boolean value) { properties.put(key, Boolean.toString(value)); }

    public boolean hasKey(final String key) { return properties.containsKey(key); }


    public void storeProperties() {
        if (null == properties) { return; }
        SettingsService.create().ifPresent(service -> properties.entrySet().forEach(entry -> service.store(entry.getKey(), entry.getValue())));
    }

    private void loadProperties() {
        SettingsService.create().ifPresent(service -> {
            Constants.DEFAULT_PROPERTIES.entrySet().forEach(entry -> {
                String value = service.retrieve(entry.getKey());
                if (null == value) {
                    service.store(entry.getKey(), entry.getValue());
                    value = entry.getValue();
                }
                properties.put(entry.getKey(), value);
            });
        });
    }

    private void createProperties() {
        SettingsService.create().ifPresent(service -> {
            Constants.DEFAULT_PROPERTIES.entrySet().forEach(entry -> {
                String value = service.retrieve(entry.getKey());
                if (null == value || value.isEmpty()) {
                    service.store(entry.getKey(), entry.getValue());
                }
            });
        });
    }
}
