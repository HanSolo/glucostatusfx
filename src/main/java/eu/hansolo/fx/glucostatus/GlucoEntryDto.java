package eu.hansolo.fx.glucostatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;


public record GlucoEntryDto(String _id, double sgv, long date, String dateString, int trend, String direction, String device, String type, int utcOffset, String sysTime) {
    public GlucoEntry getGlucoEntry() {
        return new GlucoEntry(this._id, this.sgv, this.date, OffsetDateTime.ofInstant(Instant.ofEpochSecond(this.date), ZoneId.systemDefault()), dateString, Trend.getFromText(Integer.toString(this.trend)), direction, device, type,
                              this.utcOffset, 0, 0, 0, 0, 0, this.sysTime);
    }
}
