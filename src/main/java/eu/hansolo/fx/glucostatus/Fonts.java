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

import javafx.scene.text.Font;


public class Fonts {

    private static final String CONFIG_ROUNDED_REGULAR_NAME;
    private static final String CONFIG_ROUNDED_SEMI_BOLD_NAME;
    private static final String SF_ROUNDED_REGULAR_NAME;
    private static final String SF_ROUNDED_BOLD_NAME;

    private static       String configRoundedRegularName;
    private static       String configRoundedSemiBoldName;
    private static       String sfRoundedRegularName;
    private static       String sfRoundedBoldName;


    static {
        try {
            configRoundedRegularName  = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/Config Rounded Regular.otf"), 10).getName();
            configRoundedSemiBoldName = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/Config Rounded Semibold.otf"), 10).getName();
            sfRoundedRegularName      = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/Regular.ttf"), 10).getName();
            sfRoundedBoldName         = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/TestFont.ttf"), 10).getName();
        } catch (Exception exception) { }

        CONFIG_ROUNDED_REGULAR_NAME   = configRoundedRegularName;
        CONFIG_ROUNDED_SEMI_BOLD_NAME = configRoundedSemiBoldName;
        SF_ROUNDED_REGULAR_NAME       = sfRoundedRegularName;
        SF_ROUNDED_BOLD_NAME          = sfRoundedBoldName;
    }


    // ******************** Methods *******************************************
    public static Font configRoundedRegular(final double size) { return new Font(CONFIG_ROUNDED_REGULAR_NAME, size); }
    public static Font configRoundedSemibold(final double size) { return new Font(CONFIG_ROUNDED_SEMI_BOLD_NAME, size); }

    public static Font sfRoundedRegular(final double size) { return new Font(SF_ROUNDED_REGULAR_NAME, size); }
    public static Font sfRoundedBold(final double size) { return new Font(SF_ROUNDED_BOLD_NAME, size); }
}
