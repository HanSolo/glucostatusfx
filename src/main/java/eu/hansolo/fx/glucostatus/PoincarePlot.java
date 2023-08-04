package eu.hansolo.fx.glucostatus;

import eu.hansolo.toolbox.unit.UnitDefinition;
import javafx.beans.DefaultProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

import static eu.hansolo.toolbox.Helper.clamp;
import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIGRAM_PER_DECILITER;


@DefaultProperty("children")
public class PoincarePlot extends Region {
    private static final double          PREFERRED_WIDTH  = 250;
    private static final double          PREFERRED_HEIGHT = 250;
    private static final double          MINIMUM_WIDTH    = 50;
    private static final double          MINIMUM_HEIGHT   = 50;
    private static final double          MAXIMUM_WIDTH    = 1024;
    private static final double          MAXIMUM_HEIGHT   = 1024;
    private static final double          MIN_SYMBOL_SIZE  = 2;
    private static final double          MAX_SYMBOL_SIZE  = 6;
    private static final Insets          GRAPH_INSETS     = new Insets(5, 10, 5, 10);
    private              double          size;
    private              double          width;
    private              double          height;
    private              double          availableWidth;
    private              double          availableHeight;
    private              double          stepX;
    private              double          stepY;
    private              double          symbolSize;
    private              double          halfSymbolSize;
    private              Canvas          canvas;
    private              GraphicsContext ctx;
    private              Font            ticklabelFont;
    private              double          min;
    private              double          max;
    private              double          range;
    private              UnitDefinition  unit;
    private              List<Double>    values;


    // ******************** Constructors **************************************
    public PoincarePlot() {
        loadSettings();

        values = new ArrayList<>();

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

        canvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctx    = canvas.getGraphicsContext2D();

        getChildren().setAll(canvas);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
    }


    // ******************** Methods *******************************************
    @Override protected double computeMinWidth(final double height)  { return MINIMUM_WIDTH; }
    @Override protected double computeMinHeight(final double width)  { return MINIMUM_HEIGHT; }
    @Override protected double computePrefWidth(final double height) { return super.computePrefWidth(height); }
    @Override protected double computePrefHeight(final double width) { return super.computePrefHeight(width); }
    @Override protected double computeMaxWidth(final double height)  { return MAXIMUM_WIDTH; }
    @Override protected double computeMaxHeight(final double width)  { return MAXIMUM_HEIGHT; }

    @Override public ObservableList<Node> getChildren() { return super.getChildren(); }

    public void loadSettings() {
        min   = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MIN_VALUE);
        max   = PropertyManager.INSTANCE.getDouble(Constants.PROPERTIES_MAX_VALUE);
        range = max - min;
        unit  = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_UNIT_MG) ? UnitDefinition.MILLIGRAM_PER_DECILITER : UnitDefinition.MILLIMOL_PER_LITER;
    }

    public void setValues(final UnitDefinition currentUnit, final List<Double> values) {
        this.unit = currentUnit;
        this.values.clear();
        this.values.addAll(values);
        redraw();
    }


    // ******************** Layout *******************************************
    @Override public void layoutChildren() {
        super.layoutChildren();
    }

    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();
        size   = width < height ? width : height;

        if (width > 0 && height > 0) {
            canvas.setWidth(width);
            canvas.setHeight(height);
            canvas.relocate((getWidth() - width) * 0.5, (getHeight() - height) * 0.5);

            availableWidth  = (width - GRAPH_INSETS.getLeft() - GRAPH_INSETS.getRight());
            availableHeight = (height - GRAPH_INSETS.getTop() - GRAPH_INSETS.getBottom());
            stepX           = width / range;
            stepY           = height / range;
            symbolSize      = clamp(MIN_SYMBOL_SIZE, MAX_SYMBOL_SIZE, size * 0.016);
            halfSymbolSize  = symbolSize * 0.5;
            ticklabelFont   = Fonts.sfProTextRegular(10);

            redraw();
        }
    }

    private void redraw() {
        boolean darkMode = PropertyManager.INSTANCE.getBoolean(Constants.PROPERTIES_DARK_MODE, true);

        ctx.clearRect(0, 0, width, height);
        ctx.setFill(darkMode ? Constants.DARK_BACKGROUND : Color.rgb(255, 255, 255));
        ctx.fillRect(0, 0, width, height);

        ctx.setFont(ticklabelFont);
        ctx.setFill(darkMode ? Constants.BRIGHT_TEXT : Constants.DARK_TEXT);
        ctx.setStroke(darkMode ? Color.rgb(81, 80, 78) : Color.rgb(184, 183, 183));
        ctx.setLineDashes(3, 4);
        ctx.setLineWidth(1);
        List<String> axisLabels = MILLIGRAM_PER_DECILITER == unit ? Constants.yAxisLabelsMgPerDeciliter : Constants.yAxisLabelsMmolPerLiter;

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
        double xLabelStep = availableWidth / axisLabels.size();
        for (int i = 0 ; i < axisLabels.size() ; i++) {
            double x = GRAPH_INSETS.getLeft() + i * xLabelStep + xLabelStep;
            ctx.strokeLine(x, GRAPH_INSETS.getTop(), x, height - GRAPH_INSETS.getBottom());
            ctx.fillText(axisLabels.get(i), x, height - GRAPH_INSETS.getBottom() * 0.25);
        }

        // Draw points
        for (int i = 0 ; i < values.size() - 2 ; i++) {
            final double value     = values.get(i);
            final double nextValue = values.get(i + 1);
            final double x         = value * stepX;
            final double y         = (max - nextValue + min) * stepY;
            final Color fill       = Helper.getColorForValue(unit, UnitDefinition.MILLIGRAM_PER_DECILITER == unit ? value : Helper.mgPerDeciliterToMmolPerLiter(value));

            ctx.setFill(fill);
            ctx.fillOval(x - halfSymbolSize, y - halfSymbolSize, symbolSize, symbolSize);
        }
    }
}