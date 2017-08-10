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

    private final @NotNull JSONObject json;

    public RecordSettings(@NotNull JSONObject json) {
        super();

        if (!Stream.of(FILE_NAME_FORMAT_KEY, LIMIT_KEY, UPLOAD_URL_KEY, HASH_KEY, HASH2_KEY).parallel().allMatch(json::containsKey)) {
            throw new RuntimeException("Missing key(s)");
        }

        this.json = json;
    }

    public String getFileNameFormat() {
        return json.get(FILE_NAME_FORMAT_KEY).toString();
    }

    public long getLimit() {
        return Long.parseLong(json.get(LIMIT_KEY).toString());
    }

    public String getUploadURL() {
        return json.get(UPLOAD_URL_KEY).toString();
    }

    public String getHash() {
        return json.get(HASH_KEY).toString();
    }

    public String getHash2() {
        return json.get(HASH2_KEY).toString();
    }
}
