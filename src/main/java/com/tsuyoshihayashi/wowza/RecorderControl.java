package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.vhost.IVHost;
import lombok.val;

import java.util.Arrays;

import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;

/**
 * Object that handles requests that control stream recording
 *
 * @author Alexey Donov
 */
public final class RecorderControl extends Control {
    private static final String STREAM_PARAMETER_NAME = "s";
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(RecorderControl.class);

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        // Ensure that this is a GET request
        if (!"GET".equals(request.getMethod())) {
            writeBadRequestResponse(response);
            return;
        }

        // Ensure that action parameter is present in the request
        val action = request.getParameter(ACTION_PARAMETER_NAME);
        if (action == null || !Arrays.asList(ACTION_START, ACTION_STOP).contains(action)) {
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

        // Ensure that the stream has record settings information
        val properties = stream.getProperties();
        val settings = (RecordSettings) properties.getProperty(RECORD_SETTINGS_KEY);
        if (settings == null) {
            logger.warn(String.format("No record settings for %s", streamName));
            writeResponse(response, 404, "No record settings for stream");
            return;
        }

        switch (action) {
            case ACTION_START:
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
