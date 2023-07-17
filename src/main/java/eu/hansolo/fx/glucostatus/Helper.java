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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import eu.hansolo.fx.glucostatus.Records.DataPoint;
import eu.hansolo.fx.glucostatus.Records.GlucoEntry;
import eu.hansolo.fx.glucostatus.Records.GlucoEntryDto;
import eu.hansolo.fx.glucostatus.Statistics.StatisticCalculation;
import eu.hansolo.fx.glucostatus.Statistics.StatisticRange;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.toolbox.tuples.Pair;
import eu.hansolo.toolbox.unit.Converter;
import eu.hansolo.toolbox.unit.UnitDefinition;
import eu.hansolo.toolboxfx.HelperFX;
import eu.hansolo.toolboxfx.geom.Point;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.TextAlignment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static eu.hansolo.fx.glucostatus.Constants.FIELD_DATE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DATE_STRING;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DELTA;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DEVICE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_DIRECTION;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_FILTERED;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_ID;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_NOISE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_RSSI;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_SGV;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_SYS_TIME;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_TREND;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_TYPE;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_UNFILTERED;
import static eu.hansolo.fx.glucostatus.Constants.FIELD_UTC_OFFSET;
import static eu.hansolo.toolbox.unit.Category.BLOOD_GLUCOSE;
import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIGRAM_PER_DECILITER;
import static eu.hansolo.toolbox.unit.UnitDefinition.MILLIMOL_PER_LITER;


public class Helper {
    private static final Converter  MGDL_CONVERTER = new Converter(BLOOD_GLUCOSE, MILLIGRAM_PER_DECILITER);
    private static final Converter  MMOL_CONVERTER = new Converter(BLOOD_GLUCOSE, MILLIMOL_PER_LITER);
    private static       HttpClient httpClient;
    private static       HttpClient httpClientAsync;

    public static final List<GlucoEntry> getEntries(final String jsonText) {
        final List<GlucoEntry> entries = new ArrayList<>();
        try(JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            Gson gson = new GsonBuilder().create();
            jsonReader.beginArray();
            while (jsonReader.hasNext()){
                GlucoEntryDto dto = gson.fromJson(jsonReader, GlucoEntryDto.class);
                if (null != dto) { entries.add(dto.getGlucoEntry()); }
            }
            jsonReader.endArray();
        }  catch (IOException e) {}
        return entries;
    }

    public static final List<GlucoEntry> getGlucoEntries(final String jsonText) {
        List<GlucoEntry> entries = new ArrayList<>();
        if (null == jsonText || jsonText.isEmpty()) { throw new IllegalArgumentException("Json text cannot be null or empty"); }
        final Gson      gson      = new Gson();
        final JsonArray jsonArray = gson.fromJson(jsonText, JsonArray.class);
        for (JsonElement jsonElement : jsonArray) {
            JsonObject     json       = jsonElement.getAsJsonObject();
            String         id         = json.get(FIELD_ID).getAsString();
            double         sgv        = json.has(FIELD_SGV)         ? json.get(FIELD_SGV).getAsDouble()                      : 0;
            long           datelong   = json.has(FIELD_DATE)        ? json.get(FIELD_DATE).getAsLong() / 1000                : 0;
            OffsetDateTime date       = OffsetDateTime.ofInstant(Instant.ofEpochSecond(datelong), ZoneId.systemDefault());
            String         dateString = json.has(FIELD_DATE_STRING) ? json.get(FIELD_DATE_STRING).getAsString()              : "";
            Trend          trend      = json.has(FIELD_TREND)       ? Trend.getFromText(json.get(FIELD_TREND).getAsString()) : Trend.NONE;
            String         direction  = json.has(FIELD_DIRECTION)   ? json.get(FIELD_DIRECTION).getAsString()                : "";
            String         device     = json.has(FIELD_DEVICE)      ? json.get(FIELD_DEVICE).getAsString()                   : "";
            String         type       = json.has(FIELD_TYPE)        ? json.get(FIELD_TYPE).getAsString()                     : "";
            int            utcOffset  = json.has(FIELD_UTC_OFFSET)  ? json.get(FIELD_UTC_OFFSET).getAsInt()                  : 0;
            int            noise      = json.has(FIELD_NOISE)       ? json.get(FIELD_NOISE).getAsInt()                       : 0;
            double         filtered   = json.has(FIELD_FILTERED)    ? json.get(FIELD_FILTERED).getAsDouble()                 : 0;
            double         unfiltered = json.has(FIELD_UNFILTERED)  ? json.get(FIELD_UNFILTERED).getAsDouble()               : 0;
            int            rssi       = json.has(FIELD_RSSI)        ? json.get(FIELD_RSSI).getAsInt()                        : 0;
            double         delta      = json.has(FIELD_DELTA)       ? json.get(FIELD_DELTA).getAsDouble()                    : 0;
            String         sysTime    = json.has(FIELD_SYS_TIME)    ? json.get(FIELD_SYS_TIME).getAsString()                 : "";
            entries.add(new GlucoEntry(id, sgv, datelong, date, dateString, trend, direction, device, type, utcOffset, noise, filtered, unfiltered, rssi, delta, sysTime));
        }
        return entries;
    }

    public static final Color getColorForValue(final UnitDefinition unit, final double value) {
        return Status.getByValue(unit, value).getColor();
    }

    public static final Color getColorForValue2(final UnitDefinition unit, final double value) { return Status.getByValue(unit, value).getColor2(); }

    public static final CompletableFuture<List<GlucoEntry>> getEntriesFromInterval(final Interval interval, final String nightscoutUrl) {
        final long   now  = Instant.now().getEpochSecond();
        final long   from = (now - interval.getSeconds()) * 1000;
        final long   to   = now * 1000;
        final String url  = nightscoutUrl + "?find[date][$gte]=" + from + "&find[date][$lte]=" + to + "&count=" + interval.getNoOfEntries();
        CompletableFuture<List<GlucoEntry>> cf = getAsync(url).thenApply(r -> {
            if (null == r || null == r.body() || r.body().isEmpty()) {
                return new ArrayList<>();
            } else {
                return getGlucoEntries(r.body());
            }
        });
        return cf;
    }

    public static final double mmolPerLiterToMgPerDeciliter(final double mmolPerLiter) {
        return MMOL_CONVERTER.convert(mmolPerLiter, MILLIGRAM_PER_DECILITER);
    }

    public static final double mgPerDeciliterToMmolPerLiter(final double mgPerDeciliter) {
        return MGDL_CONVERTER.convert(mgPerDeciliter, MILLIMOL_PER_LITER);
    }

    public static final int getHourFromEpochSeconds(final long epochSeconds) {
        return getZonedDateTimeFromEpochSeconds(epochSeconds).getHour();
    }

    public static final ZonedDateTime getZonedDateTimeFromEpochSeconds(final long epochSeconds) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    public static final double calcHbA1c(final List<GlucoEntry> entries) {
        double average = 0;
        double hba1c   = 0;
        if (!entries.isEmpty()) {
            average = entries.stream().map(entry -> entry.sgv()).reduce(0.0, Double::sum).doubleValue() / entries.size();
            hba1c   = (average + 100) / 36.66666;
        }
        return hba1c;
    }

    public static final Pair<List<Point>, List<Point>> createValueRangePath(final Map<LocalTime, DataPoint> dataMap, final StatisticRange range, final boolean smoothed) {
        List<LocalTime> sortedKeys         = dataMap.keySet().stream().sorted().collect(Collectors.toList());
        List<LocalTime> sortedKeysReversed = dataMap.keySet().stream().sorted(Collections.reverseOrder()).collect(Collectors.toList());
        List<Point>     minPoints          = new ArrayList<>();
        List<Point>     maxPoints          = new ArrayList<>();
        switch(range) {
            case MIN_MAX -> {
                // Collect all max values
                for (LocalTime key : sortedKeys) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).maxValue();
                    maxPoints.add(new Point(x, y));
                }
                // Collect all min values
                for (LocalTime key : sortedKeysReversed) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).minValue();
                    minPoints.add(new Point(x, y));
                }
            }
            case TEN_TO_NINETY -> {
                // Collect all max values
                for (LocalTime key : sortedKeys) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).percentile90();
                    maxPoints.add(new Point(x, y));
                }
                // Collect all min values
                for (LocalTime key : sortedKeysReversed) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).percentile10();
                    minPoints.add(new Point(x, y));
                }
            }
            case TWENTY_FIVE_TO_SEVENTY_FIVE -> {
                // Collect all max values
                for (LocalTime key : sortedKeys) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).percentile75();
                    maxPoints.add(new Point(x, y));
                }
                // Collect all min values
                for (LocalTime key : sortedKeysReversed) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).percentile25();
                    minPoints.add(new Point(x, y));
                }
            }
        }
        if (smoothed) {
            maxPoints = HelperFX.subdividePoints(maxPoints, 3);
            minPoints = HelperFX.subdividePoints(minPoints, 3);
        }
        return new Pair<>(maxPoints, minPoints);
    }

    public static final List<Point> createAveragePath(final Map<LocalTime, DataPoint> dataMap, final StatisticCalculation calculation, final boolean smoothed) {
        List<LocalTime> sortedKeys = dataMap.keySet().stream().sorted().collect(Collectors.toList());
        List<Point>     avgPoints  = new LinkedList<>();
        switch(calculation) {
            case AVERAGE -> {
                for (LocalTime key : sortedKeys) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).avgValue();
                    avgPoints.add(new Point(x, y));
                }
            }
            case MEDIAN -> {
                for (LocalTime key : sortedKeys) {
                    double x = key.getHour() * 60.0 + key.getMinute();
                    double y = dataMap.get(key).median();
                    avgPoints.add(new Point(x, y));
                }
            }
        }
        if (smoothed) {
            avgPoints = HelperFX.subdividePoints(avgPoints, 3);
        }
        return avgPoints;
    }

    public static final BufferedImage createTextTrayIcon(final OperatingSystem operatingSystem, final String text, final Color color) {
        final int    width    = 44;
        final int    height   = 22;
        final double x        = width / 2;
        final double y        = height / 2;
        final double fontSize = OperatingSystem.MACOS != operatingSystem ? 8 : 0.6363636364 * height;

        final Canvas          canvas = new Canvas(width, height);
        final GraphicsContext ctx    = canvas.getGraphicsContext2D();
        ctx.setFill(OperatingSystem.LINUX == operatingSystem ? Color.rgb(19, 19, 19) : Color.TRANSPARENT);
        ctx.fillRect(0, 0, width, height);
        ctx.setFont(Fonts.sfProRoundedSemiBold(fontSize));
        ctx.setTextAlign(OperatingSystem.MACOS != operatingSystem ? TextAlignment.RIGHT : TextAlignment.CENTER);
        ctx.setTextBaseline(VPos.CENTER);
        ctx.setFontSmoothingType(FontSmoothingType.LCD);
        ctx.setFill(color);
        ctx.fillText(text, x, y, width);

        final WritableImage img = new WritableImage(width, height);
        final SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        canvas.snapshot(parameters, img);

        return SwingFXUtils.fromFXImage(img, null);
    }

    public static final Image createTextTrayIconFX(final int width, final int height, final String text, final Color color) {
        final double fontSize = height * 0.85;
        final double x        = width  / 2;
        final double y        = height / 2;

        final Canvas          canvas = new Canvas(width, height);
        final GraphicsContext ctx    = canvas.getGraphicsContext2D();
        ctx.setFont(Fonts.sfProRoundedBold(fontSize));
        ctx.setTextAlign(TextAlignment.CENTER);
        ctx.setTextBaseline(VPos.CENTER);
        ctx.setFontSmoothingType(FontSmoothingType.GRAY);
        ctx.setFill(color);
        ctx.fillText(text, x, y);//, width);

        final WritableImage img = new WritableImage(width, height);
        final SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        canvas.snapshot(parameters, img);

        return img;
    }

    public static final void saveImage(final Image img, final String filename) {
        final BufferedImage bImg = SwingFXUtils.fromFXImage(img, null);
        saveImage(bImg, filename);
    }
    public static final void saveImage(final BufferedImage img, final String filename) {
        final File outputFile = new File(filename);
        try {
            ImageIO.write(img, "png", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final CompletableFuture<HttpResponse<String>> checkForUpdateAsync() {
        return getAsync(Constants.RELEASES_URI);
    }


    // ******************** REST calls ****************************************
    private static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                         .connectTimeout(Duration.ofSeconds(20))
                         .version(Version.HTTP_2)
                         .followRedirects(Redirect.NORMAL)
                         //.executor(Executors.newFixedThreadPool(4))
                         .build();
    }

    public static final HttpResponse<String> get(final String uri, final String token) {
        if (null == httpClient) { httpClient = createHttpClient(); }

        HttpRequest request = HttpRequest.newBuilder()
                                         .GET()
                                         .uri(URI.create(uri))
                                         .setHeader("Accept", "application/json")
                                         .setHeader("User-Agent", "GlucoStatusFX")
                                         .setHeader("API-SECRET", token)
                                         .timeout(Duration.ofSeconds(60))
                                         .build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response;
            } else {
                // Problem with url request
                return response;
            }
        } catch (CompletionException | InterruptedException | IOException e) {
            return null;
        }
    }

    public static final CompletableFuture<HttpResponse<String>> getAsync(final String uri) {
        if (null == httpClientAsync) { httpClientAsync = createHttpClient(); }

        final HttpRequest request = HttpRequest.newBuilder()
                                               .GET()
                                               .uri(URI.create(uri))
                                               .setHeader("Accept", "application/json")
                                               .setHeader("User-Agent", "GlucoStatusFX")
                                               .timeout(Duration.ofSeconds(60))
                                               .build();
        return httpClientAsync.sendAsync(request, BodyHandlers.ofString());
    }

}
