package eu.hansolo.fx.glucostatus;

public record DataPoint(double minValue, double maxValue, double avgValue, double percentile10, double percentile25, double percentile75, double percentile90, double median) { }
