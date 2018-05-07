package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.api.RecordSettingsEndpoint;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.vhost.IVHost;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;

/**
 * Object that handles requests that control stream recording
 *
 * Start recording
 * http://hostname:1935/recordctrl?a=start&s=stream_name
 *
 * Stop recording
 * http://hostname:1936/recordctrl?a=stop&s=stream_name
 *
 * @author Alexey Donov
 */
public final class RecorderControl extends Control {
    private static final String STREAM_PARAMETER_NAME = "s";
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String TITLE_PARAMETER_NAME = "title";
    private static final String COMMENT_PARAMETER_NAME = "comment";
    private static final String TEXT_ACTION_PARAMETER_NAME = "act";

    private static final String API_ENDPOINT_KEY = "apiEndpoint";
    private static final String UPLOAD_REFERER_KEY = "uploadReferer";

    private final @NotNull WMSLogger logger = WMSLoggerFactory.getLogger(RecorderControl.class);

    private @Nullable RecordSettingsEndpoint recordSettingsEndpoint;

    private @NotNull RecordSettingsEndpoint getRecordSettingsEndpoint(IVHost host) {
        if (recordSettingsEndpoint == null) {
            val hostProperties = host.getProperties();
            val apiEndpoint = hostProperties.getPropertyStr(API_ENDPOINT_KEY);
            val uploadReferer = hostProperties.getPropertyStr(UPLOAD_REFERER_KEY);
            recordSettingsEndpoint = new RecordSettingsEndpoint(apiEndpoint, uploadReferer);
        }

        return recordSettingsEndpoint;
    }

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        logRequest(request, logger);

        // Ensure that this is a GET request
        if (!"GET".equals(request.getMethod())) {
            writeBadRequestResponse(response);
            return;
        }

        // Ensure that action parameter is present in the request
        val action = request.getParameter(ACTION_PARAMETER_NAME);
        if (action == null || action.isEmpty()) {
            writeBadRequestResponse(response);
            return;
        }

        // Ensure that stream name parameter is present in the request
        val streamName = request.getParameter(STREAM_PARAMETER_NAME);
        if (streamName == null || streamName.isEmpty()) {
            writeBadRequestResponse(response);
            return;
        }

        // Ensure that the live application instance is running
        val instance = host.getApplication("live").getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);
        if (instance == null) {
            logger.warn("No live application instance");
            writeResponse(response, 500, "No live application instance");
            return;
        }

        // Ensure that the stream with requested name is present in the application instance
        val stream = instance.getStreams().getStream(streamName);
        if (stream == null) {
            logger.warn(String.format("Stream %s not found", streamName));
            writeResponse(response, 404, "Stream not found");
            return;
        }

        switch (action) {
            case ACTION_START:
                // Get recent record settings info
                val title = request.getParameter(TITLE_PARAMETER_NAME);
                val comment = request.getParameter(COMMENT_PARAMETER_NAME);
                val textAction = request.getParameter(TEXT_ACTION_PARAMETER_NAME);

                val settings = getRecordSettingsEndpoint(host).getRecordSettings(stream, title, comment, textAction);
                val streamProperties = stream.getProperties();
                streamProperties.setProperty(RECORD_SETTINGS_KEY, settings);

                // Create stream recorder parameters from the settings
                val parameters = new StreamRecorderParameters(instance);
                parameters.fileFormat = IStreamRecorderConstants.FORMAT_MP4;
                parameters.segmentationType = IStreamRecorderConstants.SEGMENT_BY_DURATION;
                parameters.segmentDuration = settings.getLimit() * 60 * 1000;
                parameters.startOnKeyFrame = true;
                parameters.recordData = true;
                parameters.outputPath = instance.getStreamStoragePath();

                // Start the record
                logger.info(String.format("Starting record for stream %s", streamName));
                host.getLiveStreamRecordManager().startRecording(instance, streamName, parameters);
                break;

            case ACTION_STOP:
                // Stop the record
                logger.info(String.format("Stopping record for stream %s", streamName));
                host.getLiveStreamRecordManager().stopRecording(instance, streamName);
                break;

            default:
                logger.error(String.format("Unknown record command [%s]", action));
                break;
        }

        writeOkResponse(response);
    }
}
