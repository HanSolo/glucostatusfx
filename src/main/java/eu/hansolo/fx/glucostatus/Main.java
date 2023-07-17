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

import com.gluonhq.attach.lifecycle.LifecycleEvent;
import com.gluonhq.attach.lifecycle.LifecycleService;
import com.gluonhq.attach.runtimeargs.RuntimeArgsService;
import com.gluonhq.attach.util.Services;
import com.gluonhq.charm.glisten.control.Alert;
import eu.hansolo.applefx.MacosButton;
import eu.hansolo.applefx.MacosLabel;
import eu.hansolo.applefx.MacosScrollPane;
import eu.hansolo.applefx.MacosSeparator;
import eu.hansolo.applefx.MacosSlider;
import eu.hansolo.applefx.MacosSwitch;
import eu.hansolo.applefx.MacosSwitchBuilder;
import eu.hansolo.applefx.MacosTextField;
import eu.hansolo.applefx.MacosToggleButton;
import eu.hansolo.applefx.MacosToggleButtonBar;
import eu.hansolo.applefx.MacosToggleButtonBarSeparator;
import eu.hansolo.applefx.tools.MacosAccentColor;
import eu.hansolo.applefx.tools.MacosSystemColor;
import eu.hansolo.fx.glucostatus.Statistics.StatisticCalculation;
import eu.hansolo.fx.glucostatus.Statistics.StatisticRange;
import eu.hansolo.fx.glucostatus.notification.Message;
import eu.hansolo.fx.glucostatus.notification.Message.Notifier;
import eu.hansolo.toolbox.tuples.Pair;
import eu.hansolo.toolbox.unit.UnitDefinition;
import eu.hansolo.toolboxfx.HelperFX;
import eu.hansolo.toolboxfx.geom.Dimension;
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
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIGRAM_PER_DECILITER;
import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIMOL_PER_LITER;


public class Main extends Application {
    private static final Dimension                  IPHONE_SCREEN  = new Dimension(390, 800);//844);
    private static final Insets                     GRAPH_INSETS   = new Insets(5, 10, 5, 10);
    private static       boolean                    switchingUnits = false;
    private        final Image                      icon           = new Image(Main.class.getResourceAsStream("icon48x48.png"));
    private              ZonedDateTime              lastUpdate     = ZonedDateTime.now().minusMinutes(6);
    private              ZonedDateTime              lastFullUpdate = ZonedDateTime.now().minusMinutes(6);
    private              boolean                    darkMode       = true;
    private              Color                      accentColor    = MacosAccentColor.BLUE.getColorDark();
    private              String                     nightscoutUrl;
    private              ZonedDateTime              lastNotification;
    private              Message.Notifier           notifier;
    private              Region                     glassOverlay;
    private              HBox                       buttonHBox;
    private              AnchorPane                 mainPane;
    private              SVGPath                    reloadButton;
    private              SVGPath                    timeInRangeChartButton;
    private              SVGPath                    patternChartButton;
    private              SVGPath                    matrixButton;
    private              SVGPath                    stackedButton;
    private              SVGPath                    exclamationMark;
    private              MacosLabel                 titleLabel;
    private              MacosLabel                 valueLabel;
    private              TextFlow                   last5DeltasLabel;
    private              MacosLabel                 hba1cLabel;
    private              MacosLabel                 timestampLabel;
    private              MacosLabel                 rangeAverageLabel;
    private              Text                       unit;
    private              Text                       delta0;
    private              Text                       delta1;
    private              Text                       delta2;
    private              Text                       delta3;
    private              Text                       delta4;
    private              AnchorPane                 vpane;
    private              StackPane                  pane;
    private              StackPane                  prefPane;
    private              MacosTextField             nightscoutUrlTextField;
    private              MacosSwitch                unitSwitch;
    private              MacosSwitch                deltaChartSwitch;
    private              MacosSwitch                enableLowSoundSwitch;
    private              MacosSwitch                enableAcceptableLowSoundSwitch;
    private              MacosSwitch                enableAcceptableHighSoundSwitch;
    private              MacosSwitch                enableHighSoundSwitch;
    private              MacosSlider                tooLowIntervalSlider;
    private              MacosSlider                tooHighIntervalSlider;
    private              MacosSlider                minAcceptableSlider;
    private              MacosSlider                minNormalSlider;
    private              MacosSlider                maxNormalSlider;
    private              MacosSlider                maxAcceptableSlider;
    private              Canvas                     canvas;
    private              GraphicsContext            ctx;
    private              AnchorPane                 chartPane;
    private              PoincarePlot               poincarePlot;
    private              boolean                    deltaChartVisible;
    private              ToggleGroup                intervalToggleGroup;
    private              MacosToggleButton          sevenDays;
    private              MacosToggleButton          seventyTwoHours;
    private              MacosToggleButton          fourtyEightHours;
    private              MacosToggleButton          twentyFourHours;
    private              MacosToggleButton          twelveHours;
    private              MacosToggleButton          sixHours;
    private              MacosToggleButton          threeHours;
    private              MacosToggleButtonBar       toggleButtonBar;
    private              AnchorPane                 prefContentPane;
    private              SVGPath                    settingsIcon;
    private              MacosButton                prefButton;
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
    private Color    currentColor;
    private Interval currentInterval;
    private Font     ticklabelFont;
    private              Font                       smallTicklabelFont;
    private              boolean                    slowlyRising;
    private              boolean                    slowlyFalling;
    private              EventHandler<MouseEvent>   eventConsumer;
    // Time in range chart
    private              StackPane                  timeInRangePane;
    private              MacosLabel                 timeInRangeTitleLabel;
    private              MacosLabel                 timeInRangeTimeIntervalLabel ;
    private              Rectangle                  timeInRangeTooHighRect;
    private              Rectangle                  timeInRangeHighRect;
    private              Rectangle                  timeInRangeNormalRect;
    private              Rectangle                  timeInRangeLowRect;
    private              Rectangle                  timeInRangeTooLowRect;
    private              MacosLabel                 timeInRangeTooHighValue;
    private              MacosLabel                 timeInRangeTooHighValueText;
    private              MacosLabel                 timeInRangeHighValue;
    private              MacosLabel                 timeInRangeHighValueText;
    private              MacosLabel                 timeInRangeNormalValue;
    private              MacosLabel                 timeInRangeNormalValueText;
    private              MacosLabel                 timeInRangeLowValue;
    private              MacosLabel                 timeInRangeLowValueText;
    private              MacosLabel                 timeInRangeTooLowValue;
    private              MacosLabel                 timeInRangeTooLowValueText;
    // Pattern Chart Pane
    private              StackPane                  patternChartPane;
    private              MacosLabel                 patternChartTitleLabel;
    private              MacosLabel                 patternChartHba1cLabel;
    private              ListView<String>           patternChartZones;
    private              Canvas                     patternChartCanvas;
    // Matrix Chart Pane
    private              StackPane                  matrixChartPane;
    private              MacosLabel                 matrixChartTitleLabel;
    private              MacosLabel                 matrixChartSubTitleLabel;
    private              MacosLabel                 matrixChartHba1cLabel;
    private              ThirtyDayView              matrixChartThirtyDayView;
    // Stacked Chart Pane
    private              StackPane                  stackedChartPane;
    private              MacosLabel                 stackedChartTitleLabel;
    private              MacosLabel                 stackedChartSubTitleLabel;
    private              MacosLabel                 stackedChartHba1cLabel;
    private              StackedLineChart           stackedLineChart;


    // ******************** Initialization ************************************
    @Override public void init() {
        nightscoutUrl     = PropertyManager.INSTANCE.getString(Constants.PROPERTIES_NIGHTSCOUT_URL);
        currentUnit       = MILLIGRAM_PER_DECILITER;
        outdated          = false;
        currentInterval   = Interval.LAST_24_HOURS;
        allEntries        = FXCollections.observableArrayList();
        entries           = new ArrayList<>();
        deltas            = new ArrayList<>();
        dialogVisible     = new SimpleBooleanProperty(false);
        deltaChartVisible = false;
        lastNotification  = ZonedDateTime.now();
        slowlyRising      = false;
        slowlyFalling     = false;
        currentEntry      = new GlucoEntry("-1", 0, OffsetDateTime.now().toEpochSecond(), OffsetDateTime.now(), "", Trend.NONE, "", "", "", 2, 0, 0, 0, 0, 0, "");
        updateSettings();

        ticklabelFont      = Fonts.configRoundedRegular(10);
        smallTicklabelFont = Fonts.configRoundedRegular(8);

        eventConsumer = evt -> {
            ToggleButton src = (ToggleButton) evt.getSource();
            if (src.isSelected()) { evt.consume(); }
        };

        intervalToggleGroup = new ToggleGroup();
        sevenDays        = createToggleButton("7 d", intervalToggleGroup, eventConsumer, darkMode);
        seventyTwoHours  = createToggleButton("72 h", intervalToggleGroup, eventConsumer, darkMode);
        fourtyEightHours = createToggleButton("48 h", intervalToggleGroup, eventConsumer, darkMode);
        twentyFourHours  = createToggleButton("24 h", intervalToggleGroup, eventConsumer, darkMode);
        twelveHours      = createToggleButton("12 h", intervalToggleGroup, eventConsumer, darkMode);
        sixHours         = createToggleButton("6 h", intervalToggleGroup, eventConsumer, darkMode);
        threeHours       = createToggleButton("3 h", intervalToggleGroup, eventConsumer, darkMode);
        twentyFourHours.setSelected(true);

        settingsIcon = new SVGPath();
        settingsIcon.setContent("M8.005,14.887c0.084,-0 0.168,-0.005 0.252,-0.014c0.084,-0.009 0.172,-0.013 0.262,-0.013l0.415,0.794c0.042,0.084 0.104,0.146 0.185,0.185c0.081,0.039 0.17,0.052 0.266,0.04c0.205,-0.036 0.322,-0.159 0.352,-0.37l0.127,-0.884c0.162,-0.048 0.324,-0.102 0.487,-0.162c0.162,-0.061 0.325,-0.124 0.487,-0.19l0.65,0.596c0.15,0.144 0.322,0.162 0.514,0.054c0.169,-0.102 0.235,-0.259 0.199,-0.469l-0.19,-0.876c0.139,-0.096 0.276,-0.198 0.411,-0.307c0.135,-0.108 0.263,-0.222 0.383,-0.343l0.822,0.334c0.198,0.079 0.367,0.036 0.505,-0.126c0.054,-0.066 0.086,-0.146 0.095,-0.239c0.009,-0.093 -0.014,-0.179 -0.068,-0.257l-0.469,-0.758c0.096,-0.139 0.185,-0.285 0.266,-0.438c0.081,-0.153 0.161,-0.308 0.239,-0.465l0.894,0.045c0.096,0 0.183,-0.025 0.261,-0.076c0.078,-0.052 0.133,-0.122 0.163,-0.212c0.036,-0.091 0.039,-0.179 0.009,-0.267c-0.03,-0.087 -0.082,-0.161 -0.154,-0.221l-0.704,-0.55c0.043,-0.163 0.08,-0.33 0.113,-0.501c0.033,-0.172 0.059,-0.345 0.077,-0.519l0.839,-0.271c0.205,-0.072 0.307,-0.207 0.307,-0.406c-0,-0.204 -0.102,-0.343 -0.307,-0.415l-0.839,-0.262c-0.018,-0.18 -0.044,-0.355 -0.077,-0.523c-0.033,-0.168 -0.07,-0.337 -0.113,-0.505l0.704,-0.551c0.072,-0.06 0.122,-0.132 0.149,-0.216c0.027,-0.085 0.026,-0.172 -0.004,-0.262c-0.03,-0.09 -0.085,-0.161 -0.163,-0.212c-0.078,-0.051 -0.165,-0.074 -0.261,-0.068l-0.894,0.036c-0.078,-0.162 -0.158,-0.319 -0.239,-0.469c-0.081,-0.15 -0.17,-0.295 -0.266,-0.433l0.469,-0.758c0.054,-0.078 0.077,-0.163 0.068,-0.253c-0.009,-0.09 -0.041,-0.168 -0.095,-0.235c-0.138,-0.168 -0.307,-0.213 -0.505,-0.135l-0.822,0.325c-0.12,-0.114 -0.248,-0.227 -0.383,-0.338c-0.135,-0.112 -0.272,-0.216 -0.411,-0.312l0.19,-0.866c0.036,-0.223 -0.03,-0.379 -0.199,-0.469c-0.192,-0.109 -0.364,-0.088 -0.514,0.063l-0.65,0.577c-0.162,-0.066 -0.325,-0.128 -0.487,-0.185c-0.163,-0.057 -0.325,-0.112 -0.487,-0.167l-0.127,-0.875c-0.03,-0.205 -0.147,-0.328 -0.352,-0.37c-0.096,-0.012 -0.185,0.002 -0.266,0.041c-0.081,0.039 -0.143,0.097 -0.185,0.176l-0.415,0.803c-0.09,-0.006 -0.178,-0.011 -0.262,-0.014c-0.084,-0.003 -0.168,-0.004 -0.252,-0.004c-0.097,-0 -0.185,0.001 -0.267,0.004c-0.081,0.003 -0.167,0.008 -0.257,0.014l-0.424,-0.803c-0.09,-0.175 -0.241,-0.247 -0.451,-0.217c-0.205,0.042 -0.319,0.165 -0.343,0.37l-0.127,0.875c-0.168,0.055 -0.333,0.109 -0.496,0.163c-0.162,0.054 -0.322,0.117 -0.478,0.189l-0.659,-0.577c-0.144,-0.151 -0.316,-0.172 -0.514,-0.063c-0.169,0.09 -0.232,0.246 -0.19,0.469l0.181,0.866c-0.139,0.096 -0.276,0.2 -0.411,0.312c-0.135,0.111 -0.263,0.224 -0.383,0.338l-0.813,-0.325c-0.198,-0.078 -0.367,-0.033 -0.505,0.135c-0.06,0.067 -0.093,0.145 -0.099,0.235c-0.006,0.09 0.015,0.172 0.063,0.244l0.469,0.767c-0.096,0.138 -0.185,0.283 -0.266,0.433c-0.081,0.15 -0.161,0.307 -0.239,0.469l-0.894,-0.036c-0.096,-0.006 -0.183,0.017 -0.261,0.068c-0.078,0.051 -0.133,0.122 -0.163,0.212c-0.03,0.09 -0.031,0.177 -0.004,0.262c0.027,0.084 0.079,0.156 0.158,0.216l0.695,0.551c-0.043,0.168 -0.08,0.337 -0.113,0.505c-0.033,0.168 -0.056,0.343 -0.068,0.523l-0.848,0.262c-0.199,0.072 -0.298,0.211 -0.298,0.415c0,0.205 0.099,0.34 0.298,0.406l0.848,0.271c0.012,0.174 0.035,0.347 0.068,0.519c0.033,0.171 0.07,0.338 0.113,0.501l-0.695,0.55c-0.079,0.06 -0.131,0.134 -0.158,0.221c-0.027,0.088 -0.026,0.176 0.004,0.267c0.03,0.09 0.085,0.16 0.163,0.212c0.078,0.051 0.165,0.076 0.261,0.076l0.894,-0.045c0.078,0.157 0.158,0.312 0.239,0.465c0.081,0.153 0.17,0.299 0.266,0.438l-0.469,0.758c-0.048,0.078 -0.069,0.164 -0.063,0.257c0.006,0.093 0.039,0.173 0.099,0.239c0.138,0.162 0.307,0.205 0.505,0.126l0.813,-0.334c0.12,0.121 0.248,0.235 0.383,0.343c0.135,0.109 0.272,0.211 0.411,0.307l-0.181,0.876c-0.042,0.21 0.021,0.367 0.19,0.469c0.192,0.108 0.364,0.09 0.514,-0.054l0.659,-0.596c0.156,0.066 0.316,0.129 0.478,0.19c0.163,0.06 0.328,0.114 0.496,0.162l0.127,0.884c0.024,0.211 0.138,0.334 0.343,0.37c0.096,0.012 0.185,-0.001 0.266,-0.04c0.081,-0.039 0.143,-0.101 0.185,-0.185l0.424,-0.794c0.09,-0 0.176,0.004 0.257,0.013c0.082,0.009 0.17,0.014 0.267,0.014Zm-0,-1.228c-0.795,0 -1.53,-0.145 -2.207,-0.437c-0.677,-0.292 -1.269,-0.697 -1.778,-1.214c-0.508,-0.517 -0.904,-1.118 -1.186,-1.8c-0.283,-0.683 -0.425,-1.416 -0.425,-2.198c0,-0.788 0.142,-1.525 0.425,-2.211c0.282,-0.686 0.678,-1.287 1.186,-1.805c0.509,-0.517 1.101,-0.922 1.778,-1.213c0.677,-0.292 1.412,-0.438 2.207,-0.438c0.788,-0 1.52,0.146 2.197,0.438c0.677,0.291 1.269,0.696 1.778,1.213c0.508,0.518 0.904,1.119 1.186,1.805c0.283,0.686 0.425,1.423 0.425,2.211c-0,0.782 -0.142,1.515 -0.425,2.198c-0.282,0.682 -0.678,1.283 -1.186,1.8c-0.509,0.517 -1.101,0.922 -1.778,1.214c-0.677,0.292 -1.409,0.437 -2.197,0.437Zm-0.028,-3.718c0.41,0 0.774,-0.118 1.092,-0.356c0.319,-0.238 0.572,-0.591 0.758,-1.06l4.504,-0l-0.01,-1.029l-4.494,-0c-0.186,-0.463 -0.439,-0.812 -0.758,-1.047c-0.318,-0.235 -0.682,-0.352 -1.092,-0.352c-0.054,0 -0.117,0.005 -0.189,0.014c-0.072,0.009 -0.169,0.025 -0.289,0.049l-2.256,-3.862l-0.92,0.523l2.292,3.899c-0.193,0.216 -0.333,0.431 -0.42,0.645c-0.087,0.214 -0.131,0.435 -0.131,0.663c0,0.211 0.042,0.42 0.127,0.627c0.084,0.208 0.222,0.423 0.415,0.646l-2.374,3.862l0.903,0.542l2.355,-3.827c0.114,0.03 0.211,0.048 0.289,0.054c0.078,0.006 0.144,0.009 0.198,0.009Zm-0.785,-1.922c0,-0.228 0.082,-0.418 0.244,-0.568c0.162,-0.151 0.349,-0.226 0.559,-0.226c0.223,0 0.416,0.075 0.578,0.226c0.162,0.15 0.244,0.34 0.244,0.568c-0,0.223 -0.082,0.412 -0.244,0.569c-0.162,0.156 -0.355,0.234 -0.578,0.234c-0.21,0 -0.397,-0.078 -0.559,-0.234c-0.162,-0.157 -0.244,-0.346 -0.244,-0.569Z");
        settingsIcon.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        MacosToggleButtonBarSeparator sep1 = createSeparator(darkMode);
        MacosToggleButtonBarSeparator sep2 = createSeparator(darkMode);
        MacosToggleButtonBarSeparator sep3 = createSeparator(darkMode);
        MacosToggleButtonBarSeparator sep4 = createSeparator(darkMode);
        MacosToggleButtonBarSeparator sep5 = createSeparator(darkMode);
        MacosToggleButtonBarSeparator sep6 = createSeparator(darkMode);

        toggleButtonBar = new MacosToggleButtonBar(sevenDays, sep1, seventyTwoHours, sep2, fourtyEightHours, sep3, twentyFourHours, sep4, twelveHours, sep5, sixHours, sep6, threeHours);
        toggleButtonBar.setDark(darkMode);
        HBox.setHgrow(toggleButtonBar, Priority.ALWAYS);

        prefButton = new MacosButton("");
        prefButton.setDark(darkMode);
        prefButton.setGraphic(settingsIcon);
        prefButton.setMinWidth(32);
        prefButton.setAlignment(Pos.CENTER);
        prefButton.setContentDisplay(ContentDisplay.CENTER);
        HBox.setHgrow(prefButton, Priority.NEVER);
        HBox.setMargin(prefButton, new Insets(0, 0, 0, 5));

        buttonHBox = new HBox(10, toggleButtonBar, prefButton);
        buttonHBox.setPadding(new Insets(0, 10, 0, 10));
        buttonHBox.setAlignment(Pos.CENTER);

        currentColor = null == currentEntry ? Constants.GRAY : Helper.getColorForValue(currentUnit, UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? currentEntry.sgv() : Helper.mgPerDeciliterToMmolPerLiter(currentEntry.sgv()));

        titleLabel = createLabel("Gluco Status FX", 20, false, false, Pos.CENTER);
        titleLabel.setTextFill(Constants.BRIGHT_TEXT);
        AnchorPane.setTopAnchor(titleLabel, 5d);
        AnchorPane.setRightAnchor(titleLabel, 0d);
        AnchorPane.setLeftAnchor(titleLabel, 0d);

        reloadButton = new SVGPath();
        reloadButton.setContent("M10.993,22c1.508,0 2.924,-0.286 4.25,-0.859c1.326,-0.572 2.495,-1.367 3.508,-2.385c1.013,-1.018 1.807,-2.19 2.384,-3.517c0.577,-1.327 0.865,-2.74 0.865,-4.239c0,-1.499 -0.288,-2.912 -0.865,-4.239c-0.577,-1.327 -1.374,-2.499 -2.391,-3.517c-1.017,-1.018 -2.188,-1.813 -3.514,-2.385c-1.326,-0.573 -2.743,-0.859 -4.25,-0.859c-1.499,0 -2.911,0.286 -4.237,0.859c-1.326,0.572 -2.493,1.367 -3.501,2.385c-1.008,1.018 -1.8,2.19 -2.377,3.517c-0.577,1.327 -0.865,2.74 -0.865,4.239c-0,1.499 0.288,2.912 0.865,4.239c0.577,1.327 1.371,2.499 2.384,3.517c1.013,1.018 2.182,1.813 3.508,2.385c1.326,0.573 2.738,0.859 4.236,0.859Zm-4.999,-10.496c-0,-0.917 0.222,-1.744 0.667,-2.48c0.445,-0.737 1.029,-1.323 1.751,-1.759c0.722,-0.436 1.496,-0.654 2.322,-0.654l0.123,-0l-0.559,-0.586c-0.063,-0.064 -0.115,-0.146 -0.156,-0.246c-0.041,-0.1 -0.061,-0.204 -0.061,-0.313c-0,-0.218 0.074,-0.404 0.224,-0.559c0.15,-0.154 0.339,-0.232 0.566,-0.232c0.099,0 0.199,0.021 0.299,0.062c0.1,0.041 0.182,0.097 0.245,0.17l2.085,2.113c0.136,0.145 0.209,0.334 0.218,0.566c0.009,0.231 -0.064,0.42 -0.218,0.565l-2.098,2.072c-0.155,0.164 -0.332,0.245 -0.531,0.245c-0.227,0 -0.416,-0.077 -0.566,-0.231c-0.15,-0.155 -0.224,-0.341 -0.224,-0.559c-0,-0.218 0.072,-0.4 0.217,-0.545l0.872,-0.859c-0.045,-0.009 -0.104,-0.014 -0.177,-0.014c-0.608,0 -1.16,0.148 -1.655,0.443c-0.495,0.296 -0.89,0.693 -1.185,1.193c-0.295,0.5 -0.443,1.054 -0.443,1.663c0,0.609 0.148,1.161 0.443,1.656c0.295,0.495 0.69,0.891 1.185,1.186c0.495,0.295 1.047,0.443 1.655,0.443c0.609,-0 1.16,-0.148 1.655,-0.443c0.495,-0.295 0.888,-0.691 1.179,-1.186c0.29,-0.495 0.436,-1.047 0.436,-1.656c-0,-0.236 0.086,-0.439 0.258,-0.607c0.173,-0.168 0.377,-0.252 0.613,-0.252c0.227,0 0.427,0.084 0.6,0.252c0.172,0.168 0.259,0.371 0.259,0.607c-0,0.918 -0.225,1.756 -0.675,2.515c-0.449,0.759 -1.053,1.365 -1.812,1.819c-0.758,0.455 -1.596,0.682 -2.513,0.682c-0.926,-0 -1.766,-0.227 -2.52,-0.682c-0.754,-0.454 -1.355,-1.065 -1.805,-1.833c-0.449,-0.768 -0.674,-1.62 -0.674,-2.556Z");
        reloadButton.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        AnchorPane.setTopAnchor(reloadButton, 10d);
        AnchorPane.setRightAnchor(reloadButton, 10d);

        valueLabel = createLabel("-", 92, true, true, Pos.CENTER);
        valueLabel.setTextFill(Constants.BRIGHT_TEXT);
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

        hba1cLabel = createLabel("-", 16, false, true, Pos.CENTER);
        hba1cLabel.setTextFill(Constants.BRIGHT_TEXT);
        AnchorPane.setRightAnchor(hba1cLabel, 0d);
        AnchorPane.setBottomAnchor(hba1cLabel, 90d);
        AnchorPane.setLeftAnchor(hba1cLabel, 0d);

        timestampLabel = createLabel("-", 16, false, true, Pos.CENTER);
        timestampLabel.setTextFill(Constants.BRIGHT_TEXT);
        AnchorPane.setRightAnchor(timestampLabel, 0d);
        AnchorPane.setBottomAnchor(timestampLabel, 62d);
        AnchorPane.setLeftAnchor(timestampLabel, 0d);

        rangeAverageLabel = createLabel("-", 24, true, true, Pos.CENTER);
        rangeAverageLabel.setAlignment(Pos.CENTER);
        rangeAverageLabel.setTextFill(Constants.BRIGHT_TEXT);
        AnchorPane.setRightAnchor(rangeAverageLabel, 0d);
        AnchorPane.setBottomAnchor(rangeAverageLabel, 20d);
        AnchorPane.setLeftAnchor(rangeAverageLabel, 0d);

        patternChartButton = new SVGPath();
        patternChartButton.setContent("M10.993,22c1.508,0 2.924,-0.286 4.25,-0.859c1.326,-0.572 2.495,-1.367 3.508,-2.385c1.013,-1.018 1.807,-2.19 2.384,-3.517c0.577,-1.327 0.865,-2.74 0.865,-4.239c0,-1.499 -0.288,-2.912 -0.865,-4.239c-0.577,-1.327 -1.374,-2.499 -2.391,-3.517c-1.017,-1.018 -2.188,-1.813 -3.514,-2.385c-1.326,-0.573 -2.743,-0.859 -4.25,-0.859c-1.499,0 -2.911,0.286 -4.237,0.859c-1.326,0.572 -2.493,1.367 -3.501,2.385c-1.008,1.018 -1.8,2.19 -2.377,3.517c-0.577,1.327 -0.865,2.74 -0.865,4.239c-0,1.499 0.288,2.912 0.865,4.239c0.577,1.327 1.371,2.499 2.384,3.517c1.013,1.018 2.182,1.813 3.508,2.385c1.326,0.573 2.738,0.859 4.236,0.859Zm-6.062,-6.911l0,-8.792c0,-0.19 0.068,-0.354 0.205,-0.49c0.136,-0.137 0.295,-0.205 0.476,-0.205c0.191,0 0.355,0.068 0.491,0.205c0.136,0.136 0.204,0.3 0.204,0.49l0,4.158l1.321,-1.363c0.273,-0.273 0.573,-0.409 0.9,-0.409c0.336,-0 0.635,0.136 0.899,0.409l1.866,1.949c0.036,0.045 0.073,0.041 0.109,-0.014l1.839,-1.854l-0.749,-0.736c-0.155,-0.145 -0.196,-0.311 -0.123,-0.497c0.073,-0.186 0.218,-0.311 0.436,-0.375l3.215,-0.831c0.2,-0.055 0.37,-0.014 0.511,0.122c0.14,0.137 0.184,0.3 0.129,0.491l-0.845,3.23c-0.063,0.228 -0.188,0.378 -0.374,0.45c-0.186,0.073 -0.352,0.032 -0.497,-0.122l-0.736,-0.764l-1.962,2.018c-0.272,0.281 -0.576,0.422 -0.912,0.422c-0.327,0 -0.631,-0.141 -0.913,-0.422l-1.839,-1.909c-0.036,-0.054 -0.077,-0.054 -0.123,0l-2.152,2.181l0,2.14c0,0.064 0.027,0.096 0.082,0.096l9.958,-0c0.181,-0 0.34,0.068 0.477,0.204c0.136,0.136 0.204,0.295 0.204,0.477c-0,0.191 -0.068,0.355 -0.204,0.491c-0.137,0.136 -0.296,0.204 -0.477,0.204l-10.476,0c-0.281,0 -0.508,-0.086 -0.681,-0.259c-0.172,-0.172 -0.259,-0.404 -0.259,-0.695Z");
        patternChartButton.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        patternChartButton.setOpacity(0.5);
        AnchorPane.setBottomAnchor(patternChartButton, 20d);
        AnchorPane.setLeftAnchor(patternChartButton, 10d);

        matrixButton = new SVGPath();
        matrixButton.setContent("M10.993,22c1.508,0 2.924,-0.286 4.25,-0.859c1.326,-0.572 2.495,-1.367 3.508,-2.385c1.013,-1.018 1.807,-2.19 2.384,-3.517c0.577,-1.327 0.865,-2.74 0.865,-4.239c0,-1.499 -0.288,-2.912 -0.865,-4.239c-0.577,-1.327 -1.374,-2.499 -2.391,-3.517c-1.017,-1.018 -2.188,-1.813 -3.514,-2.385c-1.326,-0.573 -2.743,-0.859 -4.25,-0.859c-1.499,0 -2.911,0.286 -4.237,0.859c-1.326,0.572 -2.493,1.367 -3.501,2.385c-1.008,1.018 -1.8,2.19 -2.377,3.517c-0.577,1.327 -0.865,2.74 -0.865,4.239c-0,1.499 0.288,2.912 0.865,4.239c0.577,1.327 1.371,2.499 2.384,3.517c1.013,1.018 2.182,1.813 3.508,2.385c1.326,0.573 2.738,0.859 4.236,0.859Zm0.014,-5.861c-0.418,-0 -0.777,-0.15 -1.076,-0.45c-0.3,-0.3 -0.45,-0.659 -0.45,-1.077c0,-0.418 0.15,-0.777 0.45,-1.077c0.299,-0.3 0.658,-0.449 1.076,-0.449c0.408,-0 0.765,0.149 1.069,0.449c0.304,0.3 0.456,0.659 0.456,1.077c0,0.418 -0.152,0.777 -0.456,1.077c-0.304,0.3 -0.661,0.45 -1.069,0.45Zm-3.855,-0c-0.418,-0 -0.777,-0.15 -1.076,-0.45c-0.3,-0.3 -0.45,-0.659 -0.45,-1.077c0,-0.418 0.15,-0.777 0.45,-1.077c0.299,-0.3 0.658,-0.449 1.076,-0.449c0.417,-0 0.774,0.149 1.069,0.449c0.295,0.3 0.443,0.659 0.443,1.077c-0,0.427 -0.148,0.788 -0.443,1.084c-0.295,0.295 -0.652,0.443 -1.069,0.443Zm7.696,-0c-0.408,-0 -0.763,-0.15 -1.062,-0.45c-0.3,-0.3 -0.45,-0.659 -0.45,-1.077c0,-0.418 0.15,-0.777 0.45,-1.077c0.299,-0.3 0.654,-0.449 1.062,-0.449c0.418,-0 0.777,0.149 1.076,0.449c0.3,0.3 0.45,0.659 0.45,1.077c-0,0.418 -0.15,0.777 -0.45,1.077c-0.299,0.3 -0.658,0.45 -1.076,0.45Zm-3.841,-3.612c-0.418,-0 -0.777,-0.15 -1.076,-0.45c-0.3,-0.3 -0.45,-0.659 -0.45,-1.077c0,-0.418 0.15,-0.775 0.45,-1.07c0.299,-0.295 0.658,-0.443 1.076,-0.443c0.408,-0.009 0.765,0.136 1.069,0.436c0.304,0.3 0.456,0.659 0.456,1.077c0,0.418 -0.152,0.777 -0.456,1.077c-0.304,0.3 -0.661,0.45 -1.069,0.45Zm-3.855,-0c-0.418,-0 -0.777,-0.15 -1.076,-0.45c-0.3,-0.3 -0.45,-0.659 -0.45,-1.077c0,-0.418 0.15,-0.775 0.45,-1.07c0.299,-0.295 0.658,-0.443 1.076,-0.443c0.417,-0.009 0.774,0.136 1.069,0.436c0.295,0.3 0.443,0.659 0.443,1.077c-0,0.418 -0.148,0.777 -0.443,1.077c-0.295,0.3 -0.652,0.45 -1.069,0.45Zm7.696,-0c-0.408,-0 -0.763,-0.15 -1.062,-0.45c-0.3,-0.3 -0.45,-0.659 -0.45,-1.077c0,-0.418 0.15,-0.775 0.45,-1.07c0.299,-0.295 0.654,-0.443 1.062,-0.443c0.418,-0.009 0.777,0.136 1.076,0.436c0.3,0.3 0.45,0.659 0.45,1.077c-0,0.418 -0.15,0.777 -0.45,1.077c-0.299,0.3 -0.658,0.45 -1.076,0.45Zm-3.841,-3.599c-0.418,0 -0.777,-0.15 -1.076,-0.45c-0.3,-0.3 -0.45,-0.658 -0.45,-1.077c0,-0.427 0.15,-0.786 0.45,-1.076c0.299,-0.291 0.658,-0.441 1.076,-0.45c0.408,-0 0.765,0.148 1.069,0.443c0.304,0.295 0.456,0.656 0.456,1.083c0,0.419 -0.152,0.777 -0.456,1.077c-0.304,0.3 -0.661,0.45 -1.069,0.45Zm-3.855,0c-0.418,0 -0.777,-0.15 -1.076,-0.45c-0.3,-0.3 -0.45,-0.658 -0.45,-1.077c0,-0.418 0.15,-0.776 0.45,-1.076c0.299,-0.3 0.658,-0.45 1.076,-0.45c0.417,-0 0.774,0.15 1.069,0.45c0.295,0.3 0.443,0.658 0.443,1.076c-0,0.419 -0.148,0.777 -0.443,1.077c-0.295,0.3 -0.652,0.45 -1.069,0.45Zm7.696,0c-0.408,0 -0.763,-0.15 -1.062,-0.45c-0.3,-0.3 -0.45,-0.658 -0.45,-1.077c0,-0.427 0.15,-0.786 0.45,-1.076c0.299,-0.291 0.654,-0.441 1.062,-0.45c0.418,-0 0.777,0.148 1.076,0.443c0.3,0.295 0.45,0.656 0.45,1.083c-0,0.419 -0.15,0.777 -0.45,1.077c-0.299,0.3 -0.658,0.45 -1.076,0.45Z");
        matrixButton.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        matrixButton.setOpacity(0.5);
        AnchorPane.setTopAnchor(matrixButton, 10d);
        AnchorPane.setLeftAnchor(matrixButton, 10d);

        stackedButton = new SVGPath();
        stackedButton.setContent("M10.993,-0C17.067,-0 21.998,4.933 21.998,11.01C21.998,17.087 17.067,22.02 10.993,22.02C4.92,22.02 -0.011,17.087 -0.011,11.01C-0.011,4.933 4.92,-0 10.993,-0ZM6.31,6.598L6.31,6.296C6.31,5.916 6.001,5.607 5.621,5.607C5.241,5.607 4.932,5.916 4.932,6.296L4.932,15.359C4.932,15.739 5.241,16.047 5.621,16.047L16.329,16.047C16.709,16.047 17.017,15.739 17.017,15.359C17.017,14.979 16.709,14.67 16.329,14.67L6.386,14.67C6.386,14.67 6.353,14.67 6.329,14.646C6.309,14.625 6.31,14.589 6.31,14.589L6.31,13.919C6.329,13.907 6.347,13.894 6.365,13.88L9.267,11.565C9.267,11.565 12.126,13.86 12.156,13.884C12.301,13.999 12.501,14.044 12.688,14.019C12.815,14.005 12.938,13.959 13.036,13.882C13.062,13.861 16.355,11.223 16.355,11.223C16.594,11.033 16.593,10.725 16.354,10.535C16.115,10.345 15.728,10.345 15.489,10.536L12.597,12.843L9.702,10.534C9.628,10.475 9.54,10.434 9.446,10.412C9.237,10.36 8.999,10.401 8.833,10.533L6.31,12.546L6.31,10.945C6.329,10.933 6.347,10.92 6.365,10.906L9.267,8.591C9.267,8.591 12.126,10.886 12.156,10.91C12.301,11.025 12.501,11.07 12.688,11.045C12.815,11.031 12.938,10.985 13.036,10.908C13.062,10.887 16.355,8.249 16.355,8.249C16.594,8.059 16.593,7.751 16.354,7.561C16.115,7.371 15.728,7.371 15.489,7.562L12.597,9.869L9.702,7.56C9.628,7.501 9.54,7.46 9.446,7.437C9.237,7.386 8.999,7.427 8.833,7.559L6.31,9.572L6.31,7.971C6.329,7.959 6.347,7.946 6.365,7.932L9.267,5.617C9.267,5.617 12.126,7.911 12.156,7.936C12.301,8.051 12.501,8.096 12.688,8.071C12.815,8.057 12.938,8.011 13.036,7.934C13.062,7.913 16.355,5.275 16.355,5.275C16.594,5.085 16.593,4.776 16.354,4.587C16.115,4.397 15.728,4.397 15.489,4.587L12.597,6.895L9.702,4.586C9.628,4.527 9.54,4.486 9.446,4.463C9.237,4.412 8.999,4.453 8.833,4.585L6.31,6.598Z");
        stackedButton.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        stackedButton.setOpacity(0.5);
        AnchorPane.setTopAnchor(stackedButton, 10d);
        AnchorPane.setLeftAnchor(stackedButton, 52d);

        timeInRangeChartButton = new SVGPath();
        timeInRangeChartButton.setContent("M10.993,22c1.508,0 2.924,-0.286 4.25,-0.859c1.326,-0.572 2.495,-1.367 3.508,-2.385c1.013,-1.018 1.807,-2.19 2.384,-3.517c0.577,-1.327 0.865,-2.74 0.865,-4.239c0,-1.499 -0.288,-2.912 -0.865,-4.239c-0.577,-1.327 -1.374,-2.499 -2.391,-3.517c-1.017,-1.018 -2.188,-1.813 -3.514,-2.385c-1.326,-0.573 -2.743,-0.859 -4.25,-0.859c-1.499,0 -2.911,0.286 -4.237,0.859c-1.326,0.572 -2.493,1.367 -3.501,2.385c-1.008,1.018 -1.8,2.19 -2.377,3.517c-0.577,1.327 -0.865,2.74 -0.865,4.239c-0,1.499 0.288,2.912 0.865,4.239c0.577,1.327 1.371,2.499 2.384,3.517c1.013,1.018 2.182,1.813 3.508,2.385c1.326,0.573 2.738,0.859 4.236,0.859Zm-5.421,-6.434c-0.282,0 -0.511,-0.081 -0.688,-0.245c-0.177,-0.164 -0.266,-0.382 -0.266,-0.654c-0,-0.255 0.089,-0.464 0.266,-0.627c0.177,-0.164 0.406,-0.246 0.688,-0.246l10.884,0c0.281,0 0.511,0.082 0.688,0.246c0.177,0.163 0.265,0.372 0.265,0.627c0,0.272 -0.088,0.49 -0.265,0.654c-0.177,0.164 -0.407,0.245 -0.688,0.245l-10.884,0Zm-0,-3.68c-0.282,-0 -0.511,-0.08 -0.688,-0.239c-0.177,-0.159 -0.266,-0.374 -0.266,-0.647c-0,-0.273 0.089,-0.488 0.266,-0.647c0.177,-0.159 0.406,-0.239 0.688,-0.239l10.884,0c0.281,0 0.511,0.08 0.688,0.239c0.177,0.159 0.265,0.374 0.265,0.647c0,0.273 -0.088,0.488 -0.265,0.647c-0.177,0.159 -0.407,0.239 -0.688,0.239l-10.884,-0Zm-0,-3.68c-0.282,-0 -0.511,-0.08 -0.688,-0.239c-0.177,-0.159 -0.266,-0.37 -0.266,-0.634c-0,-0.272 0.089,-0.49 0.266,-0.654c0.177,-0.164 0.406,-0.245 0.688,-0.245l10.884,-0c0.281,-0 0.511,0.081 0.688,0.245c0.177,0.164 0.265,0.382 0.265,0.654c0,0.264 -0.088,0.475 -0.265,0.634c-0.177,0.159 -0.407,0.239 -0.688,0.239l-10.884,-0Z");
        timeInRangeChartButton.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        AnchorPane.setRightAnchor(timeInRangeChartButton, 10d);
        AnchorPane.setBottomAnchor(timeInRangeChartButton, 20d);

        mainPane = new AnchorPane(titleLabel, matrixButton, reloadButton, valueLabel, last5DeltasLabel, hba1cLabel, timestampLabel, rangeAverageLabel, patternChartButton, timeInRangeChartButton, stackedButton);
        mainPane.setPrefSize(IPHONE_SCREEN.getWidth(), 285);
        mainPane.setMinHeight(285);
        mainPane.setBackground(new Background(new BackgroundFill(Constants.GRAY, CornerRadii.EMPTY, Insets.EMPTY)));

        canvas = new Canvas(IPHONE_SCREEN.getWidth(), 365);
        ctx    = canvas.getGraphicsContext2D();
        AnchorPane.setTopAnchor(canvas, 10d);
        AnchorPane.setRightAnchor(canvas, 5d);
        AnchorPane.setBottomAnchor(canvas, 5d);
        AnchorPane.setLeftAnchor(canvas, 5d);

        poincarePlot = new PoincarePlot();
        poincarePlot.setVisible(false);
        AnchorPane.setTopAnchor(poincarePlot, 10d);
        AnchorPane.setRightAnchor(poincarePlot, 5d);
        AnchorPane.setBottomAnchor(poincarePlot, 5d);
        AnchorPane.setLeftAnchor(poincarePlot, 5d);


        exclamationMark = new SVGPath();
        exclamationMark.setContent("M7.743,54.287l41.48,-0c1.613,-0 2.995,-0.346 4.147,-1.037c1.153,-0.692 2.046,-1.619 2.679,-2.783c0.634,-1.164 0.951,-2.483 0.951,-3.958c-0,-0.622 -0.092,-1.262 -0.277,-1.918c-0.184,-0.657 -0.449,-1.285 -0.795,-1.884l-20.774,-36.053c-0.737,-1.29 -1.705,-2.27 -2.904,-2.938c-1.198,-0.668 -2.454,-1.003 -3.767,-1.003c-1.314,0 -2.57,0.335 -3.768,1.003c-1.198,0.668 -2.155,1.648 -2.869,2.938l-20.774,36.053c-0.715,1.221 -1.072,2.489 -1.072,3.802c0,1.475 0.311,2.794 0.933,3.958c0.622,1.164 1.515,2.091 2.679,2.783c1.164,0.691 2.541,1.037 4.131,1.037Zm0.034,-4.874c-0.829,-0 -1.503,-0.294 -2.022,-0.882c-0.518,-0.587 -0.777,-1.261 -0.777,-2.022c-0,-0.207 0.023,-0.438 0.069,-0.691c0.046,-0.254 0.138,-0.496 0.276,-0.726l20.74,-36.087c0.254,-0.461 0.605,-0.807 1.054,-1.037c0.45,-0.231 0.905,-0.346 1.366,-0.346c0.484,-0 0.939,0.115 1.365,0.346c0.426,0.23 0.778,0.576 1.054,1.037l20.74,36.121c0.231,0.438 0.346,0.899 0.346,1.383c-0,0.761 -0.259,1.435 -0.778,2.022c-0.518,0.588 -1.204,0.882 -2.057,0.882l-41.376,-0Zm20.74,-4.494c0.83,0 1.562,-0.294 2.195,-0.881c0.634,-0.588 0.951,-1.308 0.951,-2.161c-0,-0.875 -0.311,-1.601 -0.933,-2.177c-0.623,-0.577 -1.36,-0.865 -2.213,-0.865c-0.875,0 -1.624,0.294 -2.247,0.882c-0.622,0.587 -0.933,1.308 -0.933,2.16c0,0.83 0.317,1.544 0.951,2.143c0.633,0.599 1.377,0.899 2.229,0.899Zm0,-9.056c1.521,-0 2.293,-0.795 2.316,-2.385l0.45,-14.173c0.023,-0.76 -0.237,-1.4 -0.778,-1.918c-0.542,-0.519 -1.216,-0.778 -2.022,-0.778c-0.83,0 -1.504,0.254 -2.022,0.761c-0.519,0.507 -0.767,1.14 -0.744,1.901l0.381,14.207c0.046,1.59 0.852,2.385 2.419,2.385Z");
        exclamationMark.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        exclamationMark.setVisible(false);
        StackPane problemPane = new StackPane(exclamationMark);
        problemPane.setMouseTransparent(true);
        AnchorPane.setTopAnchor(problemPane, 0d);
        AnchorPane.setRightAnchor(problemPane, 0d);
        AnchorPane.setBottomAnchor(problemPane, 0d);
        AnchorPane.setLeftAnchor(problemPane, 0d);

        chartPane = new AnchorPane(canvas, poincarePlot, problemPane);
        chartPane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));
        chartPane.setMinWidth(IPHONE_SCREEN.getWidth());
        chartPane.setMinHeight(100);

        glassOverlay = new Region();
        glassOverlay.setOpacity(0.0);
        glassOverlay.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.5), new CornerRadii(10), Insets.EMPTY)));
        glassOverlay.setVisible(false);
        glassOverlay.setManaged(false);

        vpane = new AnchorPane(buttonHBox, mainPane, chartPane);
        // Button Tool Bar
        AnchorPane.setTopAnchor(buttonHBox, 24d);
        AnchorPane.setRightAnchor(buttonHBox, 0d);
        AnchorPane.setLeftAnchor(buttonHBox, 0d);
        // Main Value
        AnchorPane.setTopAnchor(mainPane, 59d);
        AnchorPane.setRightAnchor(mainPane, 0d);
        AnchorPane.setLeftAnchor(mainPane, 0d);
        // ChartPane
        AnchorPane.setTopAnchor(chartPane, 345d);
        AnchorPane.setRightAnchor(chartPane, 0d);
        AnchorPane.setBottomAnchor(chartPane, 5d);
        AnchorPane.setLeftAnchor(chartPane, 0d);

        prefPane = createPrefPane();
        prefPane.setVisible(false);
        prefPane.setManaged(false);

        timeInRangePane = createTimeInRangePane();
        timeInRangePane.setVisible(false);
        timeInRangePane.setManaged(false);

        patternChartPane = createPatternChartPane();
        patternChartPane.setVisible(false);
        patternChartPane.setManaged(false);

        matrixChartPane = createMatrixChartPane();
        matrixChartPane.setVisible(false);
        matrixChartPane.setManaged(false);

        stackedChartPane = createStackedChartPane();
        stackedChartPane.setVisible(false);
        stackedChartPane.setManaged(false);

        pane = new StackPane(vpane, glassOverlay, prefPane, timeInRangePane, patternChartPane, matrixChartPane, stackedChartPane);
        //pane.setBackground(new Background(new BackgroundFill(MacosSystemColor.BACKGROUND.dark(), CornerRadii.EMPTY, Insets.EMPTY)));
        pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        StackPane.setMargin(vpane, new Insets(32, 0, 20, 0));
        StackPane.setMargin(prefPane, new Insets(32, 0, 20, 0));

        registerListeners();
    }


    // ******************** App lifecycle *************************************
    @Override public void start(final Stage stage) {
        notifier = Notifier.INSTANCE;

        Scene scene = new Scene(pane);
        scene.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
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
            Helper.getEntriesFromLast30Days(nightscoutUrl + Constants.URL_API).thenAccept(l -> {
                allEntries.addAll(l);
                Platform.runLater(() -> {
                    matrixButton.setOpacity(1.0);
                    patternChartButton.setOpacity(1.0);
                    stackedButton.setOpacity(1.0);
                });
                lastFullUpdate = ZonedDateTime.now();
            });
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

        /*
        RuntimeArgsService.create().ifPresent(service -> {
            service.addListener("ALERT", value -> {
                Alert alert = new Alert(AlertType.WARNING, value);
                Platform.runLater(() -> alert.showAndWait());
            });
        });
        */
    }

    @Override public void stop() {
        service.cancel();
        Platform.exit();
        System.exit(0);
    }


    // ******************** Methods *******************************************
    private void registerListeners() {
        chartPane.widthProperty().addListener((o, ov, nv) -> canvas.setWidth(nv.doubleValue() - 10));
        chartPane.heightProperty().addListener((o, ov, nv) -> canvas.setHeight(nv.doubleValue() - 15));

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
            vpane.setVisible(false);
            vpane.setManaged(false);
            prefPane.setManaged(true);
            prefPane.setVisible(true);
        });

        matrixButton.setOnMousePressed(e -> matrixButton.setOpacity(0.75));
        matrixButton.setOnMouseReleased(e -> {
            matrixButton.setOpacity(1.0);
            showMatrixChart();
        });

        stackedButton.setOnMousePressed(e -> stackedButton.setOpacity(0.75));
        stackedButton.setOnMouseReleased(e -> {
            stackedButton.setOpacity(1.0);
            showStackedChart();
        });

        reloadButton.setOnMousePressed(e -> reloadButton.setOpacity(0.75));
        reloadButton.setOnMouseReleased(e -> {
            reloadButton.setOpacity(1.0);
            reloadAllEntries();
        });

        timeInRangeChartButton.setOnMousePressed(e -> timeInRangeChartButton.setOpacity(0.75));
        timeInRangeChartButton.setOnMouseReleased(e -> {
            timeInRangeChartButton.setOpacity(1.0);
            showTimeInRangeChart();
        });

        patternChartButton.setOnMousePressed(e -> patternChartButton.setOpacity(0.75));
        patternChartButton.setOnMouseReleased(e -> {
            patternChartButton.setOpacity(1.0);
            showPatternChart();
        });

        intervalToggleGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (null == ov || null == nv) { return; }
            if (ov.equals(nv)) { nv.setSelected(true); }
            if (nv.equals(sevenDays)) {
                currentInterval = Interval.LAST_168_HOURS;
            } else if (nv.equals(seventyTwoHours)) {
                currentInterval = Interval.LAST_72_HOURS;
            } else if (nv.equals(fourtyEightHours)) {
                currentInterval = Interval.LAST_48_HOURS;
            } else if (nv.equals(twentyFourHours)) {
                currentInterval = Interval.LAST_24_HOURS;
            } else if (nv.equals(twelveHours)) {
                currentInterval = Interval.LAST_12_HOURS;
            } else if (nv.equals(sixHours)) {
                currentInterval = Interval.LAST_6_HOURS;
            } else if (nv.equals(threeHours)) {
                currentInterval = Interval.LAST_3_HOURS;
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

        canvas.setOnMousePressed(e -> {
            canvas.setVisible(false);
            poincarePlot.setVisible(true);
            poincarePlot.loadSettings();
        });
        poincarePlot.setOnMousePressed(e -> {
            poincarePlot.setVisible(false);
            canvas.setVisible(true);
        });

        /*
        Services.get(LifecycleService.class).ifPresent(service -> {
            service.addListener(LifecycleEvent.RESUME, () -> reloadAllEntries());
        });
        */
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

    private void reloadAllEntries() {
        if (null != nightscoutUrl && !nightscoutUrl.isEmpty() && ZonedDateTime.now().toEpochSecond() - lastFullUpdate.toEpochSecond() > Constants.SECONDS_PER_MINUTE) {
            matrixButton.setOpacity(0.5);
            patternChartButton.setOpacity(0.5);
            stackedButton.setOpacity(0.5);
            allEntries.clear();
            Helper.getEntriesFromLast30Days(nightscoutUrl + Constants.URL_API).thenAccept(l -> {
                allEntries.addAll(l);
                Platform.runLater(() -> {
                    matrixButton.setOpacity(1.0);
                    patternChartButton.setOpacity(1.0);
                    stackedButton.setOpacity(1.0);
                });
                lastFullUpdate = ZonedDateTime.now();
            });
            drawChart();
        }
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
                String title         = "Attention";
                String msg           = "Glucose too low soon !";
                Message notification = new Message(title, msg, icon);
                Platform.runLater(() -> notifier.notify(notification));
                return true;
            }
        } else if (soonTooHigh) {
            if (currentEntry.sgv() > Constants.DEFAULT_SOON_TOO_HIGH) {
                String title         = "Attention";
                String  msg          = "Glucose too high soon !";
                Message notification = new Message(title, msg, icon);
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

        String  msg       = "";

        if (value > maxCritical) {
            // TOO HIGH
            if (Trend.SINGLE_DOWN == trend || Trend.DOUBLE_DOWN == trend) {
                return; // Was critical but is falling again -> no notification
            } else if (trend == Trend.FLAT || Trend.FORTY_FIVE_DOWN == trend) {
                if (now.toEpochSecond() - lastNotification.toEpochSecond() > criticalMaxNotificationInterval) {
                    msg = "Glucose too high";
                }
            } else {
                msg = "Glucose too high";
            }
        } else if (value > maxAcceptable) {
            // HIGH
            if (showHighValueNotification) {
                // High
                if (Trend.SINGLE_UP == trend || Trend.DOUBLE_UP == trend) {
                    msg = "Glucose soon too high";
                } else {
                    msg = "Glucose high";
                }
            } else {
                msg = "";
            }
        } else if (value > maxNormal) {
            // ACCEPTABLE HIGH
            if (showAcceptableHighValueNotification) {
                if (Trend.SINGLE_UP == trend || Trend.DOUBLE_UP == trend) {
                    msg = "Glucose high soon";
                } else {
                    msg = "Glucose a bit high";
                }
            } else {
                msg = "";
            }
        } else if (value > minNormal) {
            // NORMAL
        } else if (value > minAcceptable) {
            // ACCEPTABLE LOW
            if (showAcceptableLowValueNotification) {
                if (Trend.SINGLE_DOWN == trend || Trend.DOUBLE_DOWN == trend) {
                    msg = "Glucose low soon";
                } else {
                    msg = "Glucose a bit low";
                }
            } else {
                msg = "";
            }
        } else if (value > minCritical) {
            // LOW
            if (showLowValueNotification) {
                if (Trend.SINGLE_DOWN == trend || Trend.DOUBLE_DOWN == trend) {
                    msg = "Glucose too low soon";
                } else {
                    msg = "Glucose low";
                }
            } else {
                msg = "";
            }
        } else {
            // TOO LOW
            if (Trend.SINGLE_UP == trend || Trend.DOUBLE_UP == trend) {
                return; // Was critical but is rising again -> no notification
            } else if (Trend.FLAT == trend || Trend.FORTY_FIVE_UP == trend) {
                if (now.toEpochSecond() - lastNotification.toEpochSecond() > criticalMinNotificationInterval) {
                    msg = "Glucose too low";
                }
            } else {
                msg = "Glucose too low";
            }
        }

        if (msg.isEmpty()) { return; }

        String format = MILLIGRAM_PER_DECILITER == currentUnit ? "%.0f" : "%.1f";
        String  body         = new StringBuilder().append(msg).append(" (").append(String.format(Locale.US, format, currentEntry.sgv())).append(" ").append(currentEntry.trend().getSymbol()).append(")").toString();
        Message notification = new Message("Attention", body, icon);

        notifier.notify(notification);

        lastNotification = now;
    }

    private void applySettingsToPreferences() {
        nightscoutUrlTextField.setText(PropertyManager.INSTANCE.getString(Constants.PROPERTIES_NIGHTSCOUT_URL));
        unitSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_UNIT_MG));
        deltaChartSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_DELTA_CHART));
        enableLowSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_LOW_VALUE_NOTIFICATION));
        enableAcceptableLowSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION));
        enableAcceptableHighSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION));
        enableHighSoundSwitch.setSelected(PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_SHOW_HIGH_VALUE_NOTIFICATION));
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
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_LOW_VALUE_NOTIFICATION, enableLowSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_LOW_VALUE_NOTIFICATION, enableAcceptableLowSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_ACCEPTABLE_HIGH_VALUE_NOTIFICATION, enableAcceptableHighSoundSwitch.isSelected());
        PropertyManager.INSTANCE.setBoolean(Constants.PROPERTIES_SHOW_HIGH_VALUE_NOTIFICATION, enableHighSoundSwitch.isSelected());
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
        long limit = OffsetDateTime.now().toEpochSecond() - currentInterval.getSeconds();
        entries      = allEntries.stream().filter(entry -> entry.datelong() > limit).collect(Collectors.toList());

        // Use last entry if filtered list is empty
        if (entries.isEmpty()) { entries.add(allEntries.get(0)); }

        currentEntry = entries.get(0);
        currentColor = null == currentEntry ? Constants.GRAY : Helper.getColorForValue(currentUnit, UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? currentEntry.sgv() : Helper.mgPerDeciliterToMmolPerLiter(currentEntry.sgv()));

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
            if (MILLIMOL_PER_LITER == currentUnit) {
                deltaMin = Helper.mgPerDeciliterToMmolPerLiter(deltaMin);
                deltaMax = Helper.mgPerDeciliterToMmolPerLiter(deltaMax);
            }
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
        outdated = (OffsetDateTime.now().toEpochSecond() - lastTimestamp.getEpochSecond() > Constants.TIMEOUT_IN_SECONDS);
        LocalDateTime dateTime = LocalDateTime.ofInstant(lastTimestamp, ZoneId.systemDefault());
        if (MILLIGRAM_PER_DECILITER == currentUnit) {
            avg = entries.stream().map(entry -> entry.sgv()).collect(Collectors.summingDouble(Double::doubleValue)) / entries.size();
        } else {
            avg = entries.stream().map(entry -> Helper.mgPerDeciliterToMmolPerLiter(entry.sgv())).collect(Collectors.summingDouble(Double::doubleValue)) / entries.size();
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
            hba1cLabel.setText(allEntries.size() > 29 ? String.format(Locale.US, "HbA1c %.1f%%", Helper.calcHbA1c(allEntries)) : "-");
            timestampLabel.setText(Constants.DTF.format(dateTime) + (outdated ? " 􀇾" : ""));
            exclamationMark.setVisible(outdated);
            rangeAverageLabel.setText(currentInterval.getUiString() + " (ø" + String.format(Locale.US, format, avg) + ")");

            drawChart();
        });

        predict();

        notifyIfNeeded();
    }

    private void drawChart() {
        if (entries.isEmpty()) { return; }
        Collections.sort(entries, Comparator.comparingLong(GlucoEntry::datelong));

        List<Double> values = new ArrayList<>();
        for (GlucoEntry entry : entries) {
            values.add(entry.sgv());
        }
        poincarePlot.setValues(currentUnit, values);

        double  width           = canvas.getWidth();
        double  height          = canvas.getHeight();
        double  availableWidth  = (width - GRAPH_INSETS.getLeft() - GRAPH_INSETS.getRight());
        double  availableHeight = (height - GRAPH_INSETS.getTop() - GRAPH_INSETS.getBottom());
        boolean darkMode        = true;

        ctx.clearRect(0, 0, width, height);
        ctx.setFill(darkMode ? Constants.DARK_BACKGROUND : Constants.BRIGHT_BACKGROUND);
        ctx.fillRect(0, 0, width, height);
        ctx.setFont(ticklabelFont);
        ctx.setFill(Constants.BRIGHT_TEXT);
        ctx.setStroke(darkMode ? Color.rgb(81, 80, 78) : Color.rgb(184, 183, 183));
        ctx.setLineDashes(3, 4);
        ctx.setLineWidth(1);
        List<String> yAxisLabels = MILLIGRAM_PER_DECILITER == currentUnit ? Constants.yAxisLabelsMgPerDeciliter : Constants.yAxisLabelsMmolPerLiter;

        // Draw chart
        long           chartStartEpoch = OffsetDateTime.now().minusSeconds(currentInterval.getSeconds()).toEpochSecond();
        OffsetDateTime chartStartDate  = OffsetDateTime.ofInstant(Instant.ofEpochSecond(chartStartEpoch), ZoneId.systemDefault());

        // Get min entry if within range
        GlucoEntry minEntry;
        if (entries.get(0).datelong() < chartStartEpoch) {
            GlucoEntry firstEntry = entries.get(0);
            minEntry = new GlucoEntry(firstEntry.id(), firstEntry.sgv(), chartStartEpoch, chartStartDate, firstEntry.dateString(), firstEntry.trend(), firstEntry.direction(),
                                      firstEntry.device(), firstEntry.type(), firstEntry.utcOffset(), firstEntry.noise(), firstEntry.filtered(), firstEntry.unfiltered(),
                                      firstEntry.rssi(), firstEntry.delta(), firstEntry.sysTime());
        } else if (entries.get(0).datelong() > chartStartEpoch) {
            GlucoEntry firstEntry = entries.get(0);
            minEntry = new GlucoEntry(firstEntry.id(), firstEntry.sgv(), chartStartEpoch, chartStartDate, firstEntry.dateString(), firstEntry.trend(), firstEntry.direction(),
                                      firstEntry.device(), firstEntry.type(), firstEntry.utcOffset(), firstEntry.noise(), firstEntry.filtered(), firstEntry.unfiltered(),
                                      firstEntry.rssi(), firstEntry.delta(), firstEntry.sysTime());
        } else {
            minEntry = entries.get(0);
        }

        double deltaTime = OffsetDateTime.now().toEpochSecond() - minEntry.datelong();
        if (deltaTime > currentInterval.getSeconds()) {
            deltaTime = OffsetDateTime.now().toEpochSecond() - OffsetDateTime.now().minusSeconds(currentInterval.getSeconds()).toEpochSecond();
        } else if (deltaTime < currentInterval.getSeconds()) {
            deltaTime = OffsetDateTime.now().toEpochSecond() - OffsetDateTime.now().minusSeconds(currentInterval.getSeconds()).toEpochSecond();
        }

        ZonedDateTime minDate         = Helper.getZonedDateTimeFromEpochSeconds(minEntry.datelong());
        double        stepX           = availableWidth / deltaTime;
        double        stepY           = availableHeight / (Constants.DEFAULT_GLUCO_RANGE);
        int           hour            = minDate.getHour();
        ZonedDateTime adjMinDate      = (hour == 23 && currentInterval != Interval.LAST_12_HOURS) ? minDate.plusSeconds(Interval.LAST_24_HOURS.getSeconds()) : minDate;
        ZonedDateTime firstFullHour   = (hour == 23 && currentInterval != Interval.LAST_12_HOURS) ? ZonedDateTime.of(adjMinDate.plusDays(1).toLocalDate(), LocalTime.MIDNIGHT, ZoneId.systemDefault()) : adjMinDate;
        long          startX          = firstFullHour.toEpochSecond() - minEntry.datelong();
        int           lastHour         = -1;
        double        oneHourStep      = Constants.SECONDS_PER_HOUR * stepX;
        long          hourCounter      = 0;

        
        // Collect nights
        ZonedDateTime startTime     = ZonedDateTime.ofInstant(Instant.ofEpochSecond(startX + minEntry.datelong()), ZoneId.systemDefault());
        ZonedDateTime endTime       = ZonedDateTime.ofInstant(Instant.ofEpochSecond(startX + minEntry.datelong() + currentInterval.getSeconds()), ZoneId.systemDefault());
        int           startHour     = startTime.getHour();
        int           endHour       = endTime.getHour();
        boolean       startsAtNight = false;
        List<eu.hansolo.toolboxfx.geom.Rectangle> nights = new ArrayList<>();

        // Chart starts at night
        if (Constants.NIGHT_HOURS.contains(startHour)) {
            double widthToNextFullHour = java.time.Duration.between(startTime, startTime.plusHours(1).truncatedTo(ChronoUnit.HOURS)).toSeconds() * stepX;
            double w                   = widthToNextFullHour + (10 - Constants.NIGHT_HOURS.indexOf(startHour) - 1) * oneHourStep;
            nights.add(new eu.hansolo.toolboxfx.geom.Rectangle(GRAPH_INSETS.getLeft(), GRAPH_INSETS.getTop(), w, availableHeight));
        }

        // Chart ends at night
        if (Constants.NIGHT_HOURS.contains(endHour)) {
            int           dayOffset  = startTime.getDayOfMonth() == endTime.getDayOfMonth() ? 0 : 1;
            ZonedDateTime nightStart = ZonedDateTime.of(endTime.toLocalDate().minusDays(dayOffset), LocalTime.of(Constants.NIGHT_START, 0), ZoneId.systemDefault());
            double w                 = availableWidth - (endTime.toEpochSecond() - nightStart.toEpochSecond()) * stepX;
            nights.add(new eu.hansolo.toolboxfx.geom.Rectangle(GRAPH_INSETS.getLeft() + (nightStart.toEpochSecond() - startX) * stepX, GRAPH_INSETS.getTop(), w, availableHeight));
        }

        // Full nights
        boolean nightStart = false;
        double  nightX     = -1;
        for (long i = startX ; i <= deltaTime ; i++) {
            int    h = ZonedDateTime.ofInstant(Instant.ofEpochSecond(i + minEntry.datelong()), ZoneId.systemDefault()).getHour();
            double x = GRAPH_INSETS.getLeft() + i * stepX;
            if (h != lastHour) {
                if (!startsAtNight && Constants.NIGHT_START == h && !nightStart) {
                    nightStart = true;
                    nightX = x;
                    }
                if (Constants.NIGHT_END == h && nightStart) {
                    nightStart = false;
                    nights.add(new eu.hansolo.toolboxfx.geom.Rectangle(nightX, GRAPH_INSETS.getTop(), 10 * oneHourStep, availableHeight));
                    }
                }
            lastHour = h;
            }

        if (nightStart) {
            nights.add(new eu.hansolo.toolboxfx.geom.Rectangle(nightX, GRAPH_INSETS.getTop(), availableWidth - nightX + GRAPH_INSETS.getRight(), availableHeight));
        }

        // Draw nights
        ctx.save();
        ctx.setFill(darkMode ? Color.rgb(255, 255, 255, 0.15) : Color.rgb(0, 0, 0, 0.1));
        nights.forEach(night -> ctx.fillRect(night.getX(), night.getY(), night.width, night.getHeight()));
        ctx.restore();

        // Draw vertical lines
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.setTextAlign(TextAlignment.CENTER);
        long interval;
        switch(currentInterval) {
            case LAST_168_HOURS:
            case LAST_720_HOURS:
            case LAST_72_HOURS: interval = Interval.LAST_6_HOURS.getHours(); break;
            case LAST_48_HOURS: interval = Interval.LAST_3_HOURS.getHours(); break;
            default           : interval = 1; break;
        }
        hourCounter = 0;
        for (long i = startX ; i <= deltaTime ; i++) {
            int    h = ZonedDateTime.ofInstant(Instant.ofEpochSecond(i + minEntry.datelong()), ZoneId.systemDefault()).getHour();
            double x = GRAPH_INSETS.getLeft() + i * stepX;
            if (h != lastHour && lastHour != -1 && i != startX) {
                if (hourCounter % interval == 0) {
                    ctx.strokeLine(x, GRAPH_INSETS.getTop(), x, height - GRAPH_INSETS.getBottom());
                    switch (currentInterval) {
                        case LAST_3_HOURS:
                        case LAST_6_HOURS: ctx.fillText(h + ":00", x, height - GRAPH_INSETS.getBottom() * 0.5); break;
                        default          : ctx.fillText(Integer.toString(h), x, height - GRAPH_INSETS.getBottom() * 0.25); break;
                    }
                }
                hourCounter++;
            }
            lastHour = h;
        }

        // Draw horizontal grid lines
        ctx.setTextAlign(TextAlignment.RIGHT);
        double yLabelStep = availableHeight / yAxisLabels.size();
        for (int i = 0 ; i < yAxisLabels.size() ; i++) {
            double y = height - GRAPH_INSETS.getBottom() - i * yLabelStep - yLabelStep;
            ctx.strokeLine(GRAPH_INSETS.getLeft(), y, width - GRAPH_INSETS.getRight(), y);
            ctx.fillText(yAxisLabels.get(i), GRAPH_INSETS.getLeft() * 2.5, y + 4);
        }

        ctx.setLineWidth(0.5);
        ctx.setLineDashes();

        // Draw acceptable limits
        double minAcceptable = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_ACCEPTABLE) * stepY;
        double maxAcceptable = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_ACCEPTABLE) * stepY;
        ctx.setStroke(Constants.YELLOW);
        ctx.strokeLine(3 * GRAPH_INSETS.getLeft(), minAcceptable, width - GRAPH_INSETS.getRight(), minAcceptable);
        ctx.strokeLine(3 * GRAPH_INSETS.getLeft(), maxAcceptable, width - GRAPH_INSETS.getRight(), maxAcceptable);

        // Draw normal area
        double minNormal    = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL) * stepY;
        double maxNormal    = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL) * stepY;
        double heightNormal = (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)) * stepY;
        ctx.setFill(HelperFX.getColorWithOpacity(Constants.GREEN, 0.1));
        ctx.setStroke(Constants.GREEN);
        ctx.setLineWidth(1);
        ctx.fillRect(3 * GRAPH_INSETS.getLeft(), maxNormal, availableWidth - 2 * GRAPH_INSETS.getRight(), heightNormal);
        ctx.strokeLine( 3 * GRAPH_INSETS.getLeft(), minNormal, width - GRAPH_INSETS.getRight(), minNormal);
        ctx.strokeLine(3 * GRAPH_INSETS.getLeft(), maxNormal, width - GRAPH_INSETS.getRight(), maxNormal);

        // Draw average
        double average;
        if (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit) {
            average = (height - GRAPH_INSETS.getBottom()) - avg * stepY;
        } else {
            average = (height - GRAPH_INSETS.getBottom()) - (Helper.mmolPerLiterToMgPerDeciliter(avg)) * stepY;
        }
        ctx.setLineDashes(2,6);
        ctx.setStroke(Helper.getColorForValue(currentUnit, avg));
        ctx.strokeLine(GRAPH_INSETS.getLeft() * 3, average, width - GRAPH_INSETS.getRight(), average);

        // Draw delta chart
        ctx.setLineDashes();
        if (deltaChartVisible) {
            ctx.setStroke(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
            ctx.setLineWidth(0.5);
            double offsetX  = GRAPH_INSETS.getLeft() + (availableWidth - Constants.DELTA_CHART_WIDTH) * 0.5;
            double factorY  = Constants.DELTA_CHART_HEIGHT / Math.max(Math.abs(deltaMax), Math.abs(deltaMin));
            double boxWidth = 5;
            double spacer   = 5;
            double zeroY    = GRAPH_INSETS.getTop() + 50;
            if (deltas.size() > 0) {
                for (int i = 0; i < 12; i++) {
                    double delta = MILLIGRAM_PER_DECILITER == currentUnit ? deltas.get(i) : Helper.mgPerDeciliterToMmolPerLiter(deltas.get(i));
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
        ctx.moveTo(GRAPH_INSETS.getLeft() + startX, height - GRAPH_INSETS.getBottom() - entries.get(0).sgv() * stepY);
        for (int i = 0 ; i < entries.size() ; i++) {
            GlucoEntry entry = entries.get(i);
            ctx.lineTo(GRAPH_INSETS.getLeft() + startX + (entry.datelong() - minEntry.datelong()) * stepX, (height - GRAPH_INSETS.getBottom()) - entry.sgv() * stepY);
        }
        ctx.lineTo(width - GRAPH_INSETS.getRight(), (height - GRAPH_INSETS.getBottom()) - entries.get(entries.size() - 1).sgv() * stepY);
        ctx.stroke();
    }


    // ******************** Factory methods ***********************************
    private Text createDeltaText(final String text, final boolean bold, final double size) {
        Text t = new Text(text);
        t.setFont(bold ? Fonts.configRoundedSemibold(size) : Fonts.configRoundedRegular(size));
        t.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        return t;
    }
    private MacosLabel createLabel(final String text, final double size, final boolean bold, final boolean rounded, final Pos alignment) {
        MacosLabel label = new MacosLabel(text);
        label.setDark(darkMode);
        if (rounded) {
            //label.setFont(bold ? Fonts.sfRoundedBold(size) : Fonts.configRoundedRegular(size));
            label.setFont(bold ? Fonts.sfRoundedBold(size) : Fonts.sfRoundedRegular(size));
        } else {
            //label.setFont(bold ? Fonts.configRoundedSemibold(size) : Fonts.configRoundedRegular(size));
            label.setFont(bold ? Fonts.configRoundedSemibold(size) : Fonts.sfRoundedRegular(size));
        }
        label.setAlignment(alignment);
        return label;
    }
    private MacosLabel createLabel(final String text, final double size, final Paint color, final boolean bold, final Pos alignment, final Priority priority) {
        MacosLabel label = new MacosLabel(text);
        //label.setFont(bold ? Fonts.configRoundedSemibold(size) : Fonts.configRoundedRegular(size));
        label.setFont(bold ? Fonts.configRoundedSemibold(size) : Fonts.sfRoundedRegular(size));
        label.setDark(darkMode);
        label.setAlignment(alignment);
        label.setPrefWidth(250);
        HBox.setHgrow(label, priority);
        return label;
    }

    private MacosToggleButton createToggleButton(final String text, final ToggleGroup toggleGroup, final EventHandler<MouseEvent> handler, final boolean dark) {
        MacosToggleButton toggleButton = new MacosToggleButton(text);
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setToggleGroup(toggleGroup);
        toggleButton.addEventFilter(MouseEvent.MOUSE_PRESSED, handler);
        toggleButton.setDark(dark);
        HBox.setHgrow(toggleButton, Priority.ALWAYS);
        return toggleButton;
    }

    private MacosToggleButtonBarSeparator createSeparator(final boolean dark) {
        MacosToggleButtonBarSeparator sep = new MacosToggleButtonBarSeparator();
        sep.setDark(dark);
        return sep;
    }


    // ******************** Settings ******************************************
    private StackPane createPrefPane() {
        String format = MILLIGRAM_PER_DECILITER == currentUnit ? "%.0f" : "%.1f";

        MacosButton backButton = new MacosButton("\u2190");
        backButton.setDark(darkMode);
        backButton.setFont(Fonts.configRoundedSemibold(16));
        backButton.setPadding(new Insets(2, 5, 2, 5));
        AnchorPane.setTopAnchor(backButton, 10d);
        AnchorPane.setLeftAnchor(backButton, 10d);
        backButton.setOnAction(e -> {
            prefPane.setVisible(false);
            prefPane.setManaged(false);
            vpane.setVisible(true);
            vpane.setManaged(true);
            savePreferencesToSettings();
        });

        MacosLabel settingsLabel = new MacosLabel("Settings");
        settingsLabel.setDark(darkMode);
        settingsLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        settingsLabel.setFont(Fonts.configRoundedSemibold(14));
        AnchorPane.setTopAnchor(settingsLabel, 50d);
        AnchorPane.setLeftAnchor(settingsLabel, 30d);

        MacosLabel nightscoutUrlLabel = new MacosLabel("URL");
        nightscoutUrlLabel.setDark(darkMode);
        nightscoutUrlLabel.setFont(Fonts.configRoundedRegular(14));
        nightscoutUrlTextField = new MacosTextField();
        nightscoutUrlTextField.setDark(darkMode);
        nightscoutUrlTextField.setFont(Fonts.configRoundedRegular(14));
        nightscoutUrlTextField.setPrefWidth(TextField.USE_COMPUTED_SIZE);
        nightscoutUrlTextField.setMaxWidth(Double.MAX_VALUE);
        nightscoutUrlTextField.setPromptText("https://YOUR_DOMAIN.herokuapp.com");
        HBox.setHgrow(nightscoutUrlTextField, Priority.ALWAYS);
        HBox nightscoutUrlBox = new HBox(10, nightscoutUrlLabel, nightscoutUrlTextField);
        nightscoutUrlBox.setAlignment(Pos.CENTER);


        MacosSeparator s1 = new MacosSeparator(Orientation.HORIZONTAL);
        s1.setDark(darkMode);
        VBox.setMargin(s1, new Insets(5, 0, 5, 0));


        unitSwitch = MacosSwitchBuilder.create().dark(darkMode).ios(true).selectedColor(accentColor).build();
        MacosLabel unitLabel = new MacosLabel("Unit " + currentUnit.UNIT.getUnitShort());
        unitLabel.setDark(darkMode);
        unitLabel.setFont(Fonts.configRoundedRegular(14));
        HBox.setHgrow(unitLabel, Priority.ALWAYS);
        HBox unitBox = new HBox(10, unitSwitch, unitLabel);
        unitBox.setAlignment(Pos.CENTER_LEFT);
        unitSwitch.selectedProperty().addListener((o, ov, nv) -> {
            switchingUnits = true;
            currentUnit = nv ? MILLIGRAM_PER_DECILITER : MILLIMOL_PER_LITER;
            unitLabel.setText("Unit " + currentUnit.UNIT.getUnitShort());
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
            switchingUnits = false;
        });


        MacosSeparator s2 = new MacosSeparator(Orientation.HORIZONTAL);
        s2.setDark(darkMode);
        VBox.setMargin(s2, new Insets(5, 0, 5, 0));


        deltaChartSwitch = MacosSwitchBuilder.create().dark(darkMode).ios(true).selectedColor(accentColor).build();
        MacosLabel deltaChartLabel = new MacosLabel("Show deltas");
        deltaChartLabel.setDark(darkMode);
        deltaChartLabel.setFont(Fonts.configRoundedRegular(14));
        HBox.setHgrow(deltaChartLabel, Priority.ALWAYS);
        HBox deltaChartBox = new HBox(10, deltaChartSwitch, deltaChartLabel);
        deltaChartBox.setAlignment(Pos.CENTER_LEFT);


        MacosSeparator s3 = new MacosSeparator(Orientation.HORIZONTAL);
        s3.setDark(darkMode);
        VBox.setMargin(s3, new Insets(5, 0, 5, 0));


        MacosLabel notificationsLabel = new MacosLabel("Notifications");
        notificationsLabel.setDark(darkMode);
        notificationsLabel.setFont(Fonts.configRoundedSemibold(14));

        MacosLabel tooLowLabel      = createLabel("Too low", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        HBox tooLowBox = new HBox(10, tooLowLabel);
        tooLowBox.setAlignment(Pos.CENTER_LEFT);

        // Low
        enableLowSoundSwitch = MacosSwitchBuilder.create().dark(darkMode).ios(true).selectedColor(accentColor).build();
        MacosLabel lowLabel      = createLabel("Low", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        HBox lowBox = new HBox(10, enableLowSoundSwitch, lowLabel);
        lowBox.setAlignment(Pos.CENTER_LEFT);

        // Acceptable low
        enableAcceptableLowSoundSwitch = MacosSwitchBuilder.create().dark(darkMode).ios(true).selectedColor(accentColor).build();
        MacosLabel acceptableLowLabel      = createLabel("Acceptable low", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        HBox acceptableLowBox = new HBox(10, enableAcceptableLowSoundSwitch, acceptableLowLabel);
        acceptableLowBox.setAlignment(Pos.CENTER_LEFT);

        // Acceptable high
        enableAcceptableHighSoundSwitch = MacosSwitchBuilder.create().dark(darkMode).ios(true).selectedColor(accentColor).build();
        MacosLabel acceptableHighLabel = createLabel("Acceptable high", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        HBox acceptableHighBox = new HBox(10, enableAcceptableHighSoundSwitch, acceptableHighLabel);
        acceptableHighBox.setAlignment(Pos.CENTER_LEFT);

        // High
        enableHighSoundSwitch = MacosSwitchBuilder.create().dark(darkMode).ios(true).selectedColor(accentColor).build();
        MacosLabel highLabel = createLabel("High", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        HBox highBox = new HBox(10, enableHighSoundSwitch, highLabel);
        highBox.setAlignment(Pos.CENTER_LEFT);

        MacosLabel tooHighLabel = createLabel("Too high", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);
        HBox tooHighBox = new HBox(10, tooHighLabel);
        tooHighBox.setAlignment(Pos.CENTER_LEFT);


        MacosSeparator s4 = new MacosSeparator(Orientation.HORIZONTAL);
        s4.setDark(darkMode);
        VBox.setMargin(s4, new Insets(5, 0, 5, 0));


        MacosLabel tooLowIntervalLabel = createLabel( "Too low interval: 5 min", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        tooLowIntervalSlider = new MacosSlider();
        tooLowIntervalSlider.setDark(darkMode);
        tooLowIntervalSlider.setMin(5);
        tooLowIntervalSlider.setMax(10);
        tooLowIntervalSlider.setSnapToTicks(true);
        tooLowIntervalSlider.setShowTickMarks(true);
        tooLowIntervalSlider.setMinorTickCount(0);
        tooLowIntervalSlider.setMajorTickUnit(1);
        tooLowIntervalSlider.setBlockIncrement(1);
        tooLowIntervalSlider.valueProperty().addListener((o, ov, nv) -> tooLowIntervalLabel.setText("Too low interval: " + String.format(Locale.US, "%.0f", tooLowIntervalSlider.getValue()) + " min"));

        MacosLabel tooHighIntervalLabel = createLabel("Too high interval: 5 min", 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        tooHighIntervalSlider = new MacosSlider();
        tooHighIntervalSlider.setDark(darkMode);
        tooHighIntervalSlider.setMin(5);
        tooHighIntervalSlider.setMax(30);
        tooHighIntervalSlider.setSnapToTicks(true);
        tooHighIntervalSlider.setShowTickMarks(true);
        tooHighIntervalSlider.setMinorTickCount(0);
        tooHighIntervalSlider.setMajorTickUnit(5);
        tooHighIntervalSlider.setBlockIncrement(5);
        tooHighIntervalSlider.valueProperty().addListener((o, ov, nv) -> tooHighIntervalLabel.setText("Too high interval: " + String.format(Locale.US, "%.0f", tooHighIntervalSlider.getValue()) + " min"));


        MacosSeparator s5 = new MacosSeparator(Orientation.HORIZONTAL);
        s5.setDark(darkMode);
        VBox.setMargin(s5, new Insets(5, 0, 5, 0));


        MacosLabel rangesLabel = new MacosLabel("Ranges");
        rangesLabel.setDark(darkMode);
        rangesLabel.setFont(Fonts.configRoundedSemibold(14));

        MacosLabel minAcceptableLabel = createLabel(new StringBuilder().append("Min acceptable ").append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_ACCEPTABLE)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        minAcceptableSlider = new MacosSlider();
        minAcceptableSlider.setDark(darkMode);
        minAcceptableSlider.setMin(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_ACCEPTABLE_MIN : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_ACCEPTABLE_MIN));
        minAcceptableSlider.setMax(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_ACCEPTABLE_MAX : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_ACCEPTABLE_MAX));
        minAcceptableSlider.setSnapToTicks(true);
        minAcceptableSlider.setShowTickMarks(true);
        minAcceptableSlider.setMinorTickCount(0);
        minAcceptableSlider.setMajorTickUnit(1);
        minAcceptableSlider.setBlockIncrement(1);
        minAcceptableSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_ACCEPTABLE));
        minAcceptableSlider.valueProperty().addListener((o, ov, nv) -> minAcceptableLabel.setText(new StringBuilder().append("Min acceptable ").append(String.format(Locale.US, format, minAcceptableSlider.getValue())).append(" ").append(currentUnit.UNIT.getUnitShort()).toString()));

        MacosLabel minNormalLabel = createLabel(new StringBuilder().append("Min normal ").append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_NORMAL : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_NORMAL)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        minNormalSlider = new MacosSlider();
        minNormalSlider.setDark(darkMode);
        minNormalSlider.setMin(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_NORMAL_MIN : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_NORMAL_MIN));
        minNormalSlider.setMax(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MIN_NORMAL_MAX : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MIN_NORMAL_MAX));
        minNormalSlider.setSnapToTicks(true);
        minNormalSlider.setShowTickMarks(true);
        minNormalSlider.setMinorTickCount(0);
        minNormalSlider.setMajorTickUnit(1);
        minNormalSlider.setBlockIncrement(1);
        minNormalSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MIN_NORMAL : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MIN_NORMAL));
        minNormalSlider.valueProperty().addListener((o, ov, nv) -> minNormalLabel.setText(new StringBuilder().append("Min normal ").append(String.format(Locale.US, format, minNormalSlider.getValue())).append(" ").append(currentUnit.UNIT.getUnitShort()).toString()));

        MacosLabel maxNormalLabel = createLabel(new StringBuilder().append("Max normal ").append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MAX_NORMAL : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MAX_NORMAL)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        maxNormalSlider = new MacosSlider();
        maxNormalSlider.setDark(darkMode);
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
            if (switchingUnits) { return; }
            if (nv.doubleValue() > maxAcceptableSlider.getValue()) { maxAcceptableSlider.setValue(nv.doubleValue()); }
        });

        MacosLabel maxAcceptableLabel = createLabel(new StringBuilder().append("Max acceptable ").append(String.format(Locale.US, format, (UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MAX_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MAX_ACCEPTABLE)))).append(" ").append(currentUnit.UNIT.getUnitShort()).toString(), 14, Constants.BRIGHT_TEXT, false, Pos.CENTER_LEFT, Priority.SOMETIMES);

        maxAcceptableSlider = new MacosSlider();
        maxAcceptableSlider.setDark(darkMode);
        maxAcceptableSlider.setMin(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MAX_ACCEPTABLE_MIN : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_ACCEPTABLE_MIN));
        maxAcceptableSlider.setMax(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.SETTINGS_MAX_ACCEPTABLE_MAX : Helper.mgPerDeciliterToMmolPerLiter(Constants.SETTINGS_MAX_ACCEPTABLE_MAX));
        maxAcceptableSlider.setSnapToTicks(true);
        maxAcceptableSlider.setShowTickMarks(true);
        maxAcceptableSlider.setMinorTickCount(0);
        maxAcceptableSlider.setMajorTickUnit(5);
        maxAcceptableSlider.setBlockIncrement(5);
        maxAcceptableSlider.setValue(UnitDefinition.MILLIGRAM_PER_DECILITER == currentUnit ? Constants.DEFAULT_MAX_ACCEPTABLE : Helper.mgPerDeciliterToMmolPerLiter(Constants.DEFAULT_MAX_ACCEPTABLE));
        maxAcceptableSlider.valueProperty().addListener((o, ov, nv) -> {
            maxAcceptableLabel.setText(new StringBuilder().append("Max acceptable ").append(String.format(Locale.US, format, maxAcceptableSlider.getValue())).append(" ").append(currentUnit.UNIT.getUnitShort()).toString());
            if (switchingUnits) { return; }
            if (nv.doubleValue() < maxNormalSlider.getValue()) { maxNormalSlider.setValue(nv.doubleValue()); }
        });


        VBox settingsVBox = new VBox(10, nightscoutUrlBox, s1, unitBox, s2, deltaChartBox, s3,
                                     notificationsLabel, tooLowBox, lowBox, acceptableLowBox, acceptableHighBox, highBox, tooHighBox, s4,
                                     tooLowIntervalLabel, tooLowIntervalSlider, tooHighIntervalLabel, tooHighIntervalSlider, s5,
                                     rangesLabel, minAcceptableLabel, minAcceptableSlider, minNormalLabel, minNormalSlider, maxNormalLabel, maxNormalSlider, maxAcceptableLabel, maxAcceptableSlider);
        settingsVBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        settingsVBox.setFillWidth(true);

        MacosScrollPane scrollPane = new MacosScrollPane(settingsVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        AnchorPane.setTopAnchor(scrollPane, 80d);
        AnchorPane.setRightAnchor(scrollPane, 30d);
        AnchorPane.setBottomAnchor(scrollPane, 20d);
        AnchorPane.setLeftAnchor(scrollPane, 30d);

        prefContentPane = new AnchorPane(backButton, settingsLabel, scrollPane);
        prefContentPane.setBackground(new Background(new BackgroundFill(darkMode ? MacosSystemColor.BACKGROUND.dark() : MacosSystemColor.BACKGROUND.aqua(), new CornerRadii(10), Insets.EMPTY)));

        StackPane prefPane = new StackPane(prefContentPane);
        return prefPane;
    }


    // ******************** Time in Range Chart *******************************
    private StackPane createTimeInRangePane() {
        double pTooHigh = 0.2;
        double pHigh    = 0.2;
        double pNormal  = 0.2;
        double pLow     = 0.2;
        double pTooLow  = 0.2;

        timeInRangeTitleLabel = createLabel("In target range", 24, true, false, Pos.CENTER);
        timeInRangeTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        timeInRangeTimeIntervalLabel = createLabel(currentInterval.getUiString(), 20, false, false, Pos.CENTER);
        timeInRangeTimeIntervalLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        double columnSize  = 140;
        timeInRangeTooHighRect = createTimeInRangeRectangle(pTooHigh, columnSize, Constants.RED);
        timeInRangeHighRect    = createTimeInRangeRectangle(pHigh, columnSize, Constants.YELLOW);
        timeInRangeNormalRect  = createTimeInRangeRectangle(pNormal, columnSize, Constants.GREEN);
        timeInRangeLowRect     = createTimeInRangeRectangle(pLow, columnSize, Constants.ORANGE);
        timeInRangeTooLowRect  = createTimeInRangeRectangle(pTooLow, columnSize, Constants.RED);
        VBox rectBox = new VBox(1, timeInRangeTooHighRect, timeInRangeHighRect, timeInRangeNormalRect, timeInRangeLowRect, timeInRangeTooLowRect);

        Color textFill = darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT;

        timeInRangeTooHighValue     = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pTooHigh * 100), 20, textFill, false, Pos.CENTER_RIGHT);
        timeInRangeTooHighValueText = createTimeInRangeLabel("Very high", 20, textFill, false, Pos.CENTER_LEFT);
        HBox       tooHighText      = new HBox(10, timeInRangeTooHighValue, timeInRangeTooHighValueText);

        timeInRangeHighValue        = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pHigh * 100), 20, textFill, false, Pos.CENTER_RIGHT);
        timeInRangeHighValueText    = createTimeInRangeLabel("High", 20, textFill, false, Pos.CENTER_LEFT);
        HBox       highText         = new HBox(10, timeInRangeHighValue, timeInRangeHighValueText);

        timeInRangeNormalValue      = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pNormal * 100), 22, textFill, true, Pos.CENTER_RIGHT);
        timeInRangeNormalValueText  = createTimeInRangeLabel("In target range", 22, textFill, true, Pos.CENTER_LEFT);
        HBox       normalText       = new HBox(10, timeInRangeNormalValue, timeInRangeNormalValueText);

        timeInRangeLowValue         = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pLow * 100), 20, textFill, false, Pos.CENTER_RIGHT);
        timeInRangeLowValueText     = createTimeInRangeLabel("Low", 20, textFill, false, Pos.CENTER_LEFT);
        HBox       lowText          = new HBox(10, timeInRangeLowValue, timeInRangeLowValueText);

        timeInRangeTooLowValue      = createTimeInRangeLabel(String.format(Locale.US, "%.0f%% ", pTooLow * 100), 20, textFill, false, Pos.CENTER_RIGHT);
        timeInRangeTooLowValueText  = createTimeInRangeLabel("Very low", 20, textFill, false, Pos.CENTER_LEFT);
        HBox       tooLowText       = new HBox(10, timeInRangeTooLowValue, timeInRangeTooLowValueText);

        VBox       textBox          = new VBox(5, tooHighText, highText, normalText, lowText, tooLowText);

        HBox       inRangeBox       = new HBox(10, rectBox, textBox);
        HBox.setMargin(rectBox, new Insets(0, 0, 0, 20));

        MacosButton closeButton = new MacosButton("Close");
        closeButton.setDark(darkMode);

        VBox       content          = new VBox(20, timeInRangeTitleLabel, timeInRangeTimeIntervalLabel, inRangeBox, closeButton);
        content.setAlignment(Pos.CENTER);
        content.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));

        StackPane timeInRangePane = new StackPane(content);
        timeInRangePane.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        timeInRangePane.setBackground(new Background(new BackgroundFill(darkMode ? Constants.DARK_BACKGROUND : Color.WHITE, new CornerRadii(10), Insets.EMPTY)));
        timeInRangePane.setBorder(new Border(new BorderStroke(Color.rgb(78, 77, 76), BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));

        closeButton.setOnAction(e -> {
            timeInRangePane.setVisible(false);
            timeInRangePane.setManaged(false);
            vpane.setVisible(true);
            vpane.setManaged(true);
            dialogVisible.set(false);
        });

        return timeInRangePane;
    }

    private void showTimeInRangeChart() {
        if (dialogVisible.get()) { return; }
        dialogVisible.set(true);
        vpane.setVisible(false);
        vpane.setManaged(false);
        timeInRangePane.setManaged(true);
        timeInRangePane.setVisible(true);

        double noOfValues = entries.size();
        double pTooHigh = (entries.stream().filter(entry -> entry.sgv() > Constants.DEFAULT_MAX_CRITICAL).count() / noOfValues);
        double pHigh    = (entries.stream().filter(entry -> entry.sgv() > PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)).filter(entry -> entry.sgv() <= PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_CRITICAL)).count() / noOfValues);
        double pNormal  = (entries.stream().filter(entry -> entry.sgv() > PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)).filter(entry -> entry.sgv() <= PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL)).count() / noOfValues);
        double pLow     = (entries.stream().filter(entry -> entry.sgv() > Constants.DEFAULT_MIN_CRITICAL).filter(entry -> entry.sgv() <= PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)).count() / noOfValues);
        double pTooLow  = (entries.stream().filter(entry -> entry.sgv() < Constants.DEFAULT_MIN_CRITICAL).count() / noOfValues);

        titleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        
        timeInRangeTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        timeInRangeTimeIntervalLabel.setText(currentInterval.getUiString());
        timeInRangeTimeIntervalLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        timeInRangeTooHighRect.setHeight(pTooHigh * 140);
        timeInRangeHighRect.setHeight(pHigh * 140);
        timeInRangeNormalRect.setHeight(pNormal * 140);
        timeInRangeLowRect.setHeight(pLow * 140);
        timeInRangeTooLowRect.setHeight(pTooLow * 140);

        timeInRangeTooHighValue.setText(String.format(Locale.US, "%.0f%% ", pTooHigh * 100));
        timeInRangeHighValue.setText(String.format(Locale.US, "%.0f%% ", pHigh * 100));
        timeInRangeNormalValue.setText(String.format(Locale.US, "%.0f%% ", pNormal * 100));
        timeInRangeLowValue.setText(String.format(Locale.US, "%.0f%% ", pLow * 100));
        timeInRangeTooLowValue.setText(String.format(Locale.US, "%.0f%% ", pTooLow * 100));
    }

    private Rectangle createTimeInRangeRectangle(final double heightFactor, final double columnSize, final Color color) {
        Rectangle rect = new Rectangle(50, heightFactor * 100 < 1 ? 1 : heightFactor * columnSize, color);
        rect.setStroke(darkMode ? Constants.DARK_BACKGROUND : Constants.BRIGHT_BACKGROUND);
        rect.setStrokeWidth(0.5);
        return rect;
    }

    private MacosLabel createTimeInRangeLabel(final String text, final double size, final Paint color, final boolean bold, final Pos alignment) {
        MacosLabel label = new MacosLabel(text);
        label.setMinWidth(50);
        label.setFont(bold ? Fonts.configRoundedSemibold(size) : Fonts.configRoundedRegular(size));
        label.setTextFill(color);
        label.setAlignment(alignment);
        return label;
    }


    // ******************** Pattern Chart *************************************
    private StackPane createPatternChartPane() {
        patternChartTitleLabel = createLabel("Pattern 24h", 24, true, false, Pos.CENTER);
        patternChartTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        patternChartHba1cLabel = createLabel(String.format(Locale.US, "HbA1c %.1f%% (last 30 days)", Helper.calcHbA1c(allEntries)), 20, false, false, Pos.CENTER);
        patternChartHba1cLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        patternChartZones = new ListView<>();
        patternChartZones.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        patternChartZones.setCellFactory(zoneListView -> new ZoneCell(darkMode));
        patternChartZones.setPrefHeight(200);

        patternChartCanvas = new Canvas(390, 300);

        MacosButton closeButton = new MacosButton("Close");
        closeButton.setDark(darkMode);

        VBox content = new VBox(20, patternChartTitleLabel, patternChartHba1cLabel, patternChartZones, patternChartCanvas, closeButton);
        content.setAlignment(Pos.CENTER);
        content.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));

        patternChartPane = new StackPane(content);
        patternChartPane.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        patternChartPane.setBackground(new Background(new BackgroundFill(darkMode ? Constants.DARK_BACKGROUND : Color.WHITE, new CornerRadii(10), Insets.EMPTY)));
        patternChartPane.setBorder(new Border(new BorderStroke(Color.rgb(78, 77, 76), BorderStrokeStyle.SOLID, new CornerRadii(10),new BorderWidths(1))));

        closeButton.setOnAction(e -> {
            patternChartPane.setVisible(false);
            patternChartPane.setManaged(false);
            vpane.setVisible(true);
            vpane.setManaged(true);
            dialogVisible.set(false);
        });

        return patternChartPane;
    }

    private void showPatternChart() {
        if (dialogVisible.get()) { return; }
        dialogVisible.set(true);
        vpane.setVisible(false);
        vpane.setManaged(false);
        patternChartPane.setManaged(true);
        patternChartPane.setVisible(true);

        patternChartTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        patternChartHba1cLabel.setText(String.format(Locale.US, "HbA1c %.1f%% (last 30 days)", Helper.calcHbA1c(allEntries)));
        patternChartHba1cLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        long             limit           = Instant.now().getEpochSecond() - Interval.LAST_168_HOURS.getSeconds();
        List<GlucoEntry> entriesLastWeek = allEntries.stream().filter(entry -> entry.datelong() > limit).collect(Collectors.toList());

        Map<LocalTime, DataPoint>        dataMap         = Statistics.analyze(entriesLastWeek);
        Pair<List<String>, List<String>> highAndLowZones = Statistics.findTimesWithLowAndHighValues(dataMap, 70, 180);
        List<String>                     lowZones        = highAndLowZones.getA();
        List<String>                     highZones       = highAndLowZones.getB();

        patternChartZones.getItems().clear();
        patternChartZones.getItems().addAll(lowZones);
        patternChartZones.getItems().addAll(highZones);

        GraphicsContext ctx = patternChartCanvas.getGraphicsContext2D();

        double width           = patternChartCanvas.getWidth();
        double height          = patternChartCanvas.getHeight();
        double availableWidth  = (width - GRAPH_INSETS.getLeft() - GRAPH_INSETS.getRight());
        double availableHeight = (height - GRAPH_INSETS.getTop() - GRAPH_INSETS.getBottom());

        ctx.clearRect(0, 0, width, height);
        ctx.setFont(smallTicklabelFont);
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.setStroke(Color.rgb(81, 80, 78));
        ctx.setLineDashes(3, 4);
        ctx.setLineWidth(1);

        List<String> yAxisLabels = MILLIGRAM_PER_DECILITER == currentUnit ? Constants.yAxisLabelsMgPerDeciliter : Constants.yAxisLabelsMmolPerLiter;

        // Draw vertical grid lines
        double stepX = availableWidth / 24;
        double stepY = availableHeight / (Constants.DEFAULT_GLUCO_RANGE);

        // Draw vertical grid lines
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.setTextAlign(TextAlignment.CENTER);
        for (int i = 0 ; i <= 23 ; i++) {
            double x = GRAPH_INSETS.getLeft() + i * stepX;
            ctx.strokeLine(x, GRAPH_INSETS.getTop(), x, height - GRAPH_INSETS.getBottom());
            ctx.fillText(Integer.toString(i), x, height - GRAPH_INSETS.getBottom() * 0.25);
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
        ctx.setStroke(darkMode ? Color.rgb(255, 255, 255, 0.5) : Color.rgb(0, 0, 0, 0.5));
        ctx.setFill(darkMode ? Color.rgb(255, 255, 255, 0.15) : Color.rgb(0, 0, 0, 0.1));
        Pair<List<Point>, List<Point>> pair      = Helper.createValueRangePath(dataMap, StatisticRange.TEN_TO_NINETY, true);
        List<Point>                    maxPoints = pair.getA();
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
    }


    // ******************** Matrix Chart **************************************
    private StackPane createMatrixChartPane() {
        matrixChartTitleLabel = createLabel("Last 30 days", 24, true, false, Pos.CENTER);
        matrixChartTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        matrixChartSubTitleLabel = createLabel("(daily average)", 16, false, false, Pos.CENTER);
        matrixChartSubTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        matrixChartHba1cLabel = createLabel(String.format(Locale.US, "HbA1c %.1f%% (last 30 days)", Helper.calcHbA1c(allEntries)), 20, false, false, Pos.CENTER);
        matrixChartHba1cLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        matrixChartThirtyDayView = new ThirtyDayView(allEntries, currentUnit);
        matrixChartThirtyDayView.setDark(darkMode);

        MacosButton closeButton = new MacosButton("Close");
        closeButton.setDark(darkMode);

        VBox content = new VBox(20, matrixChartTitleLabel, matrixChartSubTitleLabel, matrixChartHba1cLabel, matrixChartThirtyDayView, closeButton);
        content.setAlignment(Pos.CENTER);
        content.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));

        matrixChartPane = new StackPane(content);
        matrixChartPane.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        matrixChartPane.setBackground(new Background(new BackgroundFill(darkMode ? Constants.DARK_BACKGROUND : Color.WHITE , new CornerRadii(10), Insets.EMPTY)));
        matrixChartPane.setBorder(new Border(new BorderStroke(Color.rgb(78, 77, 76), BorderStrokeStyle.SOLID, new CornerRadii(10),new BorderWidths(1))));

        closeButton.setOnAction(e -> {
            matrixChartPane.setVisible(false);
            matrixChartPane.setManaged(false);
            vpane.setVisible(true);
            vpane.setManaged(true);
            dialogVisible.set(false);
        });

        return matrixChartPane;
    }

    private void showMatrixChart() {
        if (dialogVisible.get()) { return; }
        dialogVisible.set(true);
        vpane.setVisible(false);
        vpane.setManaged(false);
        matrixChartPane.setManaged(true);
        matrixChartPane.setVisible(true);

        matrixChartTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        matrixChartSubTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        matrixChartHba1cLabel.setText(String.format(Locale.US, "HbA1c %.1f%% (last 30 days)", Helper.calcHbA1c(allEntries)));
        matrixChartHba1cLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        matrixChartThirtyDayView.setEntries(allEntries, currentUnit);
        matrixChartThirtyDayView.setDark(darkMode);
    }


    // ******************** Overlay Chart **************************************
    private StackPane createStackedChartPane() {
        stackedChartTitleLabel = createLabel("Overlay", 24, true, false, Pos.CENTER);
        stackedChartTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        stackedChartSubTitleLabel = createLabel("7, 14 or 30 days", 16, false, false, Pos.CENTER);
        stackedChartSubTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        stackedChartHba1cLabel = createLabel(String.format(Locale.US, "HbA1c %.1f%% (last 30 days)", Helper.calcHbA1c(allEntries)), 20, false, false, Pos.CENTER);
        stackedChartHba1cLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        stackedLineChart = new StackedLineChart();
        stackedLineChart.setEntries(currentUnit, allEntries);

        MacosButton closeButton = new MacosButton("Close");
        closeButton.setDark(darkMode);

        VBox content = new VBox(20, stackedChartTitleLabel, stackedChartSubTitleLabel, stackedChartHba1cLabel, stackedLineChart, closeButton);
        content.setAlignment(Pos.CENTER);
        content.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(10), Insets.EMPTY)));

        stackedChartPane = new StackPane(content);
        stackedChartPane.getStylesheets().add(Main.class.getResource("glucostatus.css").toExternalForm());

        stackedChartPane.setBackground(new Background(new BackgroundFill(darkMode ? Constants.DARK_BACKGROUND : Color.WHITE , new CornerRadii(10), Insets.EMPTY)));
        stackedChartPane.setBorder(new Border(new BorderStroke(Color.rgb(78, 77, 76), BorderStrokeStyle.SOLID, new CornerRadii(10),new BorderWidths(1))));

        closeButton.setOnAction(e -> {
            stackedChartPane.setVisible(false);
            stackedChartPane.setManaged(false);
            vpane.setVisible(true);
            vpane.setManaged(true);
            dialogVisible.set(false);
        });

        return stackedChartPane;
    }

    private void showStackedChart() {
        if (dialogVisible.get()) { return; }
        dialogVisible.set(true);
        vpane.setVisible(false);
        vpane.setManaged(false);
        stackedChartPane.setManaged(true);
        stackedChartPane.setVisible(true);

        stackedChartTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        stackedChartSubTitleLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        stackedChartHba1cLabel.setText(String.format(Locale.US, "HbA1c %.1f%% (last 30 days)", Helper.calcHbA1c(allEntries)));
        stackedChartHba1cLabel.setTextFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);

        stackedLineChart.setEntries(currentUnit, allEntries);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
