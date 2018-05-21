package com.tsuyoshihayashi.api;

import com.tsuyoshihayashi.model.AliasProvider;
import com.tsuyoshihayashi.model.CameraInfo;
import com.tsuyoshihayashi.model.RecordSettings;
import com.tsuyoshihayashi.model.TextAction;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.ws.rs.client.WebTarget;

import java.util.Optional;

import static com.tsuyoshihayashi.model.RecordSettings.fromJSON;

/**
 * Record settings API endpoint
 *
 * @author Alexey Donov
 */
public final class RecordSettingsEndpoint extends Endpoint {
    private static final String API_STREAM_NAME_PARAMETER_NAME = "n";
    private static final String API_TITLE_PARAMETER_NAME = "title";
    private static final String API_COMMENT_PARAMETER_NAME = "comment";
    private static final String API_ACTION_PARAMETER_NAME = "act";

    private final @NotNull WMSLogger logger = WMSLoggerFactory.getLogger(RecordSettingsEndpoint.class);

    private final @NotNull String endpoint;
    private final @NotNull String referer;

    public RecordSettingsEndpoint(@NotNull String endpoint, @NotNull String referer) {
        this.endpoint = endpoint;
        this.referer = referer;
    }

    public @NotNull RecordSettings getRecordSettings(@NotNull IMediaStream stream, @Nullable String title, @Nullable String comment, @Nullable String textAction) {
        try {
            WebTarget target = client.target(endpoint)
                .queryParam(API_STREAM_NAME_PARAMETER_NAME, stream.getName());

            if (textAction != null) {
                target = target.queryParam(API_ACTION_PARAMETER_NAME, textAction);
            }
            if (title != null) {
                target = target.queryParam(API_TITLE_PARAMETER_NAME, title);
            }
            if (comment != null) {
                target = target.queryParam(API_COMMENT_PARAMETER_NAME, comment);
            }

            logger.info(String.format("API Request URL: %s", target.getUri()));

            val responseText = target.request().get(String.class);

            logger.info(String.format("API Response: %s", responseText));

            val response = (JSONObject) parser.parse(responseText);
            val settings = fromJSON(response, referer);

            logger.info(String.format("Record settings: %s", settings));

            return settings;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ask API to return record settings for the stream
     *
     * @param stream Stream object
     * @return Record settings object
     */
    public @NotNull RecordSettings getRecordSettings(@NotNull IMediaStream stream) {
        val cameraInfo = AliasProvider.instance().getCameraInfo(stream.getName());

        val textAction = Optional.ofNullable(cameraInfo)
            .map(CameraInfo::getTextAction)
            .map(TextAction::toString)
            .map(String::toLowerCase)
            .orElse(null);

        val title = Optional.ofNullable(cameraInfo)
            .map(CameraInfo::getTitle)
            .orElse(null);

        val comment = Optional.ofNullable(cameraInfo)
            .map(CameraInfo::getComment)
            .orElse(null);

        return getRecordSettings(stream, title, comment, textAction);
    }
}
