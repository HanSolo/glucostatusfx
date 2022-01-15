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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;


public enum Internationalization {
    INSTANCE;

    private final List<WeakReference<LocaleObserver>> localeObservers = new CopyOnWriteArrayList<>();


    // ******************** Constructors **************************************
    Internationalization() { }


    // ******************** Methods *******************************************
    public void addObserver(final LocaleObserver observer) {
        final WeakReference<LocaleObserver> observerWeakReference = new WeakReference<>(observer);
        localeObservers.add(observerWeakReference);
    }

    public void setLocale(final Locale locale) {
        Locale.setDefault(locale);
        update();
    }

    public void update() {
        ResourceBundle.clearCache();
        localeObservers.stream()
                       .map(o -> o.get())
                       .filter(Objects::nonNull)
                       .forEach(o -> o.onLocaleChanged(Locale.getDefault()));
    }
}

