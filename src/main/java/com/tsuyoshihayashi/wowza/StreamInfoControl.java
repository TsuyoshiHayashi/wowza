package com.tsuyoshihayashi.wowza;

import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.Optional;

import static java.util.Objects.isNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Object that handles requests about stream bitrate information
 *
 * Get stream information
 * http://hostname:1935/stream_info?s=stream_name
 *
 * @author Alexey Donov
 */
public final class StreamInfoControl extends Control {
    private static final String STREAM_NAME_PARAMETER_NAME = "s";

    private static final String TOTAL_BITRATE = "total_bitrate";
    private static final String VIDEO_BITRATE = "video_bitrate";
    private static final String AUDIO_BITRATE = "audio_bitrate";
    private static final String IS_RECORDING = "is_recording";
    private static final String RECORDER_STATE = "recorder_state";
    private static final String NOT_RECORDING_RECORDER_STATE = "Not recording";

    private final @NotNull WMSLogger logger = WMSLoggerFactory.getLogger(StreamInfoControl.class);

    /**
     * Create a JSON entry about the stream, containing
     * video, audio and total bitrates.
     *
     * @param stream Stream
     * @return JSON object
     */
    @SuppressWarnings("unchecked")
    private @NotNull JSONObject streamInfo(@NotNull IMediaStream stream, @NotNull IVHost host) {
        val obj = new JSONObject();
        obj.put(TOTAL_BITRATE, stream.getPublishBitrateAudio() + stream.getPublishBitrateVideo());
        obj.put(VIDEO_BITRATE, stream.getPublishBitrateVideo());
        obj.put(AUDIO_BITRATE, stream.getPublishBitrateAudio());

        // check recording status and state
        IApplicationInstance instance = host.getApplication("live").getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);
        IStreamRecorder recorder = host.getLiveStreamRecordManager().getRecorder(instance, stream.getName());
        boolean status = recorder != null && recorder.getRecorderState() == 2;
        obj.put(IS_RECORDING, status);
        String state = recorder != null ? recorder.getRecorderStateString() : NOT_RECORDING_RECORDER_STATE;
        obj.put(RECORDER_STATE, state);

        return obj;
    }

    /**
     * Create an empty JSON
     *
     * @return JSON object
     */
    @SuppressWarnings("unchecked")
    private @NotNull JSONObject emptyStreamInfo() {
        val obj = new JSONObject();
        obj.put(TOTAL_BITRATE, false);
        obj.put(VIDEO_BITRATE, false);
        obj.put(AUDIO_BITRATE, false);
        obj.put(IS_RECORDING, false);
        obj.put(RECORDER_STATE, NOT_RECORDING_RECORDER_STATE);

        return obj;
    }

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        logRequest(request, logger);

        try {
            // Ensure that this is a GET request
            if (!"GET".equals(request.getMethod())) {
                writeBadRequestResponse(response);
                return;
            }

            // Ensure that stream name parameter is present in the request
            val streamName = request.getParameter(STREAM_NAME_PARAMETER_NAME);
            if (isNull(streamName) || streamName.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            // Find the stream in Live application default instance and create a JSON response object
            val result = Optional.ofNullable(host.getApplication("live"))
                .map(application -> application.getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME))
                .map(instance -> instance.getStreams().getStream(streamName))
                .map(stream -> streamInfo(stream, host))
                .map(JSONObject::toString)
                .orElse(emptyStreamInfo().toJSONString());

            // Send the object to the client
            writeResponse(response, 200, result, APPLICATION_JSON);
        } catch (Exception e) {
            writeResponse(response, 500, e.getMessage());
        }
    }
}
