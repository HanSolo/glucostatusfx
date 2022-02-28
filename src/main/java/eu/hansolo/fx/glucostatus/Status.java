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

import eu.hansolo.toolbox.unit.UnitDefinition;
import javafx.scene.paint.Color;


public enum Status {
    NONE(0, Constants.GRAY, Constants.GRAY),
    TOO_LOW(1, Constants.RED, Constants.BLUE),
    LOW(2, Constants.ORANGE, Constants.LIGHT_BLUE),
    ACCEPTABLE_LOW(3, Constants.YELLOW, Constants.DARK_GREEN),
    NORMAL(4, Constants.GREEN, Constants.GREEN),
    ACCEPTABLE_HIGH(5, Constants.YELLOW, Constants.YELLOW),
    HIGH(6, Constants.ORANGE, Constants.ORANGE),
    TOO_HIGH(7, Constants.RED, Constants.RED);

    private final int   id;
    private final Color color;
    private final Color color2;


    // ******************** Constructors **************************************
    Status(final int id, final Color color, final Color color2) {
        this.id     = id;
        this.color  = color;
        this.color2 = color2;
    }


    // ******************** Methods *******************************************
    public int getId() { return id; }

    public Color getColor() { return color; }

    public Color getColor2() { return color2; }


    public static final Status getByValue(final UnitDefinition unit, final double value) {
        switch(unit) {
            case MILLIMOL_PER_LITER:
                if (value <= Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_CRITICAL))) {
                    return TOO_LOW;
                } else if (Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_CRITICAL)) <= value && value < Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE))) {
                    return LOW;
                } else if (Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE)) <= value && value < Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL))) {
                    return ACCEPTABLE_LOW;
                } else if (Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)) <= value && value < Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL))) {
                    return NORMAL;
                } else if (Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)) <= value && value < Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE))) {
                    return ACCEPTABLE_HIGH;
                } else if (Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE)) <= value && value < Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_CRITICAL))) {
                    return HIGH;
                } else if (value > Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_CRITICAL))) {
                    return TOO_HIGH;
                } else {
                    return NONE;
                }
            default:
                if (value <= PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_CRITICAL)) {
                    return TOO_LOW;
                } else if (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_CRITICAL) <= value && value < PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE)) {
                    return LOW;
                } else if (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE) <= value && value < PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)) {
                    return ACCEPTABLE_LOW;
                } else if (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL) <= value && value < PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)) {
                    return NORMAL;
                } else if (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL) <= value && value < PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE)) {
                    return ACCEPTABLE_HIGH;
                } else if (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE) <= value && value < PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_CRITICAL)) {
                    return HIGH;
                } else if (value > PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_CRITICAL)) {
                    return TOO_HIGH;
                } else {
                    return NONE;
                }
        }
    }
}
