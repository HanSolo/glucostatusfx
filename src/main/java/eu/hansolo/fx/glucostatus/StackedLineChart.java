package eu.hansolo.fx.glucostatus;

import eu.hansolo.fx.glucostatus.Records.ButtonShape;
import eu.hansolo.fx.glucostatus.Records.DayShape;
import eu.hansolo.fx.glucostatus.Records.GlucoEntry;
import eu.hansolo.toolbox.unit.UnitDefinition;
import eu.hansolo.toolboxfx.HelperFX;
import eu.hansolo.toolboxfx.geom.Rectangle;
import javafx.beans.DefaultProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIGRAM_PER_DECILITER;


/**
 * User: hansolo
 * Date: 02.05.23
 * Time: 08:46
 */
@DefaultProperty("children")
public class StackedLineChart extends Region {
    private static final double                           PREFERRED_WIDTH  = 250;
    private static final double                           PREFERRED_HEIGHT = 250;
    private static final double                           MINIMUM_WIDTH    = 50;
    private static final double                           MINIMUM_HEIGHT   = 50;
    private static final double                           MAXIMUM_WIDTH    = 1024;
    private static final double                           MAXIMUM_HEIGHT   = 1024;
    private static final Insets                           GRAPH_INSETS     = new Insets(5, 10, 5, 10);
    private              double                           width;
    private              double                           height;
    private              double                           availableWidth;
    private              double                           availableHeight;
    private              double                           stepX;
    private              double                           stepY;
    private              Canvas                           canvas;
    private              GraphicsContext                  ctx;
    private              Font                             ticklabelFont;
    private              double                           min;
    private              double                           max;
    private              double                           range;
    private              UnitDefinition                   unit;
    private              List<GlucoEntry>                 entries;
    private              Map<LocalDate, List<GlucoEntry>> entryMap;
    private              int                              daysToShow;
    private              Map<DayOfWeek, Boolean>          selectedDays;
    private              List<ButtonShape>                buttonShapes;
    private              List<DayShape>                   dayShapes;
    private              EventHandler<MouseEvent>         mouseHandler;


    // ******************** Constructors **************************************
    public StackedLineChart() {
        loadSettings();

        entries      = new ArrayList<>();
        entryMap     = new HashMap<>();
        daysToShow   = 14;
        selectedDays = new ConcurrentHashMap<>();
        buttonShapes = new CopyOnWriteArrayList<>();
        dayShapes    = new CopyOnWriteArrayList<>();
        mouseHandler = e -> {
            final EventType<? extends Event> type = e.getEventType();
            if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                double x = e.getX();
                double y = e.getY();
                buttonShapes.forEach(buttonShape -> {
                    if (buttonShape.shape().contains(x, y)) {
                        this.daysToShow = buttonShape.daysToShow();
                        filter();
                        redraw();
                    }
                });
                dayShapes.forEach(dayShape -> {
                    if (dayShape.shape().contains(x, y)) {
                        this.selectedDays.put(dayShape.day(), !selectedDays.get(dayShape.day()));
                        filter();
                        redraw();
                    }
                });
            }
        };

        selectedDays.put(DayOfWeek.MONDAY, true);
        selectedDays.put(DayOfWeek.TUESDAY, true);
        selectedDays.put(DayOfWeek.WEDNESDAY, true);
        selectedDays.put(DayOfWeek.THURSDAY, true);
        selectedDays.put(DayOfWeek.FRIDAY, true);
        selectedDays.put(DayOfWeek.SATURDAY, true);
        selectedDays.put(DayOfWeek.SUNDAY, true);

        initGraphics();
        registerListeners();
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

        getStyleClass().add("my-region");

        canvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctx    = canvas.getGraphicsContext2D();

        getChildren().setAll(canvas);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
    }


    // ******************** Methods *******************************************
    @Override protected double computeMinWidth(final double height) { return MINIMUM_WIDTH; }
    @Override protected double computeMinHeight(final double width) { return MINIMUM_HEIGHT; }
    @Override protected double computePrefWidth(final double height) { return super.computePrefWidth(height); }
    @Override protected double computePrefHeight(final double width) { return super.computePrefHeight(width); }
    @Override protected double computeMaxWidth(final double height) { return MAXIMUM_WIDTH; }
    @Override protected double computeMaxHeight(final double width) { return MAXIMUM_HEIGHT; }

    @Override public ObservableList<Node> getChildren() { return super.getChildren(); }

    public void loadSettings() {
        min   = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_VALUE);
        max   = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_VALUE);
        range = max - min;
        unit  = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_UNIT_MG) ? UnitDefinition.MILLIGRAM_PER_DECILITER : UnitDefinition.MILLIMOL_PER_LITER;
    }

    public void setEntries(final UnitDefinition currentUnit, final List<GlucoEntry> entries) {
        this.entries.clear();
        this.entries.addAll(entries);
        filter();

        this.unit = currentUnit;
        redraw();
    }

    private void filter() {
        entryMap.clear();
        LocalDate startDate = LocalDateTime.now().minusDays(daysToShow).toLocalDate();
        entries.stream().forEach(entry -> {
            final LocalDate localDate = entry.date().toLocalDate();
            if (localDate.isAfter(startDate)) {
                if (!entryMap.keySet().contains(localDate)) { entryMap.put(localDate, new ArrayList<>()); }
                final DayOfWeek day = localDate.getDayOfWeek();
                if (selectedDays.get(day)) {
                    entryMap.get(localDate).add(entry);
                }
            }
        });
    }


    // ******************** Layout *******************************************
    @Override public void layoutChildren() {
        super.layoutChildren();
    }

    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();

        if (width > 0 && height > 0) {
            canvas.setWidth(width);
            canvas.setHeight(height);
            canvas.relocate((getWidth() - width) * 0.5, (getHeight() - height) * 0.5);

            availableWidth  = (width - GRAPH_INSETS.getLeft() - GRAPH_INSETS.getRight());
            availableHeight = (height - GRAPH_INSETS.getTop() - GRAPH_INSETS.getBottom());
            stepX           = availableWidth / 24;
            stepY           = height / range;
            ticklabelFont   = Fonts.sfProTextRegular(10);

            redraw();
        }
    }

    private void redraw() {
        boolean darkMode = eu.hansolo.applefx.tools.Helper.isDarkMode();

        ctx.clearRect(0, 0, width, height);
        ctx.setFill(darkMode ? Constants.DARK_BACKGROUND : Constants.BRIGHT_BACKGROUND);
        ctx.fillRect(0, 0, width, height);

        ctx.setFont(ticklabelFont);
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.setStroke(darkMode ? Color.rgb(81, 80, 78) : Color.rgb(184, 183, 183));
        ctx.setLineDashes(3, 4);
        ctx.setLineWidth(1);
        List<String> axisLabels = MILLIGRAM_PER_DECILITER == unit ? Constants.yAxisLabelsMgPerDeciliter : Constants.yAxisLabelsMmolPerLiter;


        // Draw nights
        ctx.setFill(darkMode ? Color.rgb(255, 255, 255, 0.1) : Color.rgb(0, 0, 0, 0.1));
        ctx.fillRect(GRAPH_INSETS.getLeft() + stepX, GRAPH_INSETS.getTop(), stepX * 6, availableHeight);
        ctx.fillRect(GRAPH_INSETS.getLeft() + stepX + 20 * stepX, GRAPH_INSETS.getTop(), stepX * 4, availableHeight);

        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.setTextAlign(TextAlignment.CENTER);

        // Draw horizontal grid lines
        ctx.setTextAlign(TextAlignment.RIGHT);
        double yLabelStep = availableHeight / axisLabels.size();
        for (int i = 0 ; i < axisLabels.size() ; i++) {
            double y = height - GRAPH_INSETS.getBottom() - i * yLabelStep - yLabelStep;
            ctx.strokeLine(GRAPH_INSETS.getLeft(), y, width - GRAPH_INSETS.getRight(), y);
            ctx.fillText(axisLabels.get(i), GRAPH_INSETS.getLeft() * 2.5, y + 4);
        }

        // Draw vertical grid lines
        ctx.setTextAlign(TextAlignment.RIGHT);
        for (int h = 0 ; h < 24 ; h++) {
            double x = GRAPH_INSETS.getLeft() + h * stepX + stepX;
            ctx.strokeLine(x, GRAPH_INSETS.getTop(), x, height - GRAPH_INSETS.getBottom());
            ctx.fillText(Integer.toString(h), x, availableHeight - GRAPH_INSETS.getBottom() * 0.25);
        }

        // Draw normal area
        double minNormal    = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL) * stepY;
        double maxNormal    = (height - GRAPH_INSETS.getBottom()) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL) * stepY;
        double heightNormal = (PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_NORMAL) - PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_NORMAL)) * stepY;
        ctx.setLineDashes();
        ctx.setFill(HelperFX.getColorWithOpacity(Constants.GREEN, 0.1));
        ctx.setStroke(Constants.GREEN);
        ctx.setLineWidth(1);
        ctx.fillRect(3 * GRAPH_INSETS.getLeft(), maxNormal, availableWidth - 2 * GRAPH_INSETS.getRight(), heightNormal);
        ctx.strokeLine( 3 * GRAPH_INSETS.getLeft(), minNormal, width - GRAPH_INSETS.getRight(), minNormal);
        ctx.strokeLine(3 * GRAPH_INSETS.getLeft(), maxNormal, width - GRAPH_INSETS.getRight(), maxNormal);

        // Draw lines for each day
        //ctx.setLineDashes();
        double hourStepX = availableWidth / Constants.SECONDS_PER_DAY;
        entryMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(mapEntry -> {
            List<GlucoEntry> entries = mapEntry.getValue();
            entries.sort(Comparator.comparing(GlucoEntry::datelong));
            double lastX = stepX;
            double lastY = 0;
            for (GlucoEntry entry : entries) {
                final double    value   = entry.sgv();
                final double    seconds = entry.date().getHour() * 3600.0 + entry.date().getMinute() * 60 + entry.date().getSecond();
                //final Color     color   = Helper.getColorForValue(unit, UnitDefinition.MILLIGRAM_PER_DECILITER == unit ? value : eu.hansolo.fx.glucostatus.Helper.mgPerDeciliterToMmolPerLiter(value));
                //ctx.setStroke(color);
                final DayOfWeek day     = entry.date().getDayOfWeek();
                ctx.setStroke(Constants.DAY_COLOR_MAP.get(day));
                final double    x       = seconds * hourStepX + stepX;
                final double    y       = (max - value + min) * stepY;
                if (lastX != stepX) {
                    ctx.strokeLine(lastX, lastY, x, y);
                }
                lastX = x;
                lastY = y;
            }
        });

        // Draw days to show selector
        buttonShapes.clear();
        ctx.setLineDashes();
        ctx.setTextAlign(TextAlignment.CENTER);
        ctx.setTextBaseline(VPos.CENTER);
        ctx.setStroke(Color.GRAY);

        ctx.setFill(daysToShow == 30 ? Color.GRAY : darkMode ? Constants.DARK_BACKGROUND : Constants.BRIGHT_BACKGROUND);
        ctx.fillRoundRect(stepX + 20, 10, 60, 24, 10, 10);
        ctx.strokeRoundRect(stepX + 20, 10, 60, 24, 10, 10);
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.fillText("30 days", stepX + 50, 22, 60);
        buttonShapes.add(new ButtonShape(30, new Rectangle(stepX + 20, 10, 60, 24)));

        ctx.setFill(daysToShow == 14 ? Color.GRAY : darkMode ? Constants.DARK_BACKGROUND : Constants.BRIGHT_BACKGROUND);
        ctx.fillRoundRect(stepX + 100, 10, 60, 24, 10, 10);
        ctx.strokeRoundRect(stepX + 100, 10, 60, 24, 10, 10);
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.fillText("14 days", stepX + 130, 22, 60);
        buttonShapes.add(new ButtonShape(14, new Rectangle(stepX + 100, 10, 60, 24)));

        ctx.setFill(daysToShow == 7 ? Color.GRAY : darkMode ? Constants.DARK_BACKGROUND : Constants.BRIGHT_BACKGROUND);
        ctx.fillRoundRect(stepX + 180, 10, 60, 24, 10, 10);
        ctx.strokeRoundRect(stepX + 180, 10, 60, 24, 10, 10);
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.fillText("7 days", stepX + 210, 22, 60);
        buttonShapes.add(new ButtonShape(7, new Rectangle(stepX + 180, 10, 60, 24)));

        // Draw day selector
        dayShapes.clear();
        for (int i = 0 ; i < 7 ; i++) {
            final DayOfWeek day = DayOfWeek.values()[6 - i];
            ctx.setFill(selectedDays.get(day) ? Color.GRAY : darkMode ? Constants.DARK_BACKGROUND : Constants.BRIGHT_BACKGROUND);
            ctx.setStroke(Color.GRAY);
            ctx.fillRoundRect(availableWidth - stepX * i - 24, 10, 24, 24, 10, 10);
            ctx.strokeRoundRect(availableWidth - stepX * i - 24, 10, 24, 24, 10, 10);
            ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
            ctx.fillText(day.name().substring(0, 1), availableWidth - stepX * i - 12, 22, 24);
            ctx.setStroke(Constants.DAY_COLOR_MAP.get(day));
            ctx.strokeLine(availableWidth - stepX * i - 20, 30, availableWidth - stepX * i - 4, 30);
            dayShapes.add(new DayShape(day, new Rectangle(availableWidth - stepX * i - 24, 10, 24, 24)));
        }
    }
}