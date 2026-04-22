package com.aiimoment.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BackendApiClient {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8000";

    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;

    public BackendApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.gson = new Gson();
        this.baseUrl = resolveBaseUrl();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public CompletableFuture<List<AssetSummary>> fetchAssets() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/assets"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return send(request, AssetsEnvelope.class)
                .thenApply(response -> response != null && response.data != null ? response.data : Collections.emptyList());
    }

    public CompletableFuture<SearchPayload> search(String assetId, String query) {
        String body = gson.toJson(new SearchRequest(assetId, query));
        String requestUrl = baseUrl
                + "/api/search?asset_id=" + encode(assetId)
                + "&query=" + encode(query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return send(request, SearchEnvelope.class)
                .thenApply(response -> response != null ? response.data : null);
    }

    public CompletableFuture<UploadPayload> uploadMedia(File file) {
        HttpRequest request;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/media/upload?filename=" + encode(file.getName())))
                    .timeout(Duration.ofMinutes(5))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
        } catch (FileNotFoundException exc) {
            return CompletableFuture.failedFuture(exc);
        } catch (IOException exc) {
            return CompletableFuture.failedFuture(exc);
        }
        return send(request, UploadEnvelope.class)
                .thenApply(response -> response != null ? response.data : null);
    }

    public CompletableFuture<EditPayload> editMedia(
            String mediaId,
            double start,
            double end,
            double speed,
            double effectIntensity,
            String demand,
            boolean addVoiceover
    ) {
        EditRequest payload = new EditRequest(
                mediaId,
                Collections.singletonList(new EditSegment(start, end)),
                new EditOptions(speed, effectIntensity, demand, addVoiceover)
        );
        String body = gson.toJson(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/edit"))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return send(request, EditEnvelope.class)
                .thenApply(response -> response != null ? response.data : null);
    }

    public String resolveUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return baseUrl;
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        if (pathOrUrl.startsWith("/")) {
            return baseUrl + pathOrUrl;
        }
        return baseUrl + "/" + pathOrUrl;
    }

    private <T> CompletableFuture<T> send(HttpRequest request, Class<T> responseType) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IOException(extractErrorMessage(response.body(), response.statusCode())));
                    }
                    return gson.fromJson(response.body(), responseType);
                });
    }

    private static String resolveBaseUrl() {
        String fromProperty = System.getProperty("alimoment.apiBaseUrl");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return trimTrailingSlash(fromProperty);
        }
        String fromEnv = System.getenv("ALIMOMENT_API_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return trimTrailingSlash(fromEnv);
        }
        return DEFAULT_BASE_URL;
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String extractErrorMessage(String body, int statusCode) {
        if (body == null || body.isBlank()) {
            return "后端请求失败，HTTP " + statusCode;
        }
        try {
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            if (object.has("detail")) {
                return object.get("detail").getAsString();
            }
            if (object.has("message")) {
                return object.get("message").getAsString();
            }
        } catch (Exception ignored) {
            // Fall back to the raw body below.
        }
        return "后端请求失败，HTTP " + statusCode + "：" + body;
    }

    public static String describeError(Throwable error) {
        if (error == null) {
            return "未知错误";
        }
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        return message != null && !message.isBlank() ? message : "未知错误";
    }

    private static final class SearchRequest {
        @SerializedName("asset_id")
        private final String assetId;
        private final String query;

        private SearchRequest(String assetId, String query) {
            this.assetId = assetId;
            this.query = query;
        }
    }

    private static final class AssetsEnvelope {
        private String status;
        private List<AssetSummary> data;
    }

    private static final class UploadEnvelope {
        private String status;
        private UploadPayload data;
    }

    private static final class SearchEnvelope {
        private String status;
        private SearchPayload data;
    }

    private static final class EditEnvelope {
        private String status;
        private EditPayload data;
    }

    private static final class EditRequest {
        @SerializedName("media_id")
        private final String mediaId;
        private final List<EditSegment> segments;
        private final EditOptions options;

        private EditRequest(String mediaId, List<EditSegment> segments, EditOptions options) {
            this.mediaId = mediaId;
            this.segments = segments;
            this.options = options;
        }
    }

    private static final class EditSegment {
        private final double start;
        private final double end;

        private EditSegment(double start, double end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class EditOptions {
        private final double speed;
        @SerializedName("effect_intensity")
        private final double effectIntensity;
        private final String demand;
        @SerializedName("add_voiceover")
        private final boolean addVoiceover;

        private EditOptions(double speed, double effectIntensity, String demand, boolean addVoiceover) {
            this.speed = speed;
            this.effectIntensity = effectIntensity;
            this.demand = demand;
            this.addVoiceover = addVoiceover;
        }
    }

    public static final class AssetSummary {
        @SerializedName("asset_id")
        public String assetId;
        public String title;
        public double duration;
        public String status;
        public String dataset;
        @SerializedName("thumbnail_url")
        public String thumbnailUrl;
        @SerializedName("suggested_queries")
        public List<String> suggestedQueries;

        @Override
        public String toString() {
            if (title == null || title.isBlank()) {
                return assetId != null ? assetId : "未命名素材";
            }
            return title;
        }
    }

    public static final class SearchPayload {
        @SerializedName("asset_id")
        public String assetId;
        @SerializedName("source_video_id")
        public String sourceVideoId;
        @SerializedName("original_query")
        public String originalQuery;
        @SerializedName("translated_query")
        public String translatedQuery;
        @SerializedName("normalization_mode")
        public String normalizationMode;
        public List<SearchResultItem> results;
        public SearchDebug debug;
    }

    public static final class SearchResultItem {
        @SerializedName("start_time")
        public double startTime;
        @SerializedName("end_time")
        public double endTime;
        public double score;
        public int rank;
    }

    public static final class SearchDebug {
        public String dataset;
        @SerializedName("asset_status")
        public String assetStatus;
        @SerializedName("query_normalizer_mode")
        public String queryNormalizerMode;
    }

    public static final class UploadPayload {
        @SerializedName("media_id")
        public String mediaId;
        public String filename;
        @SerializedName("original_filename")
        public String originalFilename;
        @SerializedName("file_size")
        public long fileSize;
        @SerializedName("preview_filename")
        public String previewFilename;
        @SerializedName("preview_relative_url")
        public String previewRelativeUrl;
        @SerializedName("preview_url")
        public String previewUrl;
        @SerializedName("preview_note")
        public String previewNote;
    }

    public static final class EditPayload {
        @SerializedName("media_id")
        public String mediaId;
        @SerializedName("output_filename")
        public String outputFilename;
        @SerializedName("output_relative_url")
        public String outputRelativeUrl;
        @SerializedName("output_url")
        public String outputUrl;
        public List<EditResultSegment> segments;
        public double speed;
        @SerializedName("effect_intensity")
        public double effectIntensity;
        @SerializedName("voiceover_requested")
        public boolean voiceoverRequested;
        @SerializedName("voiceover_applied")
        public boolean voiceoverApplied;
        public String note;
    }

    public static final class EditResultSegment {
        public double start;
        public double end;
    }
}
