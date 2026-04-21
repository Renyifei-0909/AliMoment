package com.aiimoment.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private static final class SearchEnvelope {
        private String status;
        private SearchPayload data;
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
}
