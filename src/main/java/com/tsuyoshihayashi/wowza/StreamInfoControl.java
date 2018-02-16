package com.tsuyoshihayashi.wowza;

import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;
import org.json.simple.JSONObject;

import java.util.Optional;

import static java.util.Objects.isNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Object that handles requests about stream bitrate information
 *
 * @author Alexey Donov
 */
public class StreamInfoControl extends Control {
    private static final String STREAM_NAME_PARAMETER_NAME = "s";

    /**
     * Create a JSON entry about the stream, containing
     * video, audio and total bitrates.
     *
     * @param stream Stream
     * @return JSON object
     */
    @SuppressWarnings("unchecked")
    private JSONObject streamInfo(IMediaStream stream) {
        final JSONObject obj = new JSONObject();
        obj.put("total_bitrate", stream.getPublishBitrateAudio() + stream.getPublishBitrateVideo());
        obj.put("video_bitrate", stream.getPublishBitrateVideo());
        obj.put("audio_bitrate", stream.getPublishBitrateAudio());

        return obj;
    }

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        try {
            // Ensure that this is a GET request
            if (!"GET".equals(request.getMethod())) {
                writeBadRequestResponse(response);
                return;
            }

            // Ensure that stream name parameter is present in the request
            final String streamName = request.getParameter(STREAM_NAME_PARAMETER_NAME);
            if (isNull(streamName) || streamName.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            // Find the stream in Live application default instance and create a JSON response object
            final String result = Optional.ofNullable(host.getApplication("live"))
                .map(application -> application.getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME))
                .map(instance -> instance.getStreams().getStream(streamName))
                .map(this::streamInfo)
                .map(JSONObject::toString)
                .orElse(null);

            // Send the object to the client
            writeResponse(response, 200, result, APPLICATION_JSON);
        } catch (Exception e) {
            writeResponse(response, 500, e.getMessage());
        }
    }
}
