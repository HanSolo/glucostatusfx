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

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import eu.hansolo.applefx.MacosButton;
import eu.hansolo.applefx.MacosScrollPane;
import eu.hansolo.applefx.MacosSeparator;
import eu.hansolo.applefx.MacosSlider;
import eu.hansolo.applefx.MacosSwitch;
import eu.hansolo.applefx.MacosSwitchBuilder;
import eu.hansolo.applefx.MacosTextField;
import eu.hansolo.applefx.tools.MacosSystemColor;
import eu.hansolo.fx.glucostatus.Records.DataPoint;
import eu.hansolo.fx.glucostatus.Records.GlucoEntry;
import eu.hansolo.fx.glucostatus.Statistics.StatisticCalculation;
import eu.hansolo.fx.glucostatus.Statistics.StatisticRange;
import eu.hansolo.fx.glucostatus.i18n.I18nKeys;
import eu.hansolo.fx.glucostatus.i18n.Translator;
import eu.hansolo.fx.glucostatus.notification.Notification;
import eu.hansolo.fx.glucostatus.notification.NotificationBuilder;
import eu.hansolo.fx.glucostatus.notification.NotifierBuilder;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.toolbox.tuples.Pair;
import eu.hansolo.toolbox.unit.UnitDefinition;
import eu.hansolo.toolboxfx.HelperFX;
import eu.hansolo.toolboxfx.geom.Point;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.hansolo.toolbox.Helper.getArchitecture;
import static eu.hansolo.toolbox.Helper.getOperatingSystem;
import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIGRAM_PER_DECILITER;
import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIMOL_PER_LITER;


public class Main extends Application {
    private static final Insets                     GRAPH_INSETS  = new Insets(20, 10, 20, 10);
    private final        Image                      icon          = new Image(Main.class.getResourceAsStream("icon48x48.png"));
    private final        Image                      stageIcon     = new Image(Main.class.getResourceAsStream("icon128x128.png"));
    private              ZonedDateTime              lastUpdate    = ZonedDateTime.now().minusMinutes(6);
    private final        Translator                 translator    = new Translator(I18nKeys.RESOURCE_NAME);
    private              String                     nightscoutUrl = "";
    private              boolean                    trayIconSupported;
    private              OperatingSystem            operatingSystem;
    private              Architecture               architecture;
    private              boolean                    darkMode;
    private              Color                      accentColor;
    private              ZonedDateTime              lastNotification;
    private              Notification.Notifier      notifier;
    private              AudioClip                  notificationSound;
    private              Dialog                     aboutDialog;
    private              Stage                      stage;
    private              Region                     glassOverlay;
    private              HBox                       buttonHbox;
    private              AnchorPane                 mainPane;
    private              SVGPath                    reloadButton;
    private              SVGPath                    timeInRangeChartButton;
    private              SVGPath                    patternChartButton;
    private              SVGPath                    exclamationMark;
    private              Label                      valueLabel;
    private              TextFlow                   last5DeltasLabel;
    private              Label                      timestampLabel;
    private              Label                      rangeAverageLabel;
    private              Text                       unit;
    private              Text                       delta0;
    private              Text                       delta1;
    private              Text                       delta2;
    private              Text                       delta3;
    private              Text                       delta4;
    private              VBox                       vpane;
    private              StackPane                  pane;
    private              StackPane                  prefPane;
    private              MacosTextField             nightscoutUrlTextField;
    private              MacosSwitch                unitSwitch;
    private              MacosSwitch                deltaChartSwitch;
    private              MacosSwitch                tooLowSoundSwitch;
    private              MacosSwitch                enableLowSoundSwitch;
    private              MacosSwitch                lowSoundSwitch;
    private              MacosSwitch                enableAcceptableLowSoundSwitch;
    private              MacosSwitch                acceptableLowSoundSwitch;
    private              MacosSwitch                enableAcceptableHighSoundSwitch;
    private              MacosSwitch                acceptableHighSoundSwitch;
    private              MacosSwitch                enableHighSoundSwitch;
    private              MacosSwitch                highSoundSwitch;
    private              MacosSwitch                tooHighSoundSwitch;
    private              MacosSlider                tooLowIntervalSlider;
    private              MacosSlider                tooHighIntervalSlider;
    private              MacosSlider                minAcceptableSlider;
    private              MacosSlider                minNormalSlider;
    private              MacosSlider                maxNormalSlider;
    private              MacosSlider                maxAcceptableSlider;
    private              Canvas                     canvas;
    private              GraphicsContext            ctx;
    private              StackPane                  chartPane;
    private              boolean                    deltaChartVisible;
    private              ToggleGroup                intervalToggleGroup;
    private              ToggleButton               sevenDays;
    private              ToggleButton               seventyTwoHours;
    private              ToggleButton               fourtyEightHours;
    private              ToggleButton               twentyFourHours;
    private              ToggleButton               twelveHours;
    private              ToggleButton               sixHours;
    private              ToggleButton               threeHours;
    private              Button                     prefButton;
    private              ScheduledService<Void>     service;
    private              double                     minAcceptable;
    private              double                     minNormal;
    private              double                     maxNormal;
    private              double                     maxAcceptable;
    private              double                     minAcceptableFactor;
    private              double                     minNormalFactor;
    private              double                     maxNormalFactor;
    private              double                     maxAcceptableFactor;
    private              UnitDefinition             currentUnit;
    private              boolean                    outdated;
    private              ObservableList<GlucoEntry> allEntries;
    private              List<GlucoEntry>           entries;
    private              List<Double>               deltas;
    private              double                     avg;
    private              BooleanProperty            dialogVisible;
    private              double                     deltaMin;
    private              double                     deltaMax;
    private              GlucoEntry                 currentEntry;
    private              Color                      currentColor;
    private              TimeInterval               currentInterval;
    private              Font                       ticklabelFont;
    private              Font                       smallTicklabelFont;
    private              boolean                    slowlyRising;
    private              boolean                    slowlyFalling;
    private              boolean                    hideMenu;
    private              FXTrayIcon                 trayIcon;
    private              EventHandler<MouseEvent>   eventConsumer;


    // ******************** Initialization ************************************
    @Override public void init() {
        nightscoutUrl     = PropertyManager.INSTANCE.getString(Constants.PROPERTIES_NIGHTSCOUT_URL);
        operatingSystem   = getOperatingSystem();
        architecture      = getArchitecture();
        darkMode          = eu.hansolo.applefx.tools.Helper.isDarkMode();
        accentColor       =  eu.hansolo.applefx.tools.Helper.getMacosAccentColorAsColor();
        currentUnit       = MILLIGRAM_PER_DECILITER;
        outdated          = false;
        currentInterval   = TimeInterval.LAST_24_HOURS;
        allEntries        = FXCollections.observableArrayList();
        entries           = new ArrayList<>();
        deltas            = new ArrayList<>();
        dialogVisible     = new SimpleBooleanProperty(false);
        deltaChartVisible = false;
        lastNotification  = ZonedDateTime.now();
        notificationSound = new AudioClip(getClass().getResource("alarm.wav").toExternalForm());
        slowlyRising      = false;
        slowlyFalling     = false;
        currentEntry      = new GlucoEntry("-1", 0, Instant.now().getEpochSecond(), Instant.now(), "", Trend.NONE, "", "", "", 2, 0, 0, 0, 0, 0, "");
        updateSettings();

        ticklabelFont      = Fonts.sfProTextRegular(10);
        smallTicklabelFont = Fonts.sfProTextRegular(8);

        eventConsumer = evt -> {
            ToggleButton src = (ToggleButton) evt.getSource();
            if (src.isSelected()) { evt.consume(); }
        };

        intervalToggleGroup = new ToggleGroup();
        sevenDays        = createToggleButton(translator.get(I18nKeys.TIME_NAME_168_HOURS), intervalToggleGroup, eventConsumer);
        seventyTwoHours  = createToggleButton(translator.get(I18nKeys.TIME_NAME_72_HOURS), intervalToggleGroup, eventConsumer);
        fourtyEightHours = createToggleButton(translator.get(I18nKeys.TIME_NAME_48_HOURS), intervalToggleGroup, eventConsumer);
        twentyFourHours  = createToggleButton(translator.get(I18nKeys.TIME_NAME_24_HOURS), intervalToggleGroup, eventConsumer);
        twelveHours      = createToggleButton(translator.get(I18nKeys.TIME_NAME_12_HOURS), intervalToggleGroup, eventConsumer);
        sixHours         = createToggleButton(translator.get(I18nKeys.TIME_NAME_6_HOURS), intervalToggleGroup, eventConsumer);
        threeHours       = createToggleButton(translator.get(I18nKeys.TIME_NAME_3_HOURS), intervalToggleGroup, eventConsumer);

        twentyFourHours.setSelected(true);

        SVGPath settingsIcon = new SVGPath();
        settingsIcon.setContent("M8.005,14.887c0.084,-0 0.168,-0.005 0.252,-0.014c0.084,-0.009 0.172,-0.013 0.262,-0.013l0.415,0.794c0.042,0.084 0.104,0.146 0.185,0.185c0.081,0.039 0.17,0.052 0.266,0.04c0.205,-0.036 0.322,-0.159 0.352,-0.37l0.127,-0.884c0.162,-0.048 0.324,-0.102 0.487,-0.162c0.162,-0.061 0.325,-0.124 0.487,-0.19l0.65,0.596c0.15,0.144 0.322,0.162 0.514,0.054c0.169,-0.102 0.235,-0.259 0.199,-0.469l-0.19,-0.876c0.139,-0.096 0.276,-0.198 0.411,-0.307c0.135,-0.108 0.263,-0.222 0.383,-0.343l0.822,0.334c0.198,0.079 0.367,0.036 0.505,-0.126c0.054,-0.066 0.086,-0.146 0.095,-0.239c0.009,-0.093 -0.014,-0.179 -0.068,-0.257l-0.469,-0.758c0.096,-0.139 0.185,-0.285 0.266,-0.438c0.081,-0.153 0.161,-0.308 0.239,-0.465l0.894,0.045c0.096,0 0.183,-0.025 0.261,-0.076c0.078,-0.052 0.133,-0.122 0.163,-0.212c0.036,-0.091 0.039,-0.179 0.009,-0.267c-0.03,-0.087 -0.082,-0.161 -0.154,-0.221l-0.704,-0.55c0.043,-0.163 0.08,-0.33 0.113,-0.501c0.033,-0.172 0.059,-0.345 0.077,-0.519l0.839,-0.271c0.205,-0.072 0.307,-0.207 0.307,-0.406c-0,-0.204 -0.102,-0.343 -0.307,-0.415l-0.839,-0.262c-0.018,-0.18 -0.044,-0.355 -0.077,-0.523c-0.033,-0.168 -0.07,-0.337 -0.113,-0.505l0.704,-0.551c0.072,-0.06 0.122,-0.132 0.149,-0.216c0.027,-0.085 0.026,-0.172 -0.004,-0.262c-0.03,-0.09 -0.085,-0.161 -0.163,-0.212c-0.078,-0.051 -0.165,-0.074 -0.261,-0.068l-0.894,0.036c-0.078,-0.162 -0.158,-0.319 -0.239,-0.469c-0.081,-0.15 -0.17,-0.295 -0.266,-0.433l0.469,-0.758c0.054,-0.078 0.077,-0.163 0.068,-0.253c-0.009,-0.09 -0.041,-0.168 -0.095,-0.235c-0.138,-0.168 -0.307,-0.213 -0.505,-0.135l-0.822,0.325c-0.12,-0.114 -0.248,-0.227 -0.383,-0.338c-0.135,-0.112 -0.272,-0.216 -0.411,-0.312l0.19,-0.866c0.036,-0.223 -0.03,-0.379 -0.199,-0.469c-0.192,-0.109 -0.364,-0.088 -0.514,0.063l-0.65,0.577c-0.162,-0.066 -0.325,-0.128 -0.487,-0.185c-0.163,-0.057 -0.325,-0.112 -0.487,-0.167l-0.127,-0.875c-0.03,-0.205 -0.147,-0.328 -0.352,-0.37c-0.096,-0.012 -0.185,0.002 -0.266,0.041c-0.081,0.039 -0.143,0.097 -0.185,0.176l-0.415,0.803c-0.09,-0.006 -0.178,-0.011 -0.262,-0.014c-0.084,-0.003 -0.168,-0.004 -0.252,-0.004c-0.097,-0 -0.185,0.001 -0.267,0.004c-0.081,0.003 -0.167,0.008 -0.257,0.014l-0.424,-0.803c-0.09,-0.175 -0.241,-0.247 -0.451,-0.217c-0.205,0.042 -0.319,0.165 -0.343,0.37l-0.127,0.875c-0.168,0.055 -0.333,0.109 -0.496,0.163c-0.162,0.054 -0.322,0.117 -0.478,0.189l-0.659,-0.577c-0.144,-0.151 -0.316,-0.172 -0.514,-0.063c-0.169,0.09 -0.232,0.246 -0.19,0.469l0.181,0.866c-0.139,0.096 -0.276,0.2 -0.411,0.312c-0.135,0.111 -0.263,0.224 -0.383,0.338l-0.813,-0.325c-0.198,-0.078 -0.367,-0.033 -0.505,0.135c-0.06,0.067 -0.093,0.145 -0.099,0.235c-0.006,0.09 0.015,0.172 0.063,0.244l0.469,0.767c-0.096,0.138 -0.185,0.283 -0.266,0.433c-0.081,0.15 -0.161,0.307 -0.239,0.469l-0.894,-0.036c-0.096,-0.006 -0.183,0.017 -0.261,0.068c-0.078,0.051 -0.133,0.122 -0.163,0.212c-0.03,0.09 -0.031,0.177 -0.004,0.262c0.027,0.084 0.079,0.156 0.158,0.216l0.695,0.551c-0.043,0.168 -0.08,0.337 -0.113,0.505c-0.033,0.168 -0.056,0.343 -0.068,0.523l-0.848,0.262c-0.199,0.072 -0.298,0.211 -0.298,0.415c0,0.205 0.099,0.34 0.298,0.406l0.848,0.271c0.012,0.174 0.035,0.347 0.068,0.519c0.033,0.171 0.07,0.338 0.113,0.501l-0.695,0.55c-0.079,0.06 -0.131,0.134 -0.158,0.221c-0.027,0.088 -0.026,0.176 0.004,0.267c0.03,0.09 0.085,0.16 0.163,0.212c0.078,0.051 0.165,0.076 0.261,0.076l0.894,-0.045c0.078,0.157 0.158,0.312 0.239,0.465c0.081,0.153 0.17,0.299 0.266,0.438l-0.469,0.758c-0.048,0.078 -0.069,0.164 -0.063,0.257c0.006,0.093 0.039,0.173 0.099,0.239c0.138,0.162 0.307,0.205 0.505,0.126l0.813,-0.334c0.12,0.121 0.248,0.235 0.383,0.343c0.135,0.109 0.272,0.211 0.411,0.307l-0.181,0.876c-0.042,0.21 0.021,0.367 0.19,0.469c0.192,0.108 0.364,0.09 0.514,-0.054l0.659,-0.596c0.156,0.066 0.316,0.129 0.478,0.19c0.163,0.06 0.328,0.114 0.496,0.162l0.127,0.884c0.024,0.211 0.138,0.334 0.343,0.37c0.096,0.012 0.185,-0.001 0.266,-0.04c0.081,-0.039 0.143,-0.101 0.185,-0.185l0.424,-0.794c0.09,-0 0.176,0.004 0.257,0.013c0.082,0.009 0.17,0.014 0.267,0.014Zm-0,-1.228c-0.795,0 -1.53,-0.145 -2.207,-0.437c-0.677,-0.292 -1.269,-0.697 -1.778,-1.214c-0.508,-0.517 -0.904,-1.118 -1.186,-1.8c-0.283,-0.683 -0.425,-1.416 -0.425,-2.198c0,-0.788 0.142,-1.525 0.425,-2.211c0.282,-0.686 0.678,-1.287 1.186,-1.805c0.509,-0.517 1.101,-0.922 1.778,-1.213c0.677,-0.292 1.412,-0.438 2.207,-0.438c0.788,-0 1.52,0.146 2.197,0.438c0.677,0.291 1.269,0.696 1.778,1.213c0.508,0.518 0.904,1.119 1.186,1.805c0.283,0.686 0.425,1.423 0.425,2.211c-0,0.782 -0.142,1.515 -0.425,2.198c-0.282,0.682 -0.678,1.283 -1.186,1.8c-0.509,0.517 -1.101,0.922 -1.778,1.214c-0.677,0.292 -1.409,0.437 -2.197,0.437Zm-0.028,-3.718c0.41,0 0.774,-0.118 1.092,-0.356c0.319,-0.238 0.572,-0.591 0.758,-1.06l4.504,-0l-0.01,-1.029l-4.494,-0c-0.186,-0.463 -0.439,-0.812 -0.758,-1.047c-0.318,-0.235 -0.682,-0.352 -1.092,-0.352c-0.054,0 -0.117,0.005 -0.189,0.014c-0.072,0.009 -0.169,0.025 -0.289,0.049l-2.256,-3.862l-0.92,0.523l2.292,3.899c-0.193,0.216 -0.333,0.431 -0.42,0.645c-0.087,0.214 -0.131,0.435 -0.131,0.663c0,0.211 0.042,0.42 0.127,0.627c0.084,0.208 0.222,0.423 0.415,0.646l-2.374,3.862l0.903,0.542l2.355,-3.827c0.114,0.03 0.211,0.048 0.289,0.054c0.078,0.006 0.144,0.009 0.198,0.009Zm-0.785,-1.922c0,-0.228 0.082,-0.418 0.244,-0.568c0.162,-0.151 0.349,-0.226 0.559,-0.226c0.223,0 0.416,0.075 0.578,0.226c0.162,0.15 0.244,0.34 0.244,0.568c-0,0.223 -0.082,0.412 -0.244,0.569c-0.162,0.156 -0.355,0.234 -0.578,0.234c-0.21,0 -0.397,-0.078 -0.559,-0.234c-0.162,-0.157 -0.244,-0.346 -0.244,-0.569Z");
        settingsIcon.setFill(Constants.BRIGHT_TEXT);

        HBox buttonBar = new HBox(0, sevenDays, seventyTwoHours, fourtyEightHours, twentyFourHours, twelveHours, sixHours, threeHours);
        buttonBar.getStyleClass().add("button-bar");
        HBox.setHgrow(buttonBar, Priority.ALWAYS);
        VBox.setVgrow(buttonBar, Priority.NEVER);
        VBox.setMargin(buttonBar, new Insets(10, 10, 15, 10));

        prefButton = new Button("", settingsIcon);
        prefButton.setMinWidth(32);
        prefButton.setAlignment(Pos.CENTER);
        prefButton.setContentDisplay(ContentDisplay.CENTER);
        HBox.setHgrow(prefButton, Priority.NEVER);
        HBox.setMargin(prefButton, new Insets(0, 0, 0, 5));

        buttonHbox = new HBox(10, buttonBar, prefButton);
        buttonHbox.setAlignment(Pos.CENTER);
        buttonHbox.setPadding(new Insets(10, 10, 15, 10));

        currentColor = null == currentEntry ? Constants.GRAY : Helper.getColorForValue(currentUnit, currentEntry.sgv());

        Label titleLabel = createLabel(translator.get(I18nKeys.APP_NAME), 20, false, false, Pos.CENTER);
        AnchorPane.setTopAnchor(titleLabel, 5d);
        AnchorPane.setRightAnchor(titleLabel, 0d);
        AnchorPane.setLeftAnchor(titleLabel, 0d);

        reloadButton = new SVGPath();
        reloadButton.setContent("M1.228,8.211c-0,0.805 0.149,1.556 0.447,2.254c0.297,0.697 0.711,1.311 1.24,1.84c0.529,0.529 1.142,0.944 1.84,1.244c0.697,0.301 1.446,0.451 2.245,0.451c0.799,0 1.548,-0.15 2.245,-0.451c0.698,-0.3 1.311,-0.715 1.84,-1.244c0.529,-0.529 0.943,-1.143 1.24,-1.84c0.298,-0.698 0.447,-1.449 0.447,-2.254c0,-0.176 -0.058,-0.322 -0.174,-0.438c-0.115,-0.116 -0.264,-0.173 -0.446,-0.173c-0.171,-0 -0.309,0.057 -0.414,0.173c-0.104,0.116 -0.157,0.262 -0.157,0.438c0,0.64 -0.118,1.237 -0.355,1.791c-0.237,0.554 -0.565,1.04 -0.984,1.459c-0.419,0.419 -0.906,0.747 -1.46,0.984c-0.554,0.237 -1.148,0.356 -1.782,0.356c-0.634,-0 -1.228,-0.119 -1.782,-0.356c-0.554,-0.237 -1.041,-0.565 -1.46,-0.984c-0.419,-0.419 -0.747,-0.905 -0.984,-1.459c-0.237,-0.554 -0.355,-1.151 -0.355,-1.791c-0,-0.634 0.118,-1.228 0.355,-1.782c0.237,-0.554 0.565,-1.04 0.984,-1.459c0.419,-0.419 0.906,-0.747 1.46,-0.984c0.554,-0.237 1.148,-0.356 1.782,-0.356c0.215,0 0.42,0.007 0.616,0.021c0.196,0.014 0.379,0.04 0.55,0.078l-1.745,1.72c-0.06,0.061 -0.105,0.126 -0.132,0.195c-0.028,0.069 -0.042,0.142 -0.042,0.219c0,0.171 0.057,0.313 0.17,0.426c0.113,0.113 0.252,0.169 0.418,0.169c0.182,0 0.322,-0.055 0.421,-0.165l2.622,-2.613c0.11,-0.116 0.165,-0.259 0.165,-0.43c0,-0.166 -0.055,-0.306 -0.165,-0.422l-2.622,-2.646c-0.104,-0.121 -0.248,-0.182 -0.43,-0.182c-0.16,0 -0.296,0.059 -0.409,0.178c-0.113,0.118 -0.17,0.263 -0.17,0.434c0,0.077 0.014,0.152 0.042,0.223c0.027,0.072 0.069,0.138 0.124,0.199l1.513,1.488c-0.149,-0.027 -0.3,-0.048 -0.455,-0.062c-0.154,-0.014 -0.311,-0.021 -0.471,-0.021c-0.799,0 -1.548,0.149 -2.245,0.447c-0.698,0.298 -1.311,0.711 -1.84,1.24c-0.529,0.53 -0.943,1.143 -1.24,1.84c-0.298,0.698 -0.447,1.446 -0.447,2.245Z");
        reloadButton.setFill(Constants.BRIGHT_TEXT);
        AnchorPane.setTopAnchor(reloadButton, 10d);
        AnchorPane.setRightAnchor(reloadButton, 10d);

        valueLabel = createLabel("-", 92, true, true, Pos.CENTER);
        AnchorPane.setTopAnchor(valueLabel, 50d);
        AnchorPane.setRightAnchor(valueLabel, 0d);
        AnchorPane.setLeftAnchor(valueLabel, 0d);

        unit   = createDeltaText(currentUnit.UNIT.getUnitShort() + " (", true, 16);
        delta4 = createDeltaText("0.0, ", false, 14);
        delta3 = createDeltaText("0.0, ", false, 14);
        delta2 = createDeltaText("0.0, ", false, 14);
        delta1 = createDeltaText("0.0, ", false, 14);
        delta0 = createDeltaText("0.0)", true, 16);

        last5DeltasLabel = new TextFlow(unit, delta4, delta3, delta2, delta1, delta0);
        last5DeltasLabel.setTextAlignment(TextAlignment.CENTER);
        AnchorPane.setTopAnchor(last5DeltasLabel, 155d);
        AnchorPane.setRightAnchor(last5DeltasLabel, 0d);
        AnchorPane.setLeftAnchor(last5DeltasLabel, 0d);

        timestampLabel = createLabel("-", 16, false, true, Pos.CENTER);
        AnchorPane.setRightAnchor(timestampLabel, 0d);
        AnchorPane.setBottomAnchor(timestampLabel, 72d);
        AnchorPane.setLeftAnchor(timestampLabel, 0d);

        rangeAverageLabel = createLabel("-", 24, true, true, Pos.CENTER);
        AnchorPane.setRightAnchor(rangeAverageLabel, 0d);
        AnchorPane.setBottomAnchor(rangeAverageLabel, 20d);
        AnchorPane.setLeftAnchor(rangeAverageLabel, 0d);

        patternChartButton = new SVGPath();
        patternChartButton.setContent("M0.742,8.042l2.992,-0c0.331,-0 0.531,-0.146 0.6,-0.438l1.286,-5.958l-0.6,0l1.971,11.951c0.023,0.137 0.083,0.239 0.18,0.304c0.098,0.066 0.208,0.099 0.331,0.099c0.122,0 0.234,-0.034 0.334,-0.103c0.1,-0.068 0.164,-0.168 0.193,-0.3l1.903,-9.079l-0.489,-0.009l0.575,3.027c0.063,0.337 0.266,0.506 0.608,0.506l2.632,-0c0.155,-0 0.285,-0.052 0.39,-0.155c0.106,-0.103 0.159,-0.228 0.159,-0.377c0,-0.16 -0.051,-0.291 -0.154,-0.394c-0.103,-0.103 -0.235,-0.155 -0.395,-0.155l-3.283,0l1.114,0.918l-0.951,-4.304c-0.029,-0.131 -0.092,-0.229 -0.189,-0.291c-0.097,-0.063 -0.204,-0.095 -0.321,-0.095c-0.117,0 -0.226,0.035 -0.326,0.103c-0.1,0.069 -0.164,0.172 -0.193,0.309l-1.955,8.744l0.566,0l-1.946,-11.951c-0.017,-0.131 -0.071,-0.23 -0.163,-0.295c-0.091,-0.066 -0.194,-0.099 -0.308,-0.099c-0.115,0 -0.219,0.033 -0.313,0.099c-0.095,0.065 -0.156,0.164 -0.185,0.295l-1.612,7.485l1.115,-0.918l-3.566,0c-0.155,0 -0.285,0.053 -0.391,0.159c-0.105,0.106 -0.158,0.236 -0.158,0.39c-0,0.149 0.053,0.274 0.158,0.377c0.106,0.103 0.236,0.155 0.391,0.155Z");
        patternChartButton.setFill(Constants.BRIGHT_TEXT);
        patternChartButton.setScaleX(1.2);
        patternChartButton.setScaleY(1.2);
        AnchorPane.setBottomAnchor(patternChartButton, 30d);
        AnchorPane.setLeftAnchor(patternChartButton, 10d);

        timeInRangeChartButton = new SVGPath();
        timeInRangeChartButton.setContent("M1.195,12.162l1.63,0c0.399,0 0.698,-0.095 0.9,-0.284c0.201,-0.19 0.302,-0.473 0.302,-0.847l0,-4.843c0,-0.37 -0.101,-0.65 -0.302,-0.84c-0.202,-0.189 -0.501,-0.284 -0.9,-0.284l-1.63,-0c-0.394,-0 -0.691,0.095 -0.893,0.284c-0.201,0.19 -0.302,0.47 -0.302,0.84l0,4.843c0,0.374 0.101,0.657 0.302,0.847c0.202,0.189 0.499,0.284 0.893,0.284Zm4.99,0l1.63,0c0.399,0 0.698,-0.095 0.9,-0.284c0.201,-0.19 0.302,-0.473 0.302,-0.847l0,-6.452c0,-0.375 -0.101,-0.656 -0.302,-0.844c-0.202,-0.187 -0.501,-0.281 -0.9,-0.281l-1.63,0c-0.394,0 -0.693,0.094 -0.896,0.281c-0.204,0.188 -0.306,0.469 -0.306,0.844l-0,6.452c-0,0.374 0.102,0.657 0.306,0.847c0.203,0.189 0.502,0.284 0.896,0.284Zm4.983,0l1.63,0c0.398,0 0.698,-0.095 0.9,-0.284c0.201,-0.19 0.302,-0.473 0.302,-0.847l0,-8.069c0,-0.37 -0.101,-0.65 -0.302,-0.84c-0.202,-0.189 -0.502,-0.284 -0.9,-0.284l-1.63,-0c-0.394,-0 -0.691,0.095 -0.893,0.284c-0.201,0.19 -0.302,0.47 -0.302,0.84l-0,8.069c-0,0.374 0.101,0.657 0.302,0.847c0.202,0.189 0.499,0.284 0.893,0.284Z");
        timeInRangeChartButton.setFill(Constants.BRIGHT_TEXT);
        timeInRangeChartButton.setScaleX(1.2);
        timeInRangeChartButton.setScaleY(1.2);
        AnchorPane.setRightAnchor(timeInRangeChartButton, 10d);
        AnchorPane.setBottomAnchor(timeInRangeChartButton, 30d);


        mainPane = new AnchorPane(titleLabel, reloadButton, valueLabel, last5DeltasLabel, timestampLabel, rangeAverageLabel, patternChartButton, timeInRangeChartButton);
        mainPane.setPrefSize(820, 285);
        mainPane.setMinHeight(285);
        mainPane.setBackground(new Background(new BackgroundFill(Constants.GRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        VBox.setVgrow(mainPane, Priority.NEVER);

        canvas = new Canvas(820, 337);
        ctx    = canvas.getGraphicsContext2D();

        exclamationMark = new SVGPath();
        exclamationMark.setContent("M7.743,54.287l41.48,-0c1.613,-0 2.995,-0.346 4.147,-1.037c1.153,-0.692 2.046,-1.619 2.679,-2.783c0.634,-1.164 0.951,-2.483 0.951,-3.958c-0,-0.622 -0.092,-1.262 -0.277,-1.918c-0.184,-0.657 -0.449,-1.285 -0.795,-1.884l-20.774,-36.053c-0.737,-1.29 -1.705,-2.27 -2.904,-2.938c-1.198,-0.668 -2.454,-1.003 -3.767,-1.003c-1.314,0 -2.57,0.335 -3.768,1.003c-1.198,0.668 -2.155,1.648 -2.869,2.938l-20.774,36.053c-0.715,1.221 -1.072,2.489 -1.072,3.802c0,1.475 0.311,2.794 0.933,3.958c0.622,1.164 1.515,2.091 2.679,2.783c1.164,0.691 2.541,1.037 4.131,1.037Zm0.034,-4.874c-0.829,-0 -1.503,-0.294 -2.022,-0.882c-0.518,-0.587 -0.777,-1.261 -0.777,-2.022c-0,-0.207 0.023,-0.438 0.069,-0.691c0.046,-0.254 0.138,-0.496 0.276,-0.726l20.74,-36.087c0.254,-0.461 0.605,-0.807 1.054,-1.037c0.45,-0.231 0.905,-0.346 1.366,-0.346c0.484,-0 0.939,0.115 1.365,0.346c0.426,0.23 0.778,0.576 1.054,1.037l20.74,36.121c0.231,0.438 0.346,0.899 0.346,1.383c-0,0.761 -0.259,1.435 -0.778,2.022c-0.518,0.588 -1.204,0.882 -2.057,0.882l-41.376,-0Zm20.74,-4.494c0.83,0 1.562,-0.294 2.195,-0.881c0.634,-0.588 0.951,-1.308 0.951,-2.161c-0,-0.875 -0.311,-1.601 -0.933,-2.177c-0.623,-0.577 -1.36,-0.865 -2.213,-0.865c-0.875,0 -1.624,0.294 -2.247,0.882c-0.622,0.587 -0.933,1.308 -0.933,2.16c0,0.83 0.317,1.544 0.951,2.143c0.633,0.599 1.377,0.899 2.229,0.899Zm0,-9.056c1.521,-0 2.293,-0.795 2.316,-2.385l0.45,-14.173c0.023,-0.76 -0.237,-1.4 -0.778,-1.918c-0.542,-0.519 -1.216,-0.778 -2.022,-0.778c-0.83,0 -1.504,0.254 -2.022,0.761c-0.519,0.507 -0.767,1.14 -0.744,1.901l0.381,14.207c0.046,1.59 0.852,2.385 2.419,2.385Z");
        exclamationMark.setFill(Constants.BRIGHT_TEXT);
        exclamationMark.setVisible(false);

        chartPane = new StackPane(canvas, exclamationMark);
        chartPane.setPrefSize(820, 337);
        chartPane.setBackground(new Background(new BackgroundFill(Color.rgb(33, 28, 29), CornerRadii.EMPTY, Insets.EMPTY)));
        VBox.setVgrow(chartPane, Priority.ALWAYS);

        glassOverlay = new Region();
        glassOverlay.setOpacity(0.0);
        glassOverlay.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.5), new CornerRadii(10), Insets.EMPTY)));
        glassOverlay.setVisible(false);
        glassOverlay.setManaged(false);

        vpane = new VBox(buttonHbox, mainPane, chartPane);

        prefPane = createPrefPane();
        prefPane.setVisible(false);
        prefPane.setManaged(false);

        pane = new StackPane(vpane, glassOverlay, prefPane);
        pane.setBackground(new Background(new BackgroundFill(Color.rgb(33, 28, 29), CornerRadii.EMPTY, Insets.EMPTY)));

        registerListeners();
    }


    // ******************** App lifecycle *************************************
    @Override public void start(final Stage stage) {
        this.stage = stage;
        notifier = NotifierBuilder.create()
                                  .owner(stage)
                                  .popupLocation(OperatingSystem.MACOS == operatingSystem ? Pos.TOP_RIGHT : Pos.BOTTOM_RIGHT)
                                  .popupLifeTime(Duration.millis(5000))
                                  .build();
        this.trayIconSupported = FXTrayIcon.isSupported();

        if (trayIconSupported) {
            switch (operatingSystem) {
                case WINDOWS -> trayIcon = new FXTrayIcon(stage, getClass().getResource("icon48x48.png"));
                case MACOS   -> trayIcon = new FXTrayIcon(stage, Helper.createTextTrayIcon("--", Constants.BRIGHT_TEXT));
                case LINUX   -> trayIcon = new FXTrayIcon(stage, getClass().getResource("icon48x48.png"));
            }

            trayIcon.setTrayIconTooltip(translator.get(I18nKeys.APP_NAME));
            trayIcon.addExitItem(false);
            trayIcon.setApplicationTitle(translator.get(I18nKeys.APP_NAME));

            MenuItem aboutItem = new MenuItem(translator.get(I18nKeys.ABOUT_MENU_ITEM));
            aboutItem.setOnAction(e -> { if (!aboutDialog.isShowing()) { aboutDialog.showAndWait(); }});
            trayIcon.addMenuItem(aboutItem);

            MenuItem chartItem = new MenuItem(translator.get(I18nKeys.CHART_MENU_ITEM));
            chartItem.setOnAction(e -> {
                prefPane.setVisible(false);
                prefPane.setManaged(false);
                stage.show();
            });
            trayIcon.addMenuItem(chartItem);

            MenuItem preferencesItem = new MenuItem(translator.get(I18nKeys.PREFERENCES_MENU_ITEM));
            preferencesItem.setOnAction(e -> {
                applySettingsToPreferences();
                prefPane.setPrefSize(stage.getWidth(), stage.getHeight());
                prefPane.setManaged(true);
                prefPane.setVisible(true);
                stage.show();
            });
            trayIcon.addMenuItem(preferencesItem);

            trayIcon.addSeparator();

            MenuItem quitItem = new MenuItem(translator.get(I18nKeys.QUIT_MENU_ITEM));
            quitItem.setOnAction(e -> stop());
            trayIcon.addMenuItem(quitItem);

            trayIcon.show();
        } else {
            MenuBar menuBar = new MenuBar();
            menuBar.setUseSystemMenuBar(true);
            menuBar.setTranslateX(16);

            Menu menu = new Menu(translator.get(I18nKeys.APP_NAME));
            menu.setText(translator.get(I18nKeys.MENU));
            menu.setOnShowing(e -> hideMenu = false);
            menu.setOnHidden(e -> {
                if (!hideMenu) {
                    menu.show();
                }
            });

            CustomMenuItem aboutItem = new CustomMenuItem();
            Label          mainLabel = new Label(translator.get(I18nKeys.ABOUT_MENU_ITEM));
            mainLabel.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> hideMenu = false);
            mainLabel.addEventHandler(MouseEvent.MOUSE_EXITED, e -> hideMenu = true);
            aboutItem.setContent(mainLabel);
            aboutItem.setHideOnClick(false);
            aboutItem.setOnAction(e -> { if (!aboutDialog.isShowing()) { aboutDialog.showAndWait(); } });
            menu.getItems().add(aboutItem);

            CustomMenuItem chartItem = new CustomMenuItem();
            Label chartLabel = new Label(translator.get(I18nKeys.CHART_MENU_ITEM));
            chartLabel.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> hideMenu = false);
            chartLabel.addEventHandler(MouseEvent.MOUSE_EXITED, e -> hideMenu = true);
            chartItem.setContent(chartLabel);
            chartItem.setHideOnClick(false);
            chartItem.setOnAction(e -> {
                prefPane.setVisible(false);
                prefPane.setManaged(false);
                stage.show();
            });
            menu.getItems().add(chartItem);

            CheckMenuItem preferencesItem = new CheckMenuItem();
            preferencesItem.setVisible(true);
            preferencesItem.setText(translator.get(I18nKeys.PREFERENCES_MENU_ITEM));
            preferencesItem.selectedProperty().addListener(o -> {
                applySettingsToPreferences();
                prefPane.setPrefSize(stage.getWidth(), stage.getHeight());
                prefPane.setManaged(true);
                prefPane.setVisible(true);
                stage.show();
            });
            menu.getItems().add(preferencesItem);

            menu.getItems().add(new SeparatorMenuItem());

            CustomMenuItem quitItem = new CustomMenuItem();
            Label quitLabel = new Label(translator.get(I18nKeys.QUIT_MENU_ITEM));
            quitLabel.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> hideMenu = false);
            quitLabel.addEventHandler(MouseEvent.MOUSE_EXITED, e -> hideMenu = true);
            quitItem.setContent(quitLabel);
            quitItem.setHideOnClick(false);
            quitItem.setOnAction(e -> stop());
            menu.getItems().add(quitItem);

            menuBar.getMenus().add(menu);

            mainPane.getChildren().add(menuBar);
        }

        Scene scene = new Scene(pane);
        scene.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        stage.setScene(scene);
        //stage.setAlwaysOnTop(true);
        stage.show();
        stage.getIcons().add(stageIcon);
        stage.centerOnScreen();
        stage.setOnShowing(e -> {
            ZonedDateTime now = ZonedDateTime.now();
            if (now.toEpochSecond() - lastUpdate.toEpochSecond() > 300) {
                allEntries.clear();
                Helper.getEntriesFromLast30Days(nightscoutUrl + Constants.URL_API).thenAccept(l -> allEntries.addAll(l));
            }
        });

        postStart();
    }

    private void postStart() {
        if (null != nightscoutUrl && !nightscoutUrl.isEmpty()) {
            Helper.getEntriesFromLast30Days(nightscoutUrl + Constants.URL_API).thenAccept(l -> allEntries.addAll(l));
            service = new ScheduledService<>() {
                @Override protected Task<Void> createTask() {
                    Task task = new Task() {
                        @Override protected Object call() {
                            updateEntries();
                            return null;
                        }
                    };
                    return task;
                }
            };
            service.setPeriod(Duration.millis(60000));
            service.setRestartOnFailure(true);
            service.start();
        }
        canvas.setHeight(337);
        aboutDialog = createAboutDialog();
    }

    @Override public void stop() {
        service.cancel();
        Platform.exit();
        System.exit(0);
    }


    // ******************** Private methods ***********************************
    private void registerListeners() {
        pane.widthProperty().addListener(o -> canvas.setWidth(pane.getWidth()));
        pane.layoutBoundsProperty().addListener(o -> canvas.setHeight(pane.getHeight() - mainPane.getHeight() - buttonHbox.getHeight()));

        allEntries.addListener((ListChangeListener<GlucoEntry>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    updateUI();
                } else if (c.wasRemoved()) {

                }
            }
        });

        prefButton.setOnAction(e -> {
            applySettingsToPreferences();
            prefPane.setManaged(true);
            prefPane.setVisible(true);
        });

        reloadButton.setOnMousePressed(e -> updateEntries());

        timeInRangeChartButton.setOnMousePressed(e -> showTimeInRangeChart());

        patternChartButton.setOnMousePressed(e -> showPatternChart());

        intervalToggleGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (ov.equals(nv)) { nv.setSelected(true); }
            if (nv.equals(sevenDays)) {
                currentInterval = TimeInterval.LAST_168_HOURS;
            } else if (nv.equals(seventyTwoHours)) {
                currentInterval = TimeInterval.LAST_72_HOURS;
            } else if (nv.equals(fourtyEightHours)) {
                currentInterval = TimeInterval.LAST_48_HOURS;
            } else if (nv.equals(twentyFourHours)) {
                currentInterval = TimeInterval.LAST_24_HOURS;
            } else if (nv.equals(twelveHours)) {
                currentInterval = TimeInterval.LAST_12_HOURS;
            } else if (nv.equals(sixHours)) {
                currentInterval = TimeInterval.LAST_6_HOURS;
            } else if (nv.equals(threeHours)) {
                currentInterval = TimeInterval.LAST_3_HOURS;
            }
            updateUI();
        });

        dialogVisible.addListener((o, ov, nv) -> {
            if (nv) {
                glassOverlay.setManaged(true);
                glassOverlay.setVisible(true);
                FadeTransition fade = new FadeTransition(Duration.millis(500), glassOverlay);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                fade.play();
            } else {
                FadeTransition fade = new FadeTransition(Duration.millis(500), glassOverlay);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(e -> {
                    glassOverlay.setManaged(false);
                    glassOverlay.setVisible(false);
                });
                fade.play();
            }
        });

        canvas.widthProperty().addListener(o -> drawChart());
        canvas.heightProperty().addListener(o -> drawChart());
    }

    private void updateEntries() {
        if (null == nightscoutUrl || nightscoutUrl.isEmpty()) { return; }
        GlucoEntry           entryFound = null;
        HttpResponse<String> response   = Helper.get(nightscoutUrl + Constants.URL_API + Constants.URL_PARAM_COUNT_1);
        if (null != response && null != response.body() && !response.body().isEmpty()) {
            List<GlucoEntry> entry = Helper.getGlucoEntries(response.body());
            entryFound = entry.isEmpty() ? null : entry.get(0);
        }
        if (allEntries.isEmpty()) { return; }
        if (allEntries.get(0).datelong() == entryFound.datelong()) { return; }
        allEntries.remove(allEntries.size() - 1);
        allEntries.add(0, entryFound);
        lastUpdate = ZonedDateTime.now();
    }

    private boolean predict() {
        if (null == allEntries || allEntries.isEmpty()) { return false; }
        GlucoEntry       currentEntry = allEntries.get(0);
        List<GlucoEntry> last3Entries = allEntries.stream().limit(3).collect(Collectors.toList());

        // Soon too low
        boolean soonTooLow = last3Entries.stream().filter(entry -> Trend.DOUBLE_DOWN == entry.trend() || Trend.SINGLE_DOWN == entry.trend()).count() == 3;

        // Soon too high
        boolean soonTooHigh = last3Entries.stream().filter(entry -> Trend.DOUBLE_UP == entry.trend() || Trend.SINGLE_UP == entry.trend()).count() == 3;

        if (soonTooLow) {
            if (currentEntry.sgv() <= Constants.DEFAULT_SOON_TOO_LOW) {
                String title = translator.get(I18nKeys.PREDICTION_TITLE_TOO_LOW);
                String msg   = translator.get(I18nKeys.PREDICTION_TOO_LOW);
                Notification notification = NotificationBuilder.create().title(title).message(msg).image(icon).build();
                if (PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_LOW_NOTIFICATION)) { notificationSound.play(); }
                Platform.runLater(() -> notifier.notify(notification));
                return true;
            }
        } else if (soonTooHigh) {
            if (currentEntry.sgv() > Constants.DEFAULT_SOON_TOO_HIGH) {
                String title = translator.get(I18nKeys.PREDICTION_TITLE_TOO_HIGH);
                String msg   = translator.get(I18nKeys.PREDICTION_TOO_HIGH);
                Notification notification = NotificationBuilder.create().title(title).message(msg).image(icon).build();
                if (PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_HIGH_NOTIFICATION)) { notificationSound.play(); }
                Platform.runLater(() -> notifier.notify(notification));
                return true;
            }
        }
        return false;
    }

    private void notifyIfNeeded() {
        Trend         trend                                  = currentEntry.trend();
        ZonedDateTime now                                    = ZonedDateTime.now();
        double        value                                  = currentEntry.sgv();
        double        maxCritical                            = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_CRITICAL);
        double        maxAcceptable                          = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE);
        double        maxNormal                              = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL);
        double        minNormal                              = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL);
        double        minAcceptable                          = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE);
        double        minCritical                            = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_CRITICAL);
        boolean       showHighValueNotification              = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_HIGH_VALUE_NOTIFICATION);
        boolean       showAcceptableHighValueNotification    = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION);
        boolean       showLowValueNotification               = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_LOW_VALUE_NOTIFICATION);
        boolean       showAcceptableLowValueNotification     = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION);
        boolean       playSoundForTooHighNotification        = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_HIGH_NOTIFICATION);
        boolean       playSoundForHighNotification           = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_HIGH_NOTIFICATION);
        boolean       playSoundForAcceptableHighNotification = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_HIGH_NOTIFICATION);
        boolean       playSoundForTooLowNotification         = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_LOW_NOTIFICATION);
        boolean       playSoundForLowNotification            = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_LOW_NOTIFICATION);
        boolean       playSoundForAcceptableLowNotification  = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_LOW_NOTIFICATION);
        long          criticalMaxNotificationInterval        = PropertyManager.INSTANCE.getLong(Constants.PROPERTIES_CRITICAL_MAX_NOTIFICATION_INTERVAL);
        long          criticalMinNotificationInterval        = PropertyManager.INSTANCE.getLong(Constants.PROPERTIES_CRITICAL_MIN_NOTIFICATION_INTERVAL);

        boolean playSound = false;
        String  msg       = "";

        if (value > maxCritical) {
            // TOO HIGH
            if (Trend.SINGLE_DOWN == trend || Trend.DOUBLE_DOWN == trend) {
                return; // Was critical but is falling again -> no notification
            } else if (trend == Trend.FLAT || Trend.FORTY_FIVE_DOWN == trend) {
                if (now.toEpochSecond() - lastNotification.toEpochSecond() > criticalMaxNotificationInterval) {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_TOO_HIGH);
                    if (playSoundForTooHighNotification) { playSound = true; }
                }
            } else {
                msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_TOO_HIGH);
                if (playSoundForTooHighNotification) {  playSound = true;  }
            }
        } else if (value > maxAcceptable) {
            // HIGH
            if (showHighValueNotification) {
                // High
                if (Trend.SINGLE_UP == trend || Trend.DOUBLE_UP == trend) {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_TOO_HIGH_SOON);
                } else {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_HIGH);
                }
                if (playSoundForHighNotification) {  playSound = true;  }
            } else {
                msg = "";
            }
        } else if (value > maxNormal) {
            // ACCEPTABLE HIGH
            if (showAcceptableHighValueNotification) {
                if (Trend.SINGLE_UP == trend || Trend.DOUBLE_UP == trend) {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_HIGH_SOON);
                } else {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_A_BIT_HIGH);
                }
                if (playSoundForAcceptableHighNotification) {  playSound = true;  }
            } else {
                msg = "";
            }
        } else if (value > minNormal) {
            // NORMAL
        } else if (value > minAcceptable) {
            // ACCEPTABLE LOW
            if (showAcceptableLowValueNotification) {
                if (Trend.SINGLE_DOWN == trend || Trend.DOUBLE_DOWN == trend) {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_LOW_SOON);
                } else {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_A_BIT_LOW);
                }
                if (playSoundForAcceptableLowNotification) {  playSound = true;  }
            } else {
                msg = "";
            }
        } else if (value > minCritical) {
            // LOW
            if (showLowValueNotification) {
                if (Trend.SINGLE_DOWN == trend || Trend.DOUBLE_DOWN == trend) {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_TOO_LOW_SOON);
                } else {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_LOW);
                }
                if (playSoundForLowNotification) {  playSound = true;  }
            } else {
                msg = "";
            }
        } else {
            // TOO LOW
            if (Trend.SINGLE_UP == trend || Trend.DOUBLE_UP == trend) {
                return; // Was critical but is rising again -> no notification
            } else if (Trend.FLAT == trend || Trend.FORTY_FIVE_UP == trend) {
                if (now.toEpochSecond() - lastNotification.toEpochSecond() > criticalMinNotificationInterval) {
                    msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_TOO_LOW);
                    if (playSoundForTooLowNotification) {  playSound = true;  }
                }
            } else {
                msg = translator.get(I18nKeys.NOTIFICATION_GLUCOSE_TOO_LOW);
                if (playSoundForTooLowNotification) {  playSound = true;  }
            }
        }


        if (msg.isEmpty()) { return; }

        String format = MILLIGRAM_PER_DECILITER == currentUnit ? "%.0f" : "%.1f";
        String body = new StringBuilder().append(msg).append(" (").append(String.format(Locale.US, format, currentEntry.sgv())).append(" ").append(currentEntry.trend().getSymbol()).append(")").toString();
        Notification notification = NotificationBuilder.create().title(translator.get(I18nKeys.NOTIFICATION_TITLE)).message(body).image(icon).build();

        if (playSound) { notificationSound.play(); }
        Platform.runLater(() -> notifier.notify(notification));

        lastNotification = now;
    }

    private void applySettingsToPreferences() {
        nightscoutUrlTextField.setText(PropertyManager.INSTANCE.getString(Constants.PROPERTIES_NIGHTSCOUT_URL));
        unitSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_UNIT_MG));
        deltaChartSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_DELTA_CHART));
        tooLowSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_LOW_NOTIFICATION));
        enableLowSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_LOW_VALUE_NOTIFICATION));
        lowSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_LOW_NOTIFICATION));
        enableAcceptableLowSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION));
        acceptableLowSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_LOW_NOTIFICATION));
        enableAcceptableHighSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION));
        acceptableHighSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_HIGH_NOTIFICATION));
        enableHighSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_HIGH_VALUE_NOTIFICATION));
        highSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_HIGH_NOTIFICATION));
        tooHighSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_HIGH_NOTIFICATION));
        tooLowIntervalSlider.setValue(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_TOO_LOW_INTERVAL));
        tooHighIntervalSlider.setValue(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_TOO_HIGH_INTERVAL));
        minAcceptableSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE) : Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE)));
        minNormalSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER     == currentUnit ? PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)     : Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)));
        maxNormalSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER     == currentUnit ? PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)     : Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)));
        maxAcceptableSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE) : Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE)));
    }

    private void savePreferencesToSettings() {
        PropertyManager.INSTANCE.setString(Constants.PROPERTIES_NIGHTSCOUT_URL, nightscoutUrlTextField.getText());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_UNIT_MG, unitSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_DELTA_CHART, deltaChartSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_LOW_NOTIFICATION, tooLowSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_LOW_VALUE_NOTIFICATION, enableLowSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_LOW_NOTIFICATION, lowSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION, enableAcceptableLowSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_LOW_NOTIFICATION, acceptableLowSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION, enableAcceptableHighSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_ACCEPTABLE_HIGH_NOTIFICATION, acceptableHighSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_HIGH_VALUE_NOTIFICATION, enableHighSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_HIGH_NOTIFICATION, highSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_PLAY_SOUND_FOR_TOO_HIGH_NOTIFICATION, tooHighSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setDouble(Constants.PROPERTIES_TOO_LOW_INTERVAL, tooLowIntervalSlider.getValue());
        PropertyManager.INSTANCE.setDouble(Constants.PROPERTIES_TOO_HIGH_INTERVAL, tooHighIntervalSlider.getValue());
        PropertyManager.INSTANCE.setDouble(Constants.PROPERTIES_MIN_ACCEPTABLE, UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? minAcceptableSlider.getValue() : Helper.mmolPerLiterToMgPerDeciliter(minAcceptableSlider.getValue()));
        PropertyManager.INSTANCE.setDouble(Constants.PROPERTIES_MIN_NORMAL,     UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? minNormalSlider.getValue()     : Helper.mmolPerLiterToMgPerDeciliter(minNormalSlider.getValue()));
        PropertyManager.INSTANCE.setDouble(Constants.PROPERTIES_MAX_NORMAL,     UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? maxNormalSlider.getValue()     : Helper.mmolPerLiterToMgPerDeciliter(maxNormalSlider.getValue()));
        PropertyManager.INSTANCE.setDouble(Constants.PROPERTIES_MAX_ACCEPTABLE, UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? maxAcceptableSlider.getValue() : Helper.mmolPerLiterToMgPerDeciliter(maxAcceptableSlider.getValue()));

        PropertyManager.INSTANCE.storeProperties();

        nightscoutUrl = nightscoutUrlTextField.getText();
        if (null != nightscoutUrl && !nightscoutUrl.isEmpty() && allEntries.isEmpty()) {
            updateEntries();
            if (null != service) { service.cancel(); }

            Helper.getEntriesFromLast30Days(nightscoutUrl + Constants.URL_API).thenAccept(l -> allEntries.addAll(l));

            service = new ScheduledService<>() {
                @Override protected Task<Void> createTask() {
                    Task task = new Task() {
                        @Override protected Object call() {
                            updateEntries();
                            return null;
                        }
                    };
                    return task;
                }
            };
            service.setPeriod(Duration.millis(60000));
            service.setRestartOnFailure(true);
            service.start();
        }

        updateSettings();
        updateUI();
    }

    private void updateSettings() {
        currentUnit         = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_UNIT_MG) ? MILLIGRAM_PER_DECILITER : MILLIMOL_PER_LITER;
        deltaChartVisible   = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_DELTA_CHART);
        minAcceptable       = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE);
        minNormal           = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL);
        maxNormal           = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL);
        maxAcceptable       = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE);
        minAcceptableFactor = 1.0 - minAcceptable / Constants.DEFAULT_GLUCO_RANGE;
        minNormalFactor     = 1.0 - minNormal     / Constants.DEFAULT_GLUCO_RANGE;
        maxNormalFactor     = 1.0 - maxNormal     / Constants.DEFAULT_GLUCO_RANGE;
        maxAcceptableFactor = 1.0 - maxAcceptable / Constants.DEFAULT_GLUCO_RANGE;
    }

    private void updateUI() {
        if (allEntries.isEmpty()) { return; }
        Collections.sort(allEntries, Comparator.comparingLong(GlucoEntry::datelong).reversed());
        long limit = Instant.now().getEpochSecond() - currentInterval.getSeconds();
        entries      = allEntries.stream().filter(entry -> entry.datelong() > limit).collect(Collectors.toList());
        currentEntry = entries.get(0);
        currentColor = null == currentEntry ? Constants.GRAY : Helper.getColorForValue(currentUnit, currentEntry.sgv());

        deltas.clear();
        if (allEntries.size() > 13) {
            for (int i = 12; i > 0; i--) {
                double delta;
                if (MILLIGRAM_PER_DECILITER == currentUnit) {
                    delta = allEntries.get(i - 1).sgv() - allEntries.get(i).sgv();
                    deltas.add(delta);
                } else {
                    delta = Helper.mgPerDeciliterToMmolPerLiter(allEntries.get(i - 1).sgv()) - Helper.mgPerDeciliterToMmolPerLiter(allEntries.get(i).sgv());
                    deltas.add(delta);
                }
            }
            deltaMin = deltas.stream().min(Comparator.naturalOrder()).get();
            deltaMax = deltas.stream().max(Comparator.naturalOrder()).get();
        }
        if (MILLIGRAM_PER_DECILITER == currentUnit) {
            slowlyRising  = deltas.stream().limit(4).filter(delta -> delta > 0).filter(delta -> delta < 3).count() == 4;
            slowlyFalling = deltas.stream().limit(4).filter(delta -> delta < 0).filter(delta -> delta > -3).count() == 4;
        } else {
            slowlyRising  = deltas.stream().limit(4).filter(delta -> delta > 0).filter(delta -> delta < Helper.mgPerDeciliterToMmolPerLiter(3)).count() == 4;
            slowlyFalling = deltas.stream().limit(4).filter(delta -> delta < 0).filter(delta -> delta > Helper.mgPerDeciliterToMmolPerLiter(-3)).count() == 4;
        }

        String format           = MILLIGRAM_PER_DECILITER == currentUnit ? "%.0f" : "%.1f";
        double currentValue     = UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? currentEntry.sgv() : Helper.mgPerDeciliterToMmolPerLiter(currentEntry.sgv());
        String currentValueText = new StringBuilder().append(String.format(Locale.US, format, currentValue)).append(" ").append(currentEntry.trend().getSymbol()).toString();

        Instant lastTimestamp = Instant.ofEpochSecond(currentEntry.datelong());
        outdated = (Instant.now().getEpochSecond() - lastTimestamp.getEpochSecond() > Constants.TIMEOUT_IN_SECONDS);
        LocalDateTime dateTime = LocalDateTime.ofInstant(lastTimestamp, ZoneId.systemDefault());
        if (MILLIGRAM_PER_DECILITER == currentUnit) {
            avg = entries.stream().map(entry -> entry.sgv()).collect(Collectors.summingDouble(Double::doubleValue)) / entries.size();
        } else {
            avg = entries.stream().map(entry -> Helper.mgPerDeciliterToMmolPerLiter(entry.sgv())).collect(Collectors.summingDouble(Double::doubleValue)) / entries.size();
        }

        // Set value specific tray icon
        if (null != trayIcon && OperatingSystem.MACOS == operatingSystem) {
            SwingUtilities.invokeLater(() -> Platform.runLater(() -> {
                String text = currentValueText + (outdated ? "\u26A0" : "");
                trayIcon.setGraphic(Helper.createTextTrayIcon(text, darkMode ? Color.WHITE : Color.BLACK));
                trayIcon.setTrayIconTooltip(text);
            }));
        }
        Platform.runLater(() -> {
            unit.setText(currentUnit.UNIT.getUnitShort() + " (");
            if (deltas.isEmpty()) {
                delta4.setText("-, ");
                delta3.setText("-, ");
                delta2.setText("-, ");
                delta1.setText("-, ");
                delta0.setText("-)");
            } else {
                delta4.setText(String.format(Locale.US, "%+.1f, ", deltas.get(7)));
                delta3.setText(String.format(Locale.US, "%+.1f, ", deltas.get(8)));
                delta2.setText(String.format(Locale.US, "%+.1f, ", deltas.get(9)));
                delta1.setText(String.format(Locale.US, "%+.1f, ", deltas.get(10)));
                delta0.setText(String.format(Locale.US, "%+.1f)", deltas.get(11)));
            }

            mainPane.setBackground(new Background(new BackgroundFill(currentColor, CornerRadii.EMPTY, Insets.EMPTY)));
            valueLabel.setText(currentValueText);
            timestampLabel.setText(Constants.DTF.format(dateTime) + (outdated ? " \u26A0" : ""));
            exclamationMark.setVisible(outdated);
            rangeAverageLabel.setText(currentInterval.getUiString() + " (\u2300" + String.format(Locale.US, format, avg) + ")");

            drawChart();
        });

        predict();

        notifyIfNeeded();
    }

    private void drawChart() {
        if (entries.isEmpty()) { return; }
        Collections.sort(entries, Comparator.comparingLong(GlucoEntry::datelong));
        double width           = canvas.getWidth();
        double height          = canvas.getHeight();
        double availableWidth  = (width - GRAPH_INSETS.getLeft() - GRAPH_INSETS.getRight());
        double availableHeight = (height - GRAPH_INSETS.getTop() - GRAPH_INSETS.getBottom());

        ctx.clearRect(0, 0, width, height);
        ctx.setFill(Color.rgb(30, 28, 26));
        ctx.fillRect(0, 0, width, height);
        ctx.setFont(ticklabelFont);
        ctx.setFill(Constants.BRIGHT_TEXT);
        ctx.setStroke(Color.rgb(81, 80, 78));
        ctx.setLineDashes(3, 4);
        ctx.setLineWidth(1);
        List<String> yAxisLabels = MILLIGRAM_PER_DECILITER == currentUnit ? Constants.yAxisLabelsMgPerDeciliter : Constants.yAxisLabelsMmolPerLiter;

        // Draw vertical grid lines
        GlucoEntry    minEntry        = entries.get(0);
        GlucoEntry    maxEntry        = entries.get(entries.size() - 1);
        double        deltaTime       = (maxEntry.datelong() - minEntry.datelong());
        double        stepX           = availableWidth / deltaTime;
        double        stepY           =  availableHeight / (Constants.DEFAULT_GLUCO_RANGE);
        ZonedDateTime minDate         = Helper.getZonedDateTimeFromEpochSeconds(minEntry.datelong());
        int           hour            = minDate.getHour();
        ZonedDateTime adjMinDate      = hour == 23 ? minDate.plusSeconds(TimeInterval.LAST_24_HOURS.getSeconds()) : minDate;
        ZonedDateTime firstFullHour   = hour == 23 ? ZonedDateTime.of(adjMinDate.plusDays(1).toLocalDate(), LocalTime.MIDNIGHT, ZoneId.systemDefault()) : adjMinDate;
        long          startX          = firstFullHour.toEpochSecond() - minEntry.datelong();
        int           lh              = -1;
        double        nightLeftBounds = -1;
        double       nightRightBounds = -1;
        ctx.setFill(Color.rgb(255, 255, 255, 0.1));
        for (long i = startX ; i <= deltaTime ; i++) {
            int    h = ZonedDateTime.ofInstant(Instant.ofEpochSecond(i + minEntry.datelong()), ZoneId.systemDefault()).getHour();
            double x = GRAPH_INSETS.getLeft() + i * stepX;
            if (h != lh && lh != -1) {
                if (h == 20) {
                    nightLeftBounds = x;
                } else if (h == 6) {
                    nightRightBounds = x;
                    if (nightLeftBounds == -1) {
                        nightLeftBounds = startX * stepX;
                    }
                    if (nightRightBounds > nightLeftBounds) {
                        ctx.fillRect(nightLeftBounds, GRAPH_INSETS.getTop(), nightRightBounds - nightLeftBounds, availableHeight);
                    }
                }
            }
            lh = h;
        }

        ctx.setFill(Constants.BRIGHT_TEXT);
        ctx.setTextAlign(TextAlignment.CENTER);
        long interval;
        switch(currentInterval) {
            case LAST_168_HOURS,
                 LAST_720_HOURS,
                 LAST_72_HOURS -> interval = TimeInterval.LAST_6_HOURS.getHours();
            case LAST_48_HOURS -> interval = TimeInterval.LAST_3_HOURS.getHours();
            default            -> interval = 1;
        }
        long hourCounter = 0;
        for (long i = startX ; i <= deltaTime ; i++) {
            int    h = ZonedDateTime.ofInstant(Instant.ofEpochSecond(i + minEntry.datelong()), ZoneId.systemDefault()).getHour();
            double x = GRAPH_INSETS.getLeft() + i * stepX;
            if (h != lh && lh != -1 && i != startX) {
                if (hourCounter % interval == 0) {
                    ctx.strokeLine(x, GRAPH_INSETS.getTop(), x, height - GRAPH_INSETS.getBottom());
                    switch (currentInterval) {
                        case LAST_3_HOURS, LAST_6_HOURS -> ctx.fillText(h + ":00", x, height - GRAPH_INSETS.getBottom() * 0.5);
                        default -> ctx.fillText(Integer.toString(h), x, height - GRAPH_INSETS.getBottom() * 0.25);
                    }
                }
                hourCounter++;
            }
            lh = h;
        }

        // Draw horizontal grid lines
        ctx.setTextAlign(TextAlignment.RIGHT);
        double yLabelStep = availableHeight / yAxisLabels.size();
        for (int i = 0 ; i < yAxisLabels.size() ; i++) {
            double y = height - GRAPH_INSETS.getBottom() - i * yLabelStep - yLabelStep;
            ctx.strokeLine(GRAPH_INSETS.getLeft(), y, width - GRAPH_INSETS.getRight(), y);
            ctx.fillText(yAxisLabels.get(i), GRAPH_INSETS.getLeft() * 2.5, y + 4);
        }

        // Draw normal area
        double minNormal    = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL) * stepY;
        double maxNormal    = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL) * stepY;
        double heightNormal = (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)) * stepY;
        ctx.setFill(HelperFX.getColorWithOpacity(Constants.GREEN, 0.1));
        ctx.setStroke(Constants.GREEN);
        ctx.setLineWidth(1);
        ctx.setLineDashes();
        ctx.fillRect(3 * GRAPH_INSETS.getLeft(), maxNormal, availableWidth, heightNormal);
        ctx.strokeLine( 3 * GRAPH_INSETS.getLeft(), minNormal, width - GRAPH_INSETS.getRight(), minNormal);
        ctx.strokeLine(3 * GRAPH_INSETS.getLeft(), maxNormal, width - GRAPH_INSETS.getRight(), maxNormal);

        // Draw average
        double average = (height - GRAPH_INSETS.getBottom()) - avg * stepY;
        ctx.setStroke(Constants.GRAY);
        ctx.strokeLine(GRAPH_INSETS.getLeft() * 3, average, width - GRAPH_INSETS.getRight(), average);

        // Draw delta chart
        if (deltaChartVisible) {
            ctx.setStroke(Constants.BRIGHT_TEXT);
            ctx.setLineWidth(0.5);
            double offsetX  = GRAPH_INSETS.getLeft() + (availableWidth - Constants.DELTA_CHART_WIDTH) * 0.5;
            double factorY  = Constants.DELTA_CHART_HEIGHT / Math.max(Math.abs(deltaMax), Math.abs(deltaMin));
            double boxWidth = 5;
            double spacer   = 5;
            double zeroY    = GRAPH_INSETS.getTop() + 50;
            if (deltas.size() > 0) {
                for (int i = 0; i < 12; i++) {
                    double delta = MILLIGRAM_PER_DECILITER == currentUnit ? deltas.get(i) : Helper.mmolPerLiterToMgPerDeciliter(deltas.get(i));
                    ctx.strokeRect(offsetX + i * (boxWidth + spacer), delta > 0 ? zeroY - Math.abs(delta * factorY) : zeroY, boxWidth, Math.abs(delta) * factorY);
                }
            }
        }

        // Draw line chart
        ctx.setLineDashes();
        ctx.setStroke(new LinearGradient(0, GRAPH_INSETS.getTop(), 0, height - GRAPH_INSETS.getBottom(), false, CycleMethod.NO_CYCLE,
                                         new Stop(0.0, Constants.RED),
                                         new Stop(Constants.DEFAULT_MAX_CRITICAL_FACTOR, Constants.RED),
                                         new Stop(maxAcceptableFactor, Constants.ORANGE),
                                         new Stop(maxNormalFactor, Constants.GREEN),
                                         new Stop(minNormalFactor, Constants.GREEN),
                                         new Stop(minAcceptableFactor, Constants.ORANGE),
                                         new Stop(Constants.DEFAULT_MIN_CRITICAL_FACTOR, Constants.RED),
                                         new Stop(1.0, Constants.RED)));

        ctx.setLineWidth(2);
        ctx.beginPath();
        ctx.moveTo(GRAPH_INSETS.getLeft() + startX + (entries.get(0).datelong() - minEntry.datelong()) * stepX, height - GRAPH_INSETS.getBottom() - entries.get(0).sgv() * stepY);
        for (int i = 0 ; i < entries.size() ; i++) {
            GlucoEntry entry = entries.get(i);
            ctx.lineTo(GRAPH_INSETS.getLeft() + startX + (entry.datelong() - minEntry.datelong()) * stepX, (height - GRAPH_INSETS.getBottom()) - entry.sgv() * stepY);
        }
        ctx.stroke();
    }


    // ******************** Factory methods ***********************************
    private Text createDeltaText(final String text, final boolean bold, final double size) {
        Text t = new Text(text);
        t.setFont(bold ? Fonts.sfProRoundedBold(size) : Fonts.sfProRoundedRegular(size));
        t.setFill(Constants.BRIGHT_TEXT);
        return t;
    }

    private Label createLabel(final String text, final double size, final boolean bold, final boolean rounded, final Pos alignment) {
        return createLabel(text, size, Constants.BRIGHT_TEXT, bold, rounded, alignment);
    }
    private Label createLabel(final String text, final double size, final Paint color, final boolean bold, final boolean rounded, final Pos alignment) {
        Label label = new Label(text);
        if (rounded) {
            label.setFont(bold ? Fonts.sfProRoundedBold(size) : Fonts.sfProRoundedRegular(size));
        } else {
            label.setFont(bold ? Fonts.sfProTextBold(size) : Fonts.sfProTextRegular(size));
        }
        label.setTextFill(color);
        label.setAlignment(alignment);
        return label;
    }
    private Label createLabel(final String text, final double size, final Paint color, final boolean bold, final Pos alignment, final Priority priority) {
        Label label = new Label(text);
        label.setFont(bold ? Fonts.sfProTextBold(size) : Fonts.sfProTextRegular(size));
        label.setTextFill(color);
        label.setAlignment(alignment);
        label.setPrefWidth(250);
        HBox.setHgrow(label, priority);
        return label;
    }


    private ToggleButton createToggleButton(final String text, final ToggleGroup toggleGroup, final EventHandler<MouseEvent> handler) {
        ToggleButton toggleButton = new ToggleButton(text);
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setToggleGroup(toggleGroup);
        toggleButton.addEventFilter(MouseEvent.MOUSE_PRESSED, handler);
        HBox.setHgrow(toggleButton, Priority.ALWAYS);
        return toggleButton;
    }


    // ******************** About *********************************************
    private Dialog createAboutDialog() {
        Dialog aboutDialog = new Dialog();
        aboutDialog.initOwner(stage);
        aboutDialog.setTitle(translator.get(I18nKeys.APP_NAME));
        aboutDialog.initStyle(StageStyle.TRANSPARENT);
        aboutDialog.initModality(Modality.WINDOW_MODAL);

        DialogPane dialogPane = new DialogPane() {
            @Override protected Node createButtonBar() {
                ButtonBar buttonBar = (ButtonBar) super.createButtonBar();
                buttonBar.setPadding(new Insets(0));
                buttonBar.setVisible(false);
                buttonBar.setManaged(false);
                return buttonBar;
            }
        };
        aboutDialog.setDialogPane(dialogPane);

        Image img = new Image(Main.class.getResourceAsStream("icon128x128.png"));

        Stage aboutDialogStage = (Stage) aboutDialog.getDialogPane().getScene().getWindow();
        aboutDialogStage.getScene().setFill(Color.TRANSPARENT);
        aboutDialogStage.setAlwaysOnTop(true);
        aboutDialogStage.getScene().getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        ImageView aboutImage = new ImageView(img);
        aboutImage.setFitWidth(64);
        aboutImage.setFitHeight(64);

        Label nameLabel = new Label(translator.get(I18nKeys.APP_NAME));
        nameLabel.setPrefWidth(Label.USE_COMPUTED_SIZE);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setFont(Fonts.sfProTextBold(14));
        nameLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        Label descriptionLabel = new Label("\u00A9 Gerrit Grunwald 2022");
        descriptionLabel.setPrefWidth(Label.USE_COMPUTED_SIZE);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        descriptionLabel.setFont(Fonts.sfProTextRegular(12));
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.setAlignment(Pos.CENTER);
        descriptionLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        MacosButton closeButton = new MacosButton(translator.get(I18nKeys.ABOUT_ALERT_CLOSE_BUTTON));
        closeButton.setPrefWidth(Button.USE_COMPUTED_SIZE);
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setOnAction(e -> {
            aboutDialog.setResult(Boolean.TRUE);
            aboutDialog.close();
        });
        closeButton.setMinHeight(28);
        closeButton.setMaxHeight(28);
        closeButton.setPrefHeight(28);
        VBox.setMargin(closeButton, new Insets(20, 0, 0, 0));

        VBox aboutTextBox = new VBox(10, nameLabel, descriptionLabel, closeButton);
        aboutTextBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox aboutBox = new VBox(20, aboutImage, aboutTextBox);
        aboutBox.setAlignment(Pos.CENTER);
        aboutBox.setPadding(new Insets(20, 20, 20, 20));
        aboutBox.setMinSize(260, 232);
        aboutBox.setMaxSize(260, 232);
        aboutBox.setPrefSize(260, 232);
        aboutBox.setBackground(new Background(new BackgroundFill(MacosSystemColor.BACKGROUND.getColorDark(), new CornerRadii(10), Insets.EMPTY)));


        if (OperatingSystem.LINUX == operatingSystem && (Architecture.AARCH64 == architecture || Architecture.ARM64 == architecture)) {
            aboutDialog.getDialogPane().setContent(new StackPane(aboutBox));
        } else {
            StackPane glassPane = new StackPane(aboutBox);
            glassPane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
            glassPane.setMinSize(260, 232);
            glassPane.setMaxSize(260, 232);
            glassPane.setPrefSize(260, 232);
            glassPane.setEffect(new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.35), 10.0, 0.0, 0.0, 5));
            aboutDialog.getDialogPane().setContent(glassPane);
        }

        aboutDialog.getDialogPane().setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));

        aboutDialog.setOnShowing(e -> aboutDialogStage.centerOnScreen());


        return aboutDialog;
    }


    // ******************** Settings ******************************************
    private StackPane createPrefPane() {
        String format = MILLIGRAM_PER_DECILITER == currentUnit ? "%.0f" : "%.1f";

        MacosButton backButton = new MacosButton("\u2190");
        backButton.setFont(Fonts.sfProRoundedSemiBold(16));
        backButton.setPadding(new Insets(2, 5, 2, 5));
        AnchorPane.setTopAnchor(backButton, 10d);
        AnchorPane.setLeftAnchor(backButton, 10d);
        backButton.setOnAction(e -> {
            prefPane.setVisible(false);
            prefPane.setManaged(false);
            savePreferencesToSettings();
        });

        Label settingsLabel = new Label(translator.get(I18nKeys.SETTINGS_TITLE));
        settingsLabel.setFont(Fonts.sfProTextBold(14));
        settingsLabel.setTextFill(Constants.BRIGHT_TEXT);
        AnchorPane.setTopAnchor(settingsLabel, 50d);
        AnchorPane.setLeftAnchor(settingsLabel, 30d);

        Label nightscoutUrlLabel = new Label(translator.get(I18nKeys.SETTINGS_NIGHTSCOUT_URL));
        nightscoutUrlLabel.setFont(Fonts.sfProTextRegular(14));
        nightscoutUrlLabel.setTextFill(Constants.BRIGHT_TEXT);
        nightscoutUrlTextField = new MacosTextField();
        nightscoutUrlTextField.setDark(true);
        nightscoutUrlTextField.setFont(Fonts.sfProRoundedRegular(14));
        nightscoutUrlTextField.setPrefWidth(TextField.USE_COMPUTED_SIZE);
        nightscoutUrlTextField.setMaxWidth(Double.MAX_VALUE);
        nightscoutUrlTextField.setPromptText("https://YOUR_DOMAIN.herokuapp.com");
        HBox.setHgrow(nightscoutUrlTextField, Priority.ALWAYS);
        HBox nightscoutUrlBox = new HBox(10, nightscoutUrlLabel, nightscoutUrlTextField);
        nightscoutUrlBox.setAlignment(Pos.CENTER);


        MacosSeparator s1 = new MacosSeparator(Orientation.HORIZONTAL);
        s1.setDark(true);
        VBox.setMargin(s1, new Insets(5, 0, 5, 0));


        unitSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        Label unitLabel = new Label((UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? translator.get(I18nKeys.SETTINGS_UNIT_MG) : translator.get(I18nKeys.SETTINGS_UNIT_MMOL)) + currentUnit.UNIT.getUnitShort());
        unitLabel.setFont(Fonts.sfProTextRegular(14));
        unitLabel.setTextFill(Constants.BRIGHT_TEXT);
        HBox.setHgrow(unitLabel, Priority.ALWAYS);
        HBox unitBox = new HBox(10, unitSwitch, unitLabel);
        unitBox.setAlignment(Pos.CENTER_LEFT);
        unitSwitch.selectedProperty().addListener((o, ov, nv) -> {
            currentUnit = nv ? MILLIGRAM_PER_DECILITER : MILLIMOL_PER_LITER;
            if (MILLIGRAM_PER_DECILITER == currentUnit) {
                minAcceptableSlider.setMin(Constants.SETTINGS_MIN_ACCEPTABLE_MIN);
                minAcceptableSlider.setMax(Constants.SETTINGS_MIN_ACCEPTABLE_MAX);
                minAcceptableSlider.setMajorTickUnit(1);
                minAcceptableSlider.setBlockIncrement(1);
                minAcceptableSlider.setValue(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE));

                minNormalSlider.setMin(Constants.SETTINGS_MIN_NORMAL_MIN);
                minNormalSlider.setMax(Constants.SETTINGS_MIN_NORMAL_MAX);
                minNormalSlider.setMajorTickUnit(1);
                minNormalSlider.setBlockIncrement(1);
                minNormalSlider.setValue(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL));

                maxNormalSlider.setMin(Constants.SETTINGS_MAX_NORMAL_MIN);
                maxNormalSlider.setMax(Constants.SETTINGS_MAX_NORMAL_MAX);
                maxNormalSlider.setMajorTickUnit(5);
                maxNormalSlider.setBlockIncrement(5);
                maxNormalSlider.setValue(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL));

                maxAcceptableSlider.setMin(Constants.SETTINGS_MAX_ACCEPTABLE_MIN);
                maxAcceptableSlider.setMax(Constants.SETTINGS_MAX_ACCEPTABLE_MAX);
                maxAcceptableSlider.setMajorTickUnit(5);
                maxAcceptableSlider.setBlockIncrement(5);
                maxAcceptableSlider.setValue(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE));
            } else {
                minAcceptableSlider.setMin(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_ACCEPTABLE_MIN));
                minAcceptableSlider.setMax(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_ACCEPTABLE_MAX));
                minAcceptableSlider.setMajorTickUnit(Helper.mgPerDeciliterToMmolPerLiter(1));
                minAcceptableSlider.setBlockIncrement(Helper.mgPerDeciliterToMmolPerLiter(1));
                minAcceptableSlider.setValue(Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE)));

                minNormalSlider.setMin(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_NORMAL_MIN));
                minNormalSlider.setMax(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_NORMAL_MAX));
                minNormalSlider.setMajorTickUnit(Helper.mgPerDeciliterToMmolPerLiter(1));
                minNormalSlider.setBlockIncrement(Helper.mgPerDeciliterToMmolPerLiter(1));
                minNormalSlider.setValue(Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)));

                maxNormalSlider.setMin(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_NORMAL_MIN));
                maxNormalSlider.setMax(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_NORMAL_MAX));
                maxNormalSlider.setMajorTickUnit(Helper.mgPerDeciliterToMmolPerLiter(5));
                maxNormalSlider.setBlockIncrement(Helper.mgPerDeciliterToMmolPerLiter(5));
                maxNormalSlider.setValue(Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)));

                maxAcceptableSlider.setMin(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_ACCEPTABLE_MIN));
                maxAcceptableSlider.setMax(Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_ACCEPTABLE_MAX));
                maxAcceptableSlider.setMajorTickUnit(Helper.mgPerDeciliterToMmolPerLiter(5));
                maxAcceptableSlider.setBlockIncrement(Helper.mgPerDeciliterToMmolPerLiter(5));
                maxAcceptableSlider.setValue(Helper.mgPerDeciliterToMmolPerLiter(PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE)));
            }
        });


        MacosSeparator s2 = new MacosSeparator(Orientation.HORIZONTAL);
        s2.setDark(true);
        VBox.setMargin(s2, new Insets(5, 0, 5, 0));


        deltaChartSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        Label deltaChartLabel = new Label(translator.get(I18nKeys.SETTINGS_SHOW_DELTA_CHART));
        deltaChartLabel.setFont(Fonts.sfProTextRegular(14));
        deltaChartLabel.setTextFill(Constants.BRIGHT_TEXT);
        HBox.setHgrow(deltaChartLabel, Priority.ALWAYS);
        HBox deltaChartBox = new HBox(10, deltaChartSwitch, deltaChartLabel);
        deltaChartBox.setAlignment(Pos.CENTER_LEFT);


        MacosSeparator s3 = new MacosSeparator(Orientation.HORIZONTAL);
        s3.setDark(true);
        VBox.setMargin(s3, new Insets(5, 0, 5, 0));


        Label notificationsLabel = new Label(translator.get(I18nKeys.SETTINGS_NOTIFICATION_TITLE));
        notificationsLabel.setFont(Fonts.sfProTextBold(14));
        notificationsLabel.setTextFill(Constants.BRIGHT_TEXT);

        // Too low
        Label tooLowLabel      = createLabel(translator.get(I18nKeys.SETTINGS_TOO_LOW_VALUE_NOTIFICATION), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        Label tooLowSoundLabel = createLabel(translator.get(I18nKeys.SETTINGS_TOO_LOW_VALUE_NOTIFICATION_SOUND), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT, Priority.ALWAYS);
        tooLowSoundLabel.setMaxWidth(Double.MAX_VALUE);
        tooLowSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        HBox tooLowBox = new HBox(10, tooLowLabel, tooLowSoundLabel, tooLowSoundSwitch);
        tooLowBox.setAlignment(Pos.CENTER_LEFT);

        // Low
        enableLowSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        Label lowLabel      = createLabel(translator.get(I18nKeys.SETTINGS_LOW_VALUE_NOTIFICATION), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        Label lowSoundLabel = createLabel(translator.get(I18nKeys.SETTINGS_LOW_VALUE_NOTIFICATION_SOUND), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT, Priority.ALWAYS);
        lowSoundLabel.setMaxWidth(Double.MAX_VALUE);
        lowSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        HBox lowBox = new HBox(10, enableLowSoundSwitch, lowLabel, lowSoundLabel, lowSoundSwitch);
        lowBox.setAlignment(Pos.CENTER_LEFT);

        // Acceptable low
        enableAcceptableLowSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        Label acceptableLowLabel      = createLabel(translator.get(I18nKeys.SETTINGS_ACCEPTABLE_LOW_VALUE_NOTIFICATION), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        Label acceptableLowSoundLabel = createLabel(translator.get(I18nKeys.SETTINGS_ACCEPTABLE_LOW_VALUE_NOTIFICATION_SOUND), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT, Priority.ALWAYS);
        acceptableLowSoundLabel.setMaxWidth(Double.MAX_VALUE);
        acceptableLowSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        HBox acceptableLowBox = new HBox(10, enableAcceptableLowSoundSwitch, acceptableLowLabel, acceptableLowSoundLabel, acceptableLowSoundSwitch);
        acceptableLowBox.setAlignment(Pos.CENTER_LEFT);

        // Acceptable high
        enableAcceptableHighSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        Label acceptableHighLabel = createLabel(translator.get(I18nKeys.SETTINGS_ACCEPTABLE_HIGH_VALUE_NOTIFICATION), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        Label acceptableHighSoundLabel = createLabel(translator.get(I18nKeys.SETTINGS_ACCEPTABLE_HIGH_VALUE_NOTIFICATION_SOUND), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT, Priority.ALWAYS);
        acceptableHighSoundLabel.setMaxWidth(Double.MAX_VALUE);
        acceptableHighSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        HBox acceptableHighBox = new HBox(10, enableAcceptableHighSoundSwitch, acceptableHighLabel, acceptableHighSoundLabel, acceptableHighSoundSwitch);
        acceptableHighBox.setAlignment(Pos.CENTER_LEFT);

        // High
        enableHighSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        Label highLabel = createLabel(translator.get(I18nKeys.SETTINGS_HIGH_VALUE_NOTIFICATION), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        Label highSoundLabel = createLabel(translator.get(I18nKeys.SETTINGS_HIGH_VALUE_NOTIFICATION_SOUND), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT, Priority.ALWAYS);
        highSoundLabel.setMaxWidth(Double.MAX_VALUE);
        highSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        HBox highBox = new HBox(10, enableHighSoundSwitch, highLabel, highSoundLabel, highSoundSwitch);
        highBox.setAlignment(Pos.CENTER_LEFT);

        // Too high
        Label tooHighLabel = createLabel(translator.get(I18nKeys.SETTINGS_TOO_HIGH_VALUE_NOTIFICATION), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        Label tooHighSoundLabel = createLabel(translator.get(I18nKeys.SETTINGS_TOO_HIGH_VALUE_NOTIFICATION_SOUND), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT, Priority.ALWAYS);
        tooHighSoundLabel.setMaxWidth(Double.MAX_VALUE);
        tooHighSoundSwitch = MacosSwitchBuilder.create().dark(true).selectedColor(accentColor).build();
        HBox tooHighBox = new HBox(10, tooHighLabel, tooHighSoundLabel, tooHighSoundSwitch);
        tooHighBox.setAlignment(Pos.CENTER_LEFT);


        MacosSeparator s4 = new MacosSeparator(Orientation.HORIZONTAL);
        s4.setDark(true);
        VBox.setMargin(s4, new Insets(5, 0, 5, 0));


        // Too low interval
        Label tooLowIntervalLabel = createLabel(translator.get(I18nKeys.SETTINGS_TOO_LOW_NOTIFICATION_INTERVAL) + "5 min", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        tooLowIntervalSlider = new MacosSlider();
        tooLowIntervalSlider.setDark(true);
        tooLowIntervalSlider.setMin(5);
        tooLowIntervalSlider.setMax(10);
        tooLowIntervalSlider.setSnapToTicks(true);
        tooLowIntervalSlider.setShowTickMarks(true);
        tooLowIntervalSlider.setMinorTickCount(0);
        tooLowIntervalSlider.setMajorTickUnit(1);
        tooLowIntervalSlider.setBlockIncrement(1);
        tooLowIntervalSlider.valueProperty().addListener((o, ov, nv) -> tooLowIntervalLabel.setText(translator.get(I18nKeys.SETTINGS_TOO_LOW_NOTIFICATION_INTERVAL) + String.format(Locale.US, "%.0f", tooLowIntervalSlider.getValue()) + " min"));

        // Too high interval
        Label tooHighIntervalLabel = createLabel(translator.get(I18nKeys.SETTINGS_TOO_HIGH_NOTIFICATION_INTERVAL) + "5 min", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        tooHighIntervalSlider = new MacosSlider();
        tooHighIntervalSlider.setDark(true);
        tooHighIntervalSlider.setMin(5);
        tooHighIntervalSlider.setMax(30);
        tooHighIntervalSlider.setSnapToTicks(true);
        tooHighIntervalSlider.setShowTickMarks(true);
        tooHighIntervalSlider.setMinorTickCount(0);
        tooHighIntervalSlider.setMajorTickUnit(5);
        tooHighIntervalSlider.setBlockIncrement(5);
        tooHighIntervalSlider.valueProperty().addListener((o, ov, nv) -> tooHighIntervalLabel.setText(translator.get(I18nKeys.SETTINGS_TOO_HIGH_NOTIFICATION_INTERVAL) + String.format(Locale.US, "%.0f", tooHighIntervalSlider.getValue()) + " min"));


        MacosSeparator s5 = new MacosSeparator(Orientation.HORIZONTAL);
        s5.setDark(true);
        VBox.setMargin(s5, new Insets(5, 0, 5, 0));


        Label rangesLabel = new Label(translator.get(I18nKeys.SETTINGS_RANGES_TITLE));
        rangesLabel.setFont(Fonts.sfProTextBold(14));
        rangesLabel.setTextFill(Constants.BRIGHT_TEXT);

        Label minAcceptableLabel = createLabel(new StringBuilder().append(translator.get(I18nKeys.SETTINGS_MIN_ACCEPTABLE)).append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_ACCEPTABLE)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        minAcceptableSlider = new MacosSlider();
        minAcceptableSlider.setDark(true);
        minAcceptableSlider.setMin(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_ACCEPTABLE_MIN : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_ACCEPTABLE_MIN));
        minAcceptableSlider.setMax(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_ACCEPTABLE_MAX : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_ACCEPTABLE_MAX));
        minAcceptableSlider.setSnapToTicks(true);
        minAcceptableSlider.setShowTickMarks(true);
        minAcceptableSlider.setMinorTickCount(0);
        minAcceptableSlider.setMajorTickUnit(1);
        minAcceptableSlider.setBlockIncrement(1);
        minAcceptableSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_ACCEPTABLE));
        minAcceptableSlider.valueProperty().addListener((o, ov, nv) -> minAcceptableLabel.setText(new StringBuilder().append(translator.get(I18nKeys.SETTINGS_MIN_ACCEPTABLE)).append(String.format(Locale.US, format, minAcceptableSlider.getValue())).append(" ").append(currentUnit.UNIT.getUnitShort()).toString()));

        Label minNormalLabel = createLabel(new StringBuilder().append(translator.get(I18nKeys.SETTINGS_MIN_NORMAL)).append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_NORMAL : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_NORMAL)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        minNormalSlider = new MacosSlider();
        minNormalSlider.setDark(true);
        minNormalSlider.setMin(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_NORMAL_MIN : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_NORMAL_MIN));
        minNormalSlider.setMax(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_NORMAL_MAX : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_NORMAL_MAX));
        minNormalSlider.setSnapToTicks(true);
        minNormalSlider.setShowTickMarks(true);
        minNormalSlider.setMinorTickCount(0);
        minNormalSlider.setMajorTickUnit(1);
        minNormalSlider.setBlockIncrement(1);
        minNormalSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_NORMAL : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_NORMAL));
        minNormalSlider.valueProperty().addListener((o, ov, nv) -> minNormalLabel.setText(new StringBuilder().append(translator.get(I18nKeys.SETTINGS_MIN_NORMAL)).append(String.format(Locale.US, format, minNormalSlider.getValue())).append(" ").append(currentUnit.UNIT.getUnitShort()).toString()));

        Label maxNormalLabel = createLabel(new StringBuilder().append(translator.get(I18nKeys.SETTINGS_MAX_NORMAL)).append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MAX_NORMAL : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MAX_NORMAL)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        maxNormalSlider = new MacosSlider();
        maxNormalSlider.setDark(true);
        maxNormalSlider.setMin(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MAX_NORMAL_MIN : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_NORMAL_MIN));
        maxNormalSlider.setMax(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MAX_NORMAL_MAX : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_NORMAL_MAX));
        maxNormalSlider.setSnapToTicks(true);
        maxNormalSlider.setShowTickMarks(true);
        maxNormalSlider.setMinorTickCount(0);
        maxNormalSlider.setMajorTickUnit(5);
        maxNormalSlider.setBlockIncrement(5);
        maxNormalSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MAX_NORMAL : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MAX_NORMAL));
        maxNormalSlider.valueProperty().addListener((o, ov, nv) -> {
            maxNormalLabel.setText(new StringBuilder().append("Max normal: ").append(String.format(Locale.US, format, maxNormalSlider.getValue())).append(" ").append(currentUnit.UNIT.getUnitShort()).toString());
            if (nv.doubleValue() > maxAcceptableSlider.getValue()) { maxAcceptableSlider.setValue(nv.doubleValue()); }
        });

        Label maxAcceptableLabel = createLabel(new StringBuilder().append(translator.get(I18nKeys.SETTINGS_MAX_ACCEPTABLE)).append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MAX_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MAX_ACCEPTABLE)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        maxAcceptableSlider = new MacosSlider();
        maxAcceptableSlider.setDark(true);
        maxAcceptableSlider.setMin(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MAX_ACCEPTABLE_MIN : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_ACCEPTABLE_MIN));
        maxAcceptableSlider.setMax(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MAX_ACCEPTABLE_MAX : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_ACCEPTABLE_MAX));
        maxAcceptableSlider.setSnapToTicks(true);
        maxAcceptableSlider.setShowTickMarks(true);
        maxAcceptableSlider.setMinorTickCount(0);
        maxAcceptableSlider.setMajorTickUnit(5);
        maxAcceptableSlider.setBlockIncrement(5);
        maxAcceptableSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MAX_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MAX_ACCEPTABLE));
        maxAcceptableSlider.valueProperty().addListener((o, ov, nv) -> {
            maxAcceptableLabel.setText(new StringBuilder().append(translator.get(I18nKeys.SETTINGS_MAX_ACCEPTABLE)).append(String.format(Locale.US, format, maxAcceptableSlider.getValue())).append(" ").append(currentUnit.UNIT.getUnitShort()).toString());
            if (nv.doubleValue() < maxNormalSlider.getValue()) { maxNormalSlider.setValue(nv.doubleValue()); }
        });


        VBox settingsVBox = new VBox(10, nightscoutUrlBox, s1, unitBox, s2, deltaChartBox, s3,
                                     notificationsLabel, tooLowBox, lowBox, acceptableLowBox, acceptableHighBox, highBox, tooHighBox, s4,
                                     tooLowIntervalLabel, tooLowIntervalSlider, tooHighIntervalLabel, tooHighIntervalSlider, s5,
                                     rangesLabel, minAcceptableLabel, minAcceptableSlider, minNormalLabel, minNormalSlider, maxNormalLabel, maxNormalSlider, maxAcceptableLabel, maxAcceptableSlider);
        settingsVBox.setBackground(new Background(new BackgroundFill(MacosSystemColor.BACKGROUND.getColorDark(), CornerRadii.EMPTY, Insets.EMPTY)));
        settingsVBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        settingsVBox.setFillWidth(true);

        MacosScrollPane scrollPane = new MacosScrollPane(settingsVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setBackground(new Background(new BackgroundFill(MacosSystemColor.BACKGROUND.getColorDark(), CornerRadii.EMPTY, Insets.EMPTY)));
        AnchorPane.setTopAnchor(scrollPane, 80d);
        AnchorPane.setRightAnchor(scrollPane, 30d);
        AnchorPane.setBottomAnchor(scrollPane, 20d);
        AnchorPane.setLeftAnchor(scrollPane, 30d);

        AnchorPane pane = new AnchorPane(backButton, settingsLabel, scrollPane);
        pane.setBackground(new Background(new BackgroundFill(MacosSystemColor.BACKGROUND.getColorDark(), CornerRadii.EMPTY, Insets.EMPTY)));

        StackPane prefPane = new StackPane(pane);
        return prefPane;
    }


    // ******************** Time in Range Chart *******************************
    private void showTimeInRangeChart() {
        if (dialogVisible.get()) { return; }
        dialogVisible.set(true);
        double noOfValues = entries.size();
        double pTooHigh = (entries.stream().filter(entry -> entry.sgv() > Constants.DEFAULT_MAX_CRITICAL).count() / noOfValues);
        double pHigh    = (entries.stream().filter(entry -> entry.sgv() > PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)).filter(entry -> entry.sgv() <= PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_CRITICAL)).count() / noOfValues);
        double pNormal  = (entries.stream().filter(entry -> entry.sgv() > PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)).filter(entry -> entry.sgv() <= PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)).count() / noOfValues);
        double pLow     = (entries.stream().filter(entry -> entry.sgv() > Constants.DEFAULT_MIN_CRITICAL).filter(entry -> entry.sgv() <= PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)).count() / noOfValues);
        double pTooLow  = (entries.stream().filter(entry -> entry.sgv() < Constants.DEFAULT_MIN_CRITICAL).count() / noOfValues);

        Label titleLabel        = createLabel(translator.get(I18nKeys.STATISTICS_TITLE), 24, true, false, Pos.CENTER);
        Label timeIntervalLabel = createLabel(currentInterval.getUiString(), 20, false, false, Pos.CENTER);

        double columnSize = 140;
        Rectangle tooHighRect = createTimeInRangeRectangle(pTooHigh, columnSize, Constants.RED);
        Rectangle highRect    = createTimeInRangeRectangle(pHigh, columnSize, Constants.YELLOW);
        Rectangle normalRect  = createTimeInRangeRectangle(pNormal, columnSize, Constants.GREEN);
        Rectangle lowRect     = createTimeInRangeRectangle(pLow, columnSize, Constants.ORANGE);
        Rectangle tooLowRect  = createTimeInRangeRectangle(pTooLow, columnSize, Constants.RED);
        VBox rectBox = new VBox(tooHighRect, highRect, normalRect, lowRect, tooLowRect);

        Label tooHighValue     = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pTooHigh * 100), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT);
        Label tooHighValueText = createTimeInRangeLabel(translator.get(I18nKeys.STATISTICS_TOO_HIGH), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT);
        HBox  tooHighText      = new HBox(10, tooHighValue, tooHighValueText);

        Label highValue        = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pHigh * 100), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT);
        Label highValueText    = createTimeInRangeLabel(translator.get(I18nKeys.STATISTICS_HIGH), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT);
        HBox  highText         = new HBox(10, highValue, highValueText);

        Label normalValue      = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pNormal * 100), 22, Constants.BRIGHT_TEXT, true, Pos.CENTER_RIGHT);
        Label normalValueText  = createTimeInRangeLabel(translator.get(I18nKeys.STATISTICS_NORMAL), 22, Constants.BRIGHT_TEXT, true, Pos.CENTER_LEFT);
        HBox  normalText       = new HBox(10, normalValue, normalValueText);

        Label lowValue         = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pLow * 100), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT);
        Label lowValueText     = createTimeInRangeLabel(translator.get(I18nKeys.STATISTICS_LOW), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT);
        HBox  lowText          = new HBox(10, lowValue, lowValueText);

        Label tooLowValue      = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pTooLow * 100), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_RIGHT);
        Label tooLowValueText  = createTimeInRangeLabel(translator.get(I18nKeys.STATISTICS_TOO_LOW), 20, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT);
        HBox  tooLowText       = new HBox(10, tooLowValue, tooLowValueText);

        VBox  textBox = new VBox(5, tooHighText, highText, normalText, lowText, tooLowText);

        HBox inRangeBox = new HBox(10, rectBox, textBox);

        VBox content = new VBox(20, titleLabel, timeIntervalLabel, inRangeBox);
        content.setAlignment(Pos.CENTER);
        content.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));

        Dialog dialog = new Dialog();
        dialog.setTitle("");
        dialog.setHeaderText("");

        DialogPane dialogPane = new DialogPane() {
            @Override protected Node createButtonBar() {
                ButtonBar buttonBar = (ButtonBar) super.createButtonBar();
                buttonBar.getStyleClass().add("dialog-button-bar");
                buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
                return buttonBar;
            }
        };
        dialogPane.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        dialogPane.setBackground(new Background(new BackgroundFill(Constants.DARK_BACKGROUND, new CornerRadii(10), Insets.EMPTY)));
        dialogPane.setBorder(new Border(new BorderStroke(Color.rgb(78, 77, 76), BorderStrokeStyle.SOLID, new CornerRadii(10),new BorderWidths(1))));
        dialogPane.setContent(content);
        dialog.setDialogPane(dialogPane);

        dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        dialog.getDialogPane().setEffect(new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.35), 10.0, 0.0, 0.0, 5));

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText(translator.get(I18nKeys.STATISTICS_CLOSE_BUTTON));
        dialog.setOnCloseRequest(e -> dialogVisible.set(false));
        dialog.initStyle(StageStyle.TRANSPARENT);

        Region spacer = new Region();
        ButtonBar.setButtonData(spacer, ButtonBar.ButtonData.BIG_GAP);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        dialogPane.applyCss();
        HBox hbox = (HBox) dialogPane.lookup(".container");
        hbox.getChildren().add(spacer);

        dialog.setOnShowing(e -> {
            dialog.setX(stage.getX() + (stage.getWidth() - dialog.getDialogPane().getLayoutBounds().getWidth()) * 0.425);
            dialog.setY(stage.getY() + (stage.getHeight() - dialog.getDialogPane().getLayoutBounds().getHeight()) * 0.425);
        });

        dialog.showAndWait();
    }

    private Rectangle createTimeInRangeRectangle(final double heightFactor, final double columnSize, final Color color) {
        Rectangle rect = new Rectangle(50, heightFactor * 100 < 1 ? 1 : heightFactor * columnSize, color);
        rect.setStroke(Constants.DARK_BACKGROUND);
        rect.setStrokeWidth(0.1);
        return rect;
    }

    private Label createTimeInRangeLabel(final String text, final double size, final Paint color, final boolean bold, final Pos alignment) {
        Label label = new Label(text);
        label.setMinWidth(50);
        label.setFont(bold ? Fonts.sfProTextBold(size) : Fonts.sfProTextRegular(size));
        label.setTextFill(color);
        label.setAlignment(alignment);
        return label;
    }


    // ******************** Pattern Chart *************************************
    private void showPatternChart() {
        if (dialogVisible.get()) { return; }
        dialogVisible.set(true);

        Label titleLabel = createLabel(translator.get(I18nKeys.PATTERN_TITLE), 24, true, false, Pos.CENTER);
        Label hbac1Label = createLabel(String.format(Locale.US, "HbAc1 %.1f%% " + translator.get(I18nKeys.HBAC1_RANGE), Helper.getHbA1c(allEntries, currentUnit)), 20, false, false, Pos.CENTER);

        long limit = Instant.now().getEpochSecond() - TimeInterval.LAST_168_HOURS.getSeconds();
        List<GlucoEntry> entriesLastWeek = allEntries.stream().filter(entry -> entry.datelong() > limit).collect(Collectors.toList());

        Map<LocalTime, DataPoint> dataMap = Statistics.analyze(entriesLastWeek);
        Pair<List<String>, List<String>> highAndLowZones = Statistics.findTimesWithLowAndHighValues(dataMap, 70, 180);
        List<String> lowZones  = highAndLowZones.getA();
        List<String> highZones = highAndLowZones.getB();

        ListView<String> zones = new ListView<>();
        zones.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        zones.setCellFactory(zoneListView -> new ZoneCell());
        zones.setPrefHeight(200);
        zones.getItems().addAll(lowZones);
        zones.getItems().addAll(highZones);


        Canvas          canvas = new Canvas(640, 300);
        GraphicsContext ctx    = canvas.getGraphicsContext2D();

        double width           = canvas.getWidth();
        double height          = canvas.getHeight();
        double availableWidth  = (width - GRAPH_INSETS.getLeft() - GRAPH_INSETS.getRight());
        double availableHeight = (height - GRAPH_INSETS.getTop() - GRAPH_INSETS.getBottom());

        ctx.clearRect(0, 0, width, height);
        ctx.setFont(smallTicklabelFont);
        ctx.setFill(Constants.BRIGHT_TEXT);
        ctx.setStroke(Color.rgb(81, 80, 78));
        ctx.setLineDashes(3, 4);
        ctx.setLineWidth(1);

        List<String> yAxisLabels = MILLIGRAM_PER_DECILITER == currentUnit ? Constants.yAxisLabelsMgPerDeciliter : Constants.yAxisLabelsMmolPerLiter;

        // Draw vertical grid lines
        double stepX = availableWidth / 24;
        double stepY = availableHeight / (Constants.DEFAULT_GLUCO_RANGE);

        // Draw vertical grid lines
        ctx.setFill(Constants.BRIGHT_TEXT);
        ctx.setTextAlign(TextAlignment.CENTER);
        for (int i = 0 ; i <= 23 ; i++) {
            double x = GRAPH_INSETS.getLeft() + i * stepX;
            ctx.strokeLine(x, GRAPH_INSETS.getTop(), x, height - GRAPH_INSETS.getBottom());
            ctx.fillText(i + ":00", x, height - GRAPH_INSETS.getBottom() * 0.25);
        }

        // Draw horizontal grid lines
        ctx.setTextAlign(TextAlignment.RIGHT);
        double yLabelStep = availableHeight / yAxisLabels.size();
        for (int i = 0 ; i < yAxisLabels.size() ; i++) {
            double y = height - GRAPH_INSETS.getBottom() - i * yLabelStep - yLabelStep;
            ctx.strokeLine(GRAPH_INSETS.getLeft(), y, width - GRAPH_INSETS.getRight(), y);
            ctx.fillText(yAxisLabels.get(i), GRAPH_INSETS.getLeft() * 2.5, y + 4);
        }

        double chartStepX = availableWidth / 1440;
        ctx.setLineDashes();
        ctx.setStroke(Color.rgb(255, 255, 255, 0.5));
        ctx.setFill(Color.rgb(255, 255, 255, 0.1));
        Pair<List<Point>, List<Point>> pair = Helper.createValueRangePath(dataMap, StatisticRange.TEN_TO_NINETY, true);
        List<Point> maxPoints = pair.getA();
        List<Point> minPoints = pair.getB();

        // Envelope curve upper part
        ctx.moveTo(GRAPH_INSETS.getLeft() + maxPoints.get(0).getX() * chartStepX, (height - GRAPH_INSETS.getBottom()) - maxPoints.get(0).getY() * stepY);
        maxPoints.forEach(p -> {
            double x = GRAPH_INSETS.getLeft() + p.getX() * chartStepX;
            double y = (height - GRAPH_INSETS.getBottom()) - p.getY() * stepY;
            ctx.lineTo(x, y);
        });
        ctx.lineTo(GRAPH_INSETS.getLeft() + minPoints.get(0).getX() * chartStepX, (height - GRAPH_INSETS.getBottom()) - minPoints.get(0).getY() * stepY);
        // Envelope curve lower part
        minPoints.forEach(p -> {
            double x = GRAPH_INSETS.getLeft() + p.getX() * chartStepX;
            double y = (height - GRAPH_INSETS.getBottom()) - p.getY() * stepY;
            ctx.lineTo(x, y);
        });
        ctx.lineTo(GRAPH_INSETS.getLeft() + maxPoints.get(0).getX() * chartStepX, (height - GRAPH_INSETS.getBottom()) - maxPoints.get(0).getY() * stepY);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // Draw line chart
        List<Point> avgPoints = Helper.createAveragePath(dataMap, StatisticCalculation.MEDIAN, true);
        ctx.setStroke(new LinearGradient(0, GRAPH_INSETS.getTop(), 0, height - GRAPH_INSETS.getBottom(), false, CycleMethod.NO_CYCLE,
                                         new Stop(0.0, Constants.RED),
                                         new Stop(Constants.DEFAULT_MAX_CRITICAL_FACTOR, Constants.RED),
                                         new Stop(maxAcceptableFactor, Constants.ORANGE),
                                         new Stop(maxNormalFactor, Constants.GREEN),
                                         new Stop(minNormalFactor, Constants.GREEN),
                                         new Stop(minAcceptableFactor, Constants.ORANGE),
                                         new Stop(Constants.DEFAULT_MIN_CRITICAL_FACTOR, Constants.RED),
                                         new Stop(1.0, Constants.RED)));
        ctx.setLineWidth(2);
        ctx.beginPath();
        ctx.moveTo(GRAPH_INSETS.getLeft() + avgPoints.get(0).getX() * chartStepX, (height - GRAPH_INSETS.getBottom()) - avgPoints.get(0).getY() * stepY);
        avgPoints.forEach(p -> {
            double x = GRAPH_INSETS.getLeft() + p.getX() * chartStepX;
            double y = (height - GRAPH_INSETS.getBottom()) - p.getY() * stepY;
            ctx.lineTo(x, y);
        });
        ctx.stroke();


        VBox content = new VBox(20, titleLabel, hbac1Label, zones, canvas);
        content.setAlignment(Pos.CENTER);
        content.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));

        Dialog dialog = new Dialog();
        dialog.setTitle("");
        dialog.setHeaderText("");

        DialogPane dialogPane = new DialogPane() {
            @Override protected Node createButtonBar() {
                ButtonBar buttonBar = (ButtonBar) super.createButtonBar();
                buttonBar.getStyleClass().add("dialog-button-bar");
                buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
                return buttonBar;
            }
        };
        dialogPane.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        dialogPane.setBackground(new Background(new BackgroundFill(Constants.DARK_BACKGROUND, new CornerRadii(10), Insets.EMPTY)));
        dialogPane.setBorder(new Border(new BorderStroke(Color.rgb(78, 77, 76), BorderStrokeStyle.SOLID, new CornerRadii(10),new BorderWidths(1))));
        dialogPane.setContent(content);
        dialog.setDialogPane(dialogPane);

        dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        dialog.getDialogPane().setEffect(new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.35), 10.0, 0.0, 0.0, 5));

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText(translator.get(I18nKeys.PATTERN_CLOSE_BUTTON));
        dialog.setOnCloseRequest(e -> dialogVisible.set(false));
        dialog.initStyle(StageStyle.TRANSPARENT);

        Region spacer = new Region();
        ButtonBar.setButtonData(spacer, ButtonBar.ButtonData.BIG_GAP);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        dialogPane.applyCss();
        HBox hbox = (HBox) dialogPane.lookup(".container");
        hbox.getChildren().add(spacer);

        dialog.setOnShowing(e -> {
            dialog.setX(stage.getX() + (stage.getWidth() - dialog.getDialogPane().getLayoutBounds().getWidth()) * 0.5);
            dialog.setY(stage.getY() + (stage.getHeight() - dialog.getDialogPane().getLayoutBounds().getHeight()) * 0.1);
        });

        dialog.showAndWait();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
