package com.tsuyoshihayashi.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

/**
 * Object that represents record settings received from API
 *
 * @author Alexey Donov
 */
public final class RecordSettings {
    private static final String FILE_NAME_FORMAT_KEY = "record_name";
    private static final String LIMIT_KEY = "limit";
    private static final String UPLOAD_URL_KEY = "place";
    private static final String HASH_KEY = "hash";
    private static final String HASH2_KEY = "hash2";
    private static final String AUTO_RECORD_KEY = "manual_start";

    private final @NotNull String fileNameFormat;
    private final long limit;
    private final boolean autoRecord;
    private final @Nullable String uploadURL;
    private final @NotNull String hash;
    private final @NotNull String hash2;
    private final @NotNull String referer;

    public RecordSettings(@NotNull String fileNameFormat, long limit, boolean autoRecord, @Nullable String uploadURL, @NotNull String hash, @NotNull String hash2, @NotNull String referer) {
        super();

        this.fileNameFormat = fileNameFormat;
        this.limit = limit;
        this.autoRecord = autoRecord;
        this.uploadURL = uploadURL;
        this.hash = hash;
        this.hash2 = hash2;
        this.referer = referer;
    }

    /**
     * Create an instance from JSON response
     *
     * @param json JSON response from API
     * @param referer Referer domain name that made the request
     * @return Record settings from the JSON
     */
    public static RecordSettings fromJSON(@NotNull JSONObject json, @NotNull String referer) {
        if (!json.containsKey(FILE_NAME_FORMAT_KEY) ||
            !json.containsKey(LIMIT_KEY) ||
            !json.containsKey(HASH_KEY) ||
            !json.containsKey(HASH2_KEY)) {
            throw new IllegalArgumentException("JSON Object is not full");
        }

        boolean manualStart = false;
        try {
            manualStart = json.containsKey(AUTO_RECORD_KEY) && Integer.parseInt(json.get(AUTO_RECORD_KEY).toString()) == 1;
        } catch (NumberFormatException ignore) { }

        return new RecordSettings(json.get(FILE_NAME_FORMAT_KEY).toString(),
            Long.parseLong(json.get(LIMIT_KEY).toString()),
            !manualStart,
            json.containsKey(UPLOAD_URL_KEY) ? json.get(UPLOAD_URL_KEY).toString() : null,
            json.get(HASH_KEY).toString(),
            json.get(HASH2_KEY) != null ? json.get(HASH2_KEY).toString() : "", referer);
    }

    public @NotNull String getFileNameFormat() {
        return fileNameFormat;
    }

    public long getLimit() {
        return limit;
    }

    public boolean isAutoRecord() {
        return autoRecord;
    }

    public @Nullable String getUploadURL() {
        return uploadURL;
    }

    public @NotNull String getHash() {
        return hash;
    }

    public @NotNull String getHash2() {
        return hash2;
    }

    public @NotNull String getReferer() {
        return referer;
    }
}
