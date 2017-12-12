package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;

import java.util.Arrays;

import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;

/**
 * @author Alexey Donov
 */
public class RecorderControl extends Control {
    private static final String STREAM_PARAMETER_NAME = "s";
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(RecorderControl.class);

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        if (!"GET".equals(request.getMethod())) {
            writeBadRequestResponse(response);
            return;
        }

        final String action = request.getParameter(ACTION_PARAMETER_NAME);
        if (action == null || !Arrays.asList(ACTION_START, ACTION_STOP).contains(action)) {
            writeBadRequestResponse(response);
            return;
        }

        final String streamName = request.getParameter(STREAM_PARAMETER_NAME);
        if (streamName == null || streamName.isEmpty()) {
            writeBadRequestResponse(response);
            return;
        }

        final IApplicationInstance instance = host.getApplication("live").getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);
        if (instance == null) {
            logger.warn("No live application instance");
            writeResponse(response, 500, "No live application instance");
            return;
        }

        final IMediaStream stream = instance.getStreams().getStream(streamName);
        if (stream == null) {
            logger.warn(String.format("Stream %s not found", streamName));
            writeResponse(response, 404, "Stream not found");
            return;
        }

        final WMSProperties properties = stream.getProperties();
        final RecordSettings settings = (RecordSettings) properties.getProperty(RECORD_SETTINGS_KEY);
        if (settings == null) {
            logger.warn(String.format("No record settings for %s", streamName));
            writeResponse(response, 404, "No record settings for stream");
            return;
        }

        switch (action) {
            case ACTION_START:
                logger.info(String.format("Starting record for stream %s", streamName));
                final StreamRecorderParameters parameters = new StreamRecorderParameters(instance);
                parameters.fileFormat = IStreamRecorderConstants.FORMAT_MP4;
                parameters.segmentationType = IStreamRecorderConstants.SEGMENT_BY_DURATION;
                parameters.segmentDuration = settings.getLimit() * 60 * 1000;
                parameters.startOnKeyFrame = true;
                parameters.recordData = true;
                parameters.outputPath = instance.getStreamStoragePath();

                host.getLiveStreamRecordManager().startRecording(instance, streamName, parameters);
                break;

            case ACTION_STOP:
                logger.info(String.format("Stopping record for stream %s", streamName));
                host.getLiveStreamRecordManager().stopRecording(instance, streamName);
                break;

            default:
                logger.error(String.format("Unknown record command [%s]", action));
                break;
        }

        writeResponse(response, 200, "{\"ok\": true}", "application/json");
    }
}
