package eu.hansolo.fx.glucostatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class GlucoseTrendPredictor {

    // -------------------------------------------------------------------------
    // Data Models
    // -------------------------------------------------------------------------

    public record GlucoseReading(Instant timestamp, double value) {}

    public enum GlucoTrend {
        RISING_RAPIDLY,  // > 2 mg/dL/min
        RISING,          // 1–2 mg/dL/min
        RISING_SLOWLY,   // 0.5–1 mg/dL/min
        STABLE,          // < 0.5 mg/dL/min change
        FALLING_SLOWLY,
        FALLING,
        FALLING_RAPIDLY
    }

    public sealed interface Warning permits Warning.PredictedLow, Warning.PredictedHigh, Warning.ApproachingLow, Warning.ApproachingHigh {
        record PredictedLow(double projectedValue)    implements Warning {}
        record PredictedHigh(double projectedValue)   implements Warning {}
        record ApproachingLow(double projectedValue)  implements Warning {}
        record ApproachingHigh(double projectedValue) implements Warning {}
    }

    public record GlucosePrediction(double projectedValue, GlucoTrend glucoTrend, Optional<Warning> warning, boolean isReliable) {}


    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    public record Config(
    int readingCount,           // Number of past readings to use
    double lambda,              // Exponential decay weight (0.5–0.9)
    double projectionMinutes,   // Minutes ahead to project
    double maxPhysioRatePerMin, // Physiological max rate mg/dL/min
    double warningMargin,       // Warn this many mg/dL before threshold
    double lowThreshold,        // Low glucose threshold (mg/dL)
    double highThreshold        // High glucose threshold (mg/dL)
    ) {
        public static Config defaults() {
            return new Config(
            8,     // 40 minutes of history
            0.7,   // exponential decay
            10.0,  // project 10 minutes ahead
            4.0,   // max 4 mg/dL/min physiologically
            15.0,  // warn 15 mg/dL before threshold
            70.0,  // low threshold
            180.0  // high threshold
            );
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final Config config;

    public GlucoseTrendPredictor() {
        this(Config.defaults());
    }

    public GlucoseTrendPredictor(Config config) {
        this.config = config;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Predict glucose value at +10 minutes given recent readings.
     * Readings must be sorted oldest → newest.
     *
     * @param readings CGM readings in chronological order
     * @return prediction, or empty if insufficient data
     */
    public Optional<GlucosePrediction> predict(final List<GlucoseReading> readings) {
        if (readings.size() < 3) return Optional.empty();

        List<GlucoseReading> recent = tail(readings, config.readingCount());
        Instant anchor = recent.getFirst().timestamp();

        // Convert to (minutes since oldest, value) pairs
        double[] xs = new double[recent.size()];
        double[] ys = new double[recent.size()];

        for (int i = 0; i < recent.size(); i++) {
            xs[i] = (recent.get(i).timestamp().getEpochSecond() - anchor.getEpochSecond()) / 60.0;
            ys[i] = recent.get(i).value();
        }

        double[] weights    = exponentialWeights(recent.size());
        double[] regression = weightedLinearRegression(xs, ys, weights);
        double   slope      = regression[0];
        double   intercept  = regression[1];

        // Project forward from the last reading
        double lastX          = xs[xs.length - 1];
        double targetX        = lastX + config.projectionMinutes();
        double projectedValue = slope * targetX + intercept;

        // Reliability check: is the rate of change physiologically plausible?
        boolean isReliable = Math.abs(slope) <= config.maxPhysioRatePerMin();

        GlucoTrend        glucoTrend = classifyTrend(slope);
        Optional<Warning> warning    = evaluateWarning(projectedValue);

        return Optional.of(new GlucosePrediction(projectedValue, glucoTrend, warning, isReliable));
    }

    // -------------------------------------------------------------------------
    // Weighted Linear Regression
    // -------------------------------------------------------------------------

    private double[] weightedLinearRegression(double[] xs, double[] ys, double[] weights) {
        double W   = 0;
        double Wx  = 0;
        double Wy  = 0;
        double Wxx = 0;
        double Wxy = 0;

        for (int i = 0; i < xs.length; i++) {
            W   += weights[i];
            Wx  += weights[i] * xs[i];
            Wy  += weights[i] * ys[i];
            Wxx += weights[i] * xs[i] * xs[i];
            Wxy += weights[i] * xs[i] * ys[i];
        }

        double denominator = W * Wxx - Wx * Wx;
        if (Math.abs(denominator) < 1e-10) {
            return new double[] { 0.0, Wy / W }; // Degenerate case: return mean as flat line
        }

        double slope     = (W * Wxy - Wx * Wy) / denominator;
        double intercept = (Wy - slope * Wx) / W;
        return new double[]{ slope, intercept };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Exponential weights: oldest reading gets lowest weight, normalized to sum = 1. */
    private double[] exponentialWeights(int count) {
        double[] raw = new double[count];
        double   sum = 0;
        for (int i = 0; i < count; i++) {
            raw[i] = Math.pow(config.lambda(), count - 1 - i);
            sum += raw[i];
        }
        for (int i = 0; i < count; i++) { raw[i] /= sum; }
        return raw;
    }

    /** Classify trend based on slope in mg/dL per minute. */
    private GlucoTrend classifyTrend(double slopePerMin) {
        if      (slopePerMin >=  2.0)  return GlucoTrend.RISING_RAPIDLY;
        else if (slopePerMin >=  1.0)  return GlucoTrend.RISING;
        else if (slopePerMin >=  0.5)  return GlucoTrend.RISING_SLOWLY;
        else if (slopePerMin > -0.5)   return GlucoTrend.STABLE;
        else if (slopePerMin > -1.0)   return GlucoTrend.FALLING_SLOWLY;
        else if (slopePerMin > -2.0)   return GlucoTrend.FALLING;
        else                           return GlucoTrend.FALLING_RAPIDLY;
    }

    private Optional<Warning> evaluateWarning(double projectedValue) {
        if (projectedValue < config.lowThreshold()) {
            return Optional.of(new Warning.PredictedLow(projectedValue));
        } else if (projectedValue < config.lowThreshold() + config.warningMargin()) {
            return Optional.of(new Warning.ApproachingLow(projectedValue));
        } else if (projectedValue > config.highThreshold()) {
            return Optional.of(new Warning.PredictedHigh(projectedValue));
        } else if (projectedValue > config.highThreshold() - config.warningMargin()) {
            return Optional.of(new Warning.ApproachingHigh(projectedValue));
        }
        return Optional.empty();
    }

    private <T> List<T> tail(List<T> list, int n) {
        return list.subList(Math.max(0, list.size() - n), list.size());
    }

    // -------------------------------------------------------------------------
    // Usage Example
    // -------------------------------------------------------------------------
    /*
    public static void main(String[] args) {
        var predictor = new GlucoseTrendPredictor();

        // Simulate 8 readings, 5 min apart, slowly rising
        var now = Instant.now();
        var readings = List.of(
        new GlucoseReading(now.minusSeconds(35 * 60), 110),
        new GlucoseReading(now.minusSeconds(30 * 60), 115),
        new GlucoseReading(now.minusSeconds(25 * 60), 118),
        new GlucoseReading(now.minusSeconds(20 * 60), 122),
        new GlucoseReading(now.minusSeconds(15 * 60), 128),
        new GlucoseReading(now.minusSeconds(10 * 60), 135),
        new GlucoseReading(now.minusSeconds( 5 * 60), 143),
        new GlucoseReading(now,                       152)
                              );

        predictor.predict(readings).ifPresent(p -> {
            if (!p.isReliable()) {
                System.out.println("Sensor noise detected — prediction suppressed");
                return;
            }
            System.out.printf("Projected glucose in 10 min: %d mg/dL%n", (int) p.projectedValue());
            System.out.println("Trend: " + p.glucoTrend());
            p.warning().ifPresent(w -> switch (w) {
                case Warning.PredictedLow  wl -> System.out.printf("⚠️  LOW predicted:     %d mg/dL%n", (int) wl.projectedValue());
                case Warning.PredictedHigh wh -> System.out.printf("⚠️  HIGH predicted:    %d mg/dL%n", (int) wh.projectedValue());
                case Warning.ApproachingLow  al -> System.out.printf("⚡ Approaching low:   %d mg/dL%n", (int) al.projectedValue());
                case Warning.ApproachingHigh ah -> System.out.printf("⚡ Approaching high:  %d mg/dL%n", (int) ah.projectedValue());
            });
        });
    }
    */
}
