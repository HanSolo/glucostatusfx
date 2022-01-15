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

package eu.hansolo.fx.glucostatus.i18n;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;


public class Utf8ResourceBundle {
    private final static String ISO_8859_1 = "ISO-8859-1";
    private final static String UTF_8      = "UTF-8";


    // ******************** Methods *******************************************
    public static final ResourceBundle getBundle(final String baseName, final Locale locale) {
        return createUtf8PropertyResourceBundle(ResourceBundle.getBundle(baseName, locale));
    }

    private static ResourceBundle createUtf8PropertyResourceBundle(final ResourceBundle bundle) {
        if (!(bundle instanceof PropertyResourceBundle)) { return bundle; }
        return new Utf8PropertyResourceBundle((PropertyResourceBundle) bundle);
    }


    // ******************** Inner classes *************************************
    private static class Utf8PropertyResourceBundle extends ResourceBundle {
        private final PropertyResourceBundle bundle;


        // ******************** Constructors **************************************
        private Utf8PropertyResourceBundle(final PropertyResourceBundle bundle) {
            this.bundle = bundle;
        }


        // ******************** Methods *******************************************
        @Override public String getBaseBundleName() {
            return bundle.getBaseBundleName();
        }

        @Override public Locale getLocale() {
            return bundle.getLocale();
        }

        @Override public boolean containsKey(final String key) {
            return bundle.containsKey(key);
        }

        @Override public Set<String> keySet() {
            return bundle.keySet();
        }

        @Override public Enumeration getKeys() {
            return bundle.getKeys();
        }

        @Override protected Object handleGetObject(final String key) {
            try {
                final String value = bundle.getString(key);
                if (value == null) { return null; }
                try {
                    return new String(value.getBytes(UTF_8), UTF_8);
                } catch (final UnsupportedEncodingException e) {
                    throw new RuntimeException("Encoding not supported", e);
                }
            } catch (MissingResourceException e) {
                return key;
            }
        }
    }
}
