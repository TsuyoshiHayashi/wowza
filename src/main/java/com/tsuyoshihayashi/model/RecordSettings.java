package com.tsuyoshihayashi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Stream;

/**
 * Object that represents record settings received from API
 *
 * @author Alexey Donov
 */
@Data
@AllArgsConstructor
public final class RecordSettings {
    private static final String FILE_NAME_FORMAT_KEY = "record_name";
    private static final String LIMIT_KEY = "limit";
    private static final String UPLOAD_URL_KEY = "place";
    private static final String HASH_KEY = "hash";
    private static final String HASH2_KEY = "hash2";
    private static final String AUTO_RECORD_KEY = "manual_start";
    private static final String TITLE_KEY = "title";
    private static final String COMMENT_KEY = "comment";
    private static final String ACTION_KEY = "act";

    private final @NotNull String fileNameFormat;
    private final long limit;
    private final boolean autoRecord;
    private final @Nullable String uploadURL;
    private final @NotNull String hash;
    private final @NotNull String hash2;
    private final @NotNull String referer;
    private final @Nullable String title;
    private final @Nullable String comment;
    private final @Nullable String action;

    /**
     * Create an instance from JSON response
     *
     * @param json JSON response from API
     * @param referer Referer domain name that made the request
     * @return Record settings from the JSON
     */
    public static @NotNull RecordSettings fromJSON(@NotNull JSONObject json, @NotNull String referer) {
        if (Stream.of(FILE_NAME_FORMAT_KEY, LIMIT_KEY, HASH_KEY, HASH2_KEY)
            .map(json::get)
            .anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("JSON Object is not full");
        }

        boolean manualStart = false;
        try {
            manualStart = json.containsKey(AUTO_RECORD_KEY) && Integer.parseInt(json.get(AUTO_RECORD_KEY).toString()) == 1;
        } catch (NumberFormatException ignore) {
            // No op
        }

        return new RecordSettings(json.get(FILE_NAME_FORMAT_KEY).toString(),
            Long.parseLong(json.get(LIMIT_KEY).toString()),
            !manualStart,
            Optional.ofNullable(json.get(UPLOAD_URL_KEY)).map(Object::toString).orElse(null),
            json.get(HASH_KEY).toString(),
            json.get(HASH2_KEY).toString(),
            referer,
            Optional.ofNullable(json.get(TITLE_KEY)).map(Object::toString).orElse(null),
            Optional.ofNullable(json.get(COMMENT_KEY)).map(Object::toString).orElse(null),
            Optional.ofNullable(json.get(ACTION_KEY)).map(Object::toString).orElse(null));
    }
}
