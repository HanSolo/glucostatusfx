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

 import eu.hansolo.fx.glucostatus.Records.GlucoEntry;
 import eu.hansolo.toolbox.tuples.Pair;
 import eu.hansolo.toolbox.unit.UnitDefinition;
 import eu.hansolo.toolboxfx.geom.Rectangle;
 import javafx.animation.PauseTransition;
 import javafx.beans.DefaultProperty;
 import javafx.beans.property.BooleanProperty;
 import javafx.beans.property.BooleanPropertyBase;
 import javafx.collections.ObservableList;
 import javafx.css.PseudoClass;
 import javafx.geometry.Insets;
 import javafx.geometry.VPos;
 import javafx.scene.Node;
 import javafx.scene.canvas.Canvas;
 import javafx.scene.canvas.GraphicsContext;
 import javafx.scene.input.MouseEvent;
 import javafx.scene.layout.Region;
 import javafx.scene.paint.Color;
 import javafx.scene.text.TextAlignment;
 import javafx.util.Duration;

 import java.time.DayOfWeek;
 import java.time.Instant;
 import java.time.LocalDate;
 import java.time.LocalTime;
 import java.time.ZoneId;
 import java.time.ZonedDateTime;
 import java.time.format.TextStyle;
 import java.time.temporal.WeekFields;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Optional;
 import java.util.Random;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.stream.Collectors;


 @DefaultProperty("children")
 public class ThirtyDayView extends Region {
     private static final Random                                  RND               = new Random();
     private static final double                                  PREFERRED_WIDTH   = 400;
     private static final double                                  PREFERRED_HEIGHT  = 300;
     private static final double                                  MINIMUM_WIDTH     = 50;
     private static final double                                  MINIMUM_HEIGHT    = 50;
     private static final double                                  MAXIMUM_WIDTH     = 4096;
     private static final double                                  MAXIMUM_HEIGHT    = 4096;
     private static final int                                     SLEEP_DURATION    = 3000;
     private static final PseudoClass                             DARK_PSEUDO_CLASS = PseudoClass.getPseudoClass("dark");
     private              boolean                                 _dark;
     private              BooleanProperty                         dark;
     private              String                                  userAgentStyleSheet;
     private              double                                  boxWidth;
     private              double                                  boxHeight;
     private              double                                  boxCenterX;
     private              double                                  boxCenterY;
     private              double                                  boxRadius;
     private              double                                  boxOffset;
     private              double                                  doubleBoxOffset;
     private              double                                  width;
     private              double                                  height;
     private              double                                  size;
     private              Canvas                                  canvas;
     private              GraphicsContext                         ctx;
     private              UnitDefinition                          unit;
     private              Map<LocalDate, Double>                  entries;
     private              Map<LocalDate,Rectangle>                boxes;
     private              LocalDate                               selectedDate;


     // ******************** Constructors **************************************
     public ThirtyDayView() {
         this(List.of(), UnitDefinition.MILLIGRAM_PER_DECILITER);
     }
     public ThirtyDayView(final List<GlucoEntry> glucoEntries, final UnitDefinition unit) {
         this._dark        = true; //eu.hansolo.applefx.tools.Helper.isDarkMode();
         this.entries      = new ConcurrentHashMap<>(30);
         this.unit         = unit;
         this.boxes        = new ConcurrentHashMap<>();
         this.selectedDate = null;
         initGraphics();
         registerListeners();

         pseudoClassStateChanged(DARK_PSEUDO_CLASS, _dark);
         setEntries(glucoEntries, unit);
     }


     // ******************** Initialization ************************************
     private void initGraphics() {
         if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 || Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
             if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                 setPrefSize(getPrefWidth(), getPrefHeight());
             } else {
                 setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
             }
         }

         getStyleClass().add("thirty-day-view");

         canvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
         ctx    = canvas.getGraphicsContext2D();

         setPadding(new Insets(10));

         getChildren().setAll(canvas);
     }

     private void registerListeners() {
         widthProperty().addListener(o -> resize());
         heightProperty().addListener(o -> resize());
         canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
             Optional<Entry<LocalDate, Rectangle>> optEntry = boxes.entrySet().stream().filter(entry -> entry.getValue().contains(e.getX(), e.getY())).findFirst();
             selectedDate = optEntry.isPresent() ? optEntry.get().getKey() : null;
             redraw();
             PauseTransition pause = new PauseTransition(Duration.millis(SLEEP_DURATION));
             pause.setOnFinished(ev -> {
                 selectedDate = null;
                 redraw();
             });
             pause.play();
         });
     }


     // ******************** Methods *******************************************
     @Override protected double computeMinWidth(final double height) { return MINIMUM_WIDTH; }
     @Override protected double computeMinHeight(final double width) { return MINIMUM_HEIGHT; }
     @Override protected double computePrefWidth(final double height) { return super.computePrefWidth(height); }
     @Override protected double computePrefHeight(final double width) { return super.computePrefHeight(width); }
     @Override protected double computeMaxWidth(final double height) { return MAXIMUM_WIDTH; }
     @Override protected double computeMaxHeight(final double width) { return MAXIMUM_HEIGHT; }

     @Override public ObservableList<Node> getChildren() { return super.getChildren(); }

     public final boolean isDark() {
         return null == dark ? _dark : dark.get();
     }
     public final void setDark(final boolean dark) {
         if (null == this.dark) {
             _dark = dark;
             pseudoClassStateChanged(DARK_PSEUDO_CLASS, dark);
         } else {
             this.dark.set(dark);
         }
     }
     public final BooleanProperty darkProperty() {
         if (null == dark) {
             dark = new BooleanPropertyBase() {
                 @Override protected void invalidated() { pseudoClassStateChanged(DARK_PSEUDO_CLASS, get()); }
                 @Override public Object getBean() { return ThirtyDayView.this; }
                 @Override public String getName() { return "dark"; }
             };
         }
         return dark;
     }

     public void setEntries(final List<GlucoEntry> entries, final UnitDefinition unit) {
         this.unit = unit;

         ZonedDateTime    currentDate     = ZonedDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, ZoneId.systemDefault());
         ZonedDateTime    startDate       = currentDate.minusDays(30);
         long             startDateLong   = startDate.toEpochSecond();
         List<GlucoEntry> filteredEntries = entries.stream().filter(entry -> entry.datelong() >= startDateLong).collect(Collectors.toList());
         Map<LocalDate, Pair<Double,Double>> aggregatedEntries = new HashMap<>();
         filteredEntries.forEach(entry -> {
             LocalDate date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(entry.datelong()), ZoneId.systemDefault()).toLocalDate();
             Optional<Entry<LocalDate, Pair<Double, Double>>> optEntry = aggregatedEntries.entrySet().stream().filter(ntry -> ntry.getKey().isEqual(date)).findFirst();
             if (optEntry.isPresent()) {
                 double value      = aggregatedEntries.get(optEntry.get().getKey()).getA();
                 double noOfValues = aggregatedEntries.get(optEntry.get().getKey()).getB();
                 aggregatedEntries.put(optEntry.get().getKey(), new Pair<>(value + entry.sgv(), noOfValues + 1.0));
             } else {
                 aggregatedEntries.put(date, new Pair<>(entry.sgv(), 1.0));
             }
         });

         this.entries.clear();
         aggregatedEntries.entrySet().forEach(entry -> this.entries.put(entry.getKey(), (entry.getValue().getA() / entry.getValue().getB())));

         redraw();
     }


     // ******************** Layout *******************************************
     @Override public void layoutChildren() {
         super.layoutChildren();
     }

     @Override public String getUserAgentStylesheet() {
         if (null == userAgentStyleSheet) { userAgentStyleSheet = ThirtyDayView.class.getResource("thirty-day-view.css").toExternalForm(); }
         return userAgentStyleSheet;
     }

     private void resize() {
         width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
         height = getHeight() - getInsets().getTop() - getInsets().getBottom();
         size   = width < height ? width : height;

         if (width > 0 && height > 0) {
             canvas.setWidth(width);
             canvas.setHeight(height);
             canvas.relocate((getWidth() - width) * 0.5, (getHeight() - height) * 0.5);

             boxWidth        = width / 8;
             boxHeight       = height / 7;
             boxCenterX      = boxWidth * 0.5;
             boxCenterY      = boxHeight * 0.5;
             boxRadius       = size * 0.05;
             boxOffset       = size * 0.01;
             doubleBoxOffset = 2 * boxOffset;

             redraw();
         }
     }

     private void redraw() {
         boxes.clear();

         LocalDate currentDate     = LocalDate.now();
         LocalDate startDate       = currentDate.minusDays(30);
         int       startWeek       = startDate.get(WeekFields.ISO.weekOfYear());
         Color     foregroundColor = isDark() ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT;

         ctx.clearRect(0, 0, width, height);
         ctx.setStroke(foregroundColor);
         ctx.setFill(foregroundColor);
         ctx.setFont(Fonts.sfProRoundedBold(size * 0.065));
         ctx.setTextAlign(TextAlignment.CENTER);
         ctx.setTextBaseline(VPos.CENTER);
         for (int y = 0 ; y < 8 ; y++) {
             for (int x = 0 ; x < 9 ; x++) {
                 double posX = x * boxWidth;
                 double posY = y * boxHeight;
                 if (x == 0 && y > 0) {
                     ctx.fillText(Integer.toString(startWeek + y), posX + boxCenterX, posY + boxCenterY);
                 } else if (y == 0 && x > 0 && x < 8) {
                     ctx.fillText(DayOfWeek.values()[x - 1].getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault()), posX + boxCenterX, posY + boxCenterY);
                 }
             }
         }

         int indexX = currentDate.getDayOfWeek().getValue() - 1;
         int indexY = 5;
         ctx.setFont(Fonts.sfProRoundedRegular(size * 0.065));
         for (int i = 0 ; i < 30 ; i++) {
             LocalDate date = currentDate.minusDays(i);
             double  posX       = boxWidth + indexX * boxWidth;
             double  posY       = boxHeight + indexY * boxHeight;
             boolean showValue  = null != selectedDate && date.isEqual(selectedDate) && entries.containsKey(selectedDate);
             Color   valueColor = entries.isEmpty() ? Constants.GRAY : Helper.getColorForValue2(unit, UnitDefinition.MILLIGRAM_PER_DECILITER == unit ? entries.get(date) : Helper.mgPerDeciliterToMmolPerLiter(entries.get(date)));
             if (entries.containsKey(date)) {
                 if (showValue) {
                     ctx.setStroke(foregroundColor);
                     ctx.strokeRoundRect(posX + boxOffset, posY + boxOffset, boxWidth - doubleBoxOffset, boxHeight - doubleBoxOffset, boxRadius, boxRadius);
                 } else {
                     ctx.setFill(valueColor);
                     ctx.fillRoundRect(posX + boxOffset, posY + boxOffset, boxWidth - doubleBoxOffset, boxHeight - doubleBoxOffset, boxRadius, boxRadius);
                 }
             }
             boxes.put(date, new Rectangle(posX + boxOffset, posY + boxOffset, boxWidth - doubleBoxOffset, boxHeight - doubleBoxOffset));

             if (showValue) {
                 ctx.setFill(isDark() ? valueColor : valueColor.darker());
                 if (UnitDefinition.MILLIGRAM_PER_DECILITER == unit) {
                     ctx.fillText(String.format(Locale.US, "%.0f", this.entries.get(selectedDate)), posX + boxCenterX, posY + boxCenterY, boxWidth);
                 } else {
                     ctx.fillText(String.format(Locale.US, "%.1f", Helper.mgPerDeciliterToMmolPerLiter(this.entries.get(selectedDate))), posX + boxCenterX, posY + boxCenterY, boxWidth);
                 }
             } else {
                 ctx.setFill(foregroundColor);
                 ctx.fillText(Integer.toString(date.getDayOfMonth()), posX + boxCenterX, posY + boxCenterY);
             }

             indexX = indexX - 1;
             if (indexX == -1) {
                 indexX = 6;
                 indexY--;
             }
         }
     }
 }