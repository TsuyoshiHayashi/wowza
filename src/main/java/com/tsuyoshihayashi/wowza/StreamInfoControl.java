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
 * @author Alexey Donov
 */
public class StreamInfoControl extends Control {
    private static final String STREAM_NAME_PARAMETER_NAME = "s";

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
            if (!"GET".equals(request.getMethod())) {
                writeBadRequestResponse(response);
                return;
            }

            final String streamName = request.getParameter(STREAM_NAME_PARAMETER_NAME);
            if (isNull(streamName) || streamName.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            final String result = Optional.ofNullable(host.getApplication("live"))
                .map(application -> application.getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME))
                .map(instance -> instance.getStreams().getStream(streamName))
                .map(this::streamInfo)
                .map(JSONObject::toString)
                .orElse(null);

            writeResponse(response, 200, result, APPLICATION_JSON);
        } catch (Exception e) {
            writeResponse(response, 500, e.getMessage());
        }
    }
}
