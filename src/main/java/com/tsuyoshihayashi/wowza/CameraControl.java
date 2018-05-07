package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.AliasProvider;
import com.tsuyoshihayashi.model.CameraInfo;
import com.tsuyoshihayashi.model.TextAction;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.mediacaster.MediaCasterStreamItem;
import com.wowza.wms.mediacaster.MediaCasterStreamMap;
import com.wowza.wms.vhost.IVHost;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Object that handles requests to start/stop RTSP camera streaming
 *
 * Starting a stream
 * http://hostname:1935/cameractrl?a=start&s=streamname&c=rtsp://camerahost/stream
 *
 * Stopping a stream
 * http://hostname:1936/cameractrl?a=stop&s=streamname
 *
 * @author Alexey Donov
 */
public final class CameraControl extends Control {
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String STREAM_PARAMETER_NAME = "s";
    private static final String URL_PARAMETER_NAME = "c";
    private static final String TITLE_PARAMETER_NAME = "title";
    private static final String COMMENT_PARAMETER_NAME = "comment";
    private static final String ACTION_TEXT_ACTION = "act";

    private final @NotNull WMSLogger logger = WMSLoggerFactory.getLogger(CameraControl.class);

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        logRequest(request, logger);

        try {
            logger.info("Received camera command with parameters: " + request.getParameterMap().toString());

            // Ensure that this is a GET request
            if (!"GET".equals(request.getMethod())) {
                writeBadRequestResponse(response);
                return;
            }

            // Ensure that the action parameter is present in the request
            val action = request.getParameter(ACTION_PARAMETER_NAME);
            if (action == null || action.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            // Ensure that the stream name parameter is present in the request
            val streamName = request.getParameter(STREAM_PARAMETER_NAME);
            if (streamName == null || streamName.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            val application = host.getApplication("live");
            val instance = application.getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);

            switch (action) {
                case ACTION_START:
                    // Ensure that the camera RTSP URL parameter is present in the request
                    val url = request.getParameter(URL_PARAMETER_NAME);
                    if (url == null || url.isEmpty()) {
                        writeBadRequestResponse(response);
                        return;
                    }

                    // If there is already a stream with this name, stop it
                    Optional.ofNullable(instance.getMediaCasterStreams())
                        .map(MediaCasterStreamMap::getMediaCasterStreamItems)
                        .map(List::stream)
                        .flatMap(stream -> stream.map(MediaCasterStreamItem::getMediaCasterId).filter(streamName::equals).findAny())
                        .ifPresent(instance::stopMediaCasterStream);

                    // Set the camera info object for the stream name
                    val title = request.getParameter(TITLE_PARAMETER_NAME);
                    val comment = request.getParameter(COMMENT_PARAMETER_NAME);
                    val textActionString = request.getParameter(ACTION_TEXT_ACTION);
                    TextAction textAction = null;
                    if (textActionString != null && !textActionString.isEmpty()) {
                        try {
                            textAction = TextAction.valueOf(textActionString);
                        } catch (IllegalArgumentException ignore) {
                            // No op
                        }
                    }
                    val info = new CameraInfo(url, title, comment, textAction);
                    AliasProvider.instance().setCameraInfo(streamName, info);

                    // Start the streaming
                    logger.info(String.format("Starting camera stream=%s url=%s", streamName, url));
                    instance.startMediaCasterStream(streamName, "rtp");
                    break;

                case ACTION_STOP:
                    logger.info(String.format("Stopping camera stream=%s", streamName));

                    // Stop the streaming
                    instance.stopMediaCasterStream(streamName);
                    break;

                default:
                    writeBadRequestResponse(response);
                    return;
            }

            writeOkResponse(response);
        } catch (Exception e) {
            writeResponse(response, 500, e.getMessage());
        }
    }
}
