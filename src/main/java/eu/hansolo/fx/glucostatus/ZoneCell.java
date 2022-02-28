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

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.text.TextAlignment;


public class ZoneCell extends ListCell<String> {
    private Label zoneLabel;


    // ******************** Constructors **************************************
    public ZoneCell(final boolean darkMode) {
        zoneLabel = new Label();
        zoneLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        zoneLabel.setFont(Fonts.configRoundedRegular(12));
        zoneLabel.setAlignment(Pos.CENTER);
        zoneLabel.setTextAlignment(TextAlignment.CENTER);
        zoneLabel.setContentDisplay(ContentDisplay.CENTER);
        zoneLabel.setPrefWidth(Label.USE_COMPUTED_SIZE);
        zoneLabel.setMaxWidth(Double.MAX_VALUE);
        setStyle("-fx-background-color: transparent;");
    }


    // ******************** Methods *******************************************
    @Override protected void updateItem(final String zoneText, final boolean empty) {
        setText(null);
        if (empty || null == zoneText || zoneText.isEmpty()) {
            setGraphic(null);
        } else {
            zoneLabel.setText(zoneText);
            setGraphic(zoneLabel);
        }
    }
}
