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
    private static final String SF_PRO_ROUNDED_REGULAR_NAME;
    private static final String SF_PRO_ROUNDED_SEMI_BOLD_NAME;
    private static final String SF_PRO_ROUNDED_BOLD_NAME;
    private static final String SF_PRO_TEXT_REGULAR_NAME;
    private static final String SF_PRO_TEXT_BOLD_NAME;
    private static       String sfProRoundedSemiBoldName;
    private static       String sfProRoundedRegularName;
    private static       String sfProRoundedBoldName;
    private static       String sfProTextRegularName;
    private static       String sfProTextBoldName;

    static {
        try {
            sfProRoundedRegularName   = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/SF-Pro-Rounded-Regular.ttf"), 10).getName();
            sfProRoundedSemiBoldName  = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/SF-Pro-Rounded-Semibold.ttf"), 10).getName();
            sfProRoundedBoldName      = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/SF-Pro-Rounded-Bold.ttf"), 10).getName();
            sfProTextRegularName      = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/SF-Pro-Text-Regular.ttf"), 10).getName();
            sfProTextBoldName         = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/glucostatus/SF-Pro-Text-Bold.ttf"), 10).getName();
        } catch (Exception exception) { }
        SF_PRO_ROUNDED_REGULAR_NAME   = sfProRoundedRegularName;
        SF_PRO_ROUNDED_SEMI_BOLD_NAME = sfProRoundedSemiBoldName;
        SF_PRO_ROUNDED_BOLD_NAME      = sfProRoundedBoldName;
        SF_PRO_TEXT_REGULAR_NAME      = sfProTextRegularName;
        SF_PRO_TEXT_BOLD_NAME         = sfProTextBoldName;
    }


    // ******************** Methods *******************************************
    public static Font sfProRoundedRegular(final double size) { return new Font(SF_PRO_ROUNDED_REGULAR_NAME, size); }
    public static Font sfProRoundedSemiBold(final double size) { return new Font(SF_PRO_ROUNDED_SEMI_BOLD_NAME, size); }
    public static Font sfProRoundedBold(final double size) { return new Font(SF_PRO_ROUNDED_BOLD_NAME, size); }
    public static Font sfProTextRegular(final double size) { return new Font(SF_PRO_TEXT_REGULAR_NAME, size); }
    public static Font sfProTextBold(final double size) { return new Font(SF_PRO_TEXT_BOLD_NAME, size); }
}
