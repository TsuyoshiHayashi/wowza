package com.tsuyoshihayashi.model;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.stream.Stream;

/**
 * @author Alexey Donov
 */
public final class RecordSettings {
    private static final String FILE_NAME_FORMAT_KEY = "record_name";
    private static final String LIMIT_KEY = "limit";
    private static final String UPLOAD_URL_KEY = "place";
    private static final String HASH_KEY = "hash";
    private static final String HASH2_KEY = "hash2";

    private final @NotNull String fileNameFormat;
    private final long limit;
    private final @NotNull String uploadURL;
    private final @NotNull String hash;
    private final @NotNull String hash2;
    private final @NotNull String referer;

    public RecordSettings(@NotNull String fileNameFormat, long limit, @NotNull String uploadURL, @NotNull String hash, @NotNull String hash2, @NotNull String referer) {
        super();

        this.fileNameFormat = fileNameFormat;
        this.limit = limit;
        this.uploadURL = uploadURL;
        this.hash = hash;
        this.hash2 = hash2;
        this.referer = referer;
    }

    public RecordSettings(@NotNull JSONObject json, @NotNull String referer) {
        this(json.get(FILE_NAME_FORMAT_KEY).toString(), Long.parseLong(json.get(LIMIT_KEY).toString()), json.get(UPLOAD_URL_KEY).toString(), json.get(HASH_KEY).toString(), json.get(HASH2_KEY).toString(), referer);
    }

    public @NotNull String getFileNameFormat() {
        return fileNameFormat;
    }

    public long getLimit() {
        return limit;
    }

    public @NotNull String getUploadURL() {
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
