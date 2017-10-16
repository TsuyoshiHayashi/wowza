package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.http.HTTPProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;

import java.io.IOException;
import java.util.Arrays;

import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;

/**
 * @author Alexey Donov
 */
public class RecorderControl extends HTTPProvider2Base {
    private static final String STREAM_PARAMETER_NAME = "s";
    private static final String ACTION_PARAMETER_NAME = "a";
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(RecorderControl.class);

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        if (!"GET".equals(request.getMethod())) {
            return;
        }

        final String action = request.getParameter(ACTION_PARAMETER_NAME);
        if (action == null || !Arrays.asList(ACTION_START, ACTION_STOP).contains(action)) {
            return;
        }

        final String streamName = request.getParameter(STREAM_PARAMETER_NAME);
        if (streamName == null || streamName.isEmpty()) {
            return;
        }

        final IApplicationInstance instance = host.getApplication("live").getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);
        if (instance == null) {
            logger.warn("No live application instance");
            return;
        }

        final IMediaStream stream = instance.getStreams().getStream(streamName);
        if (stream == null) {
            logger.warn(String.format("Stream %s not found", streamName));
            return;
        }

        final WMSProperties properties = stream.getProperties();
        final RecordSettings settings = (RecordSettings) properties.getProperty(RECORD_SETTINGS_KEY);
        if (settings == null) {
            logger.warn(String.format("No record settings for %s", streamName));
            return;
        }

        switch (action) {
            case ACTION_START:
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
                host.getLiveStreamRecordManager().stopRecording(instance, streamName);
                break;
        }

        response.setResponseCode(200);
        response.setHeader("Content-Type", "application/json");
        try {
            response.getOutputStream().write("{ok: true}".getBytes());
            response.getOutputStream().close();
        } catch (IOException ignore) { }
    }
}
