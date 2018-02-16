package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.AliasProvider;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.mediacaster.MediaCasterStreamItem;
import com.wowza.wms.vhost.IVHost;

/**
 * Object that handles requests to start/stop RTSP camera streaming
 *
 * @author Alexey Donov
 */
public final class CameraControl extends Control {
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String STREAM_PARAMETER_NAME = "s";
    private static final String URL_PARAMETER_NAME = "c";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(CameraControl.class);

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        try {
            // Ensure that this is a GET request
            if (!"GET".equals(request.getMethod())) {
                writeBadRequestResponse(response);
                return;
            }

            // Ensure that the action parameter is present in the request
            final String action = request.getParameter(ACTION_PARAMETER_NAME);
            if (action == null || action.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            // Ensure that the stream name parameter is present in the request
            final String streamName = request.getParameter(STREAM_PARAMETER_NAME);
            if (streamName == null || streamName.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            final IApplication application = host.getApplication("live");
            final IApplicationInstance instance = application.getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);

            switch (action) {
                case ACTION_START:
                    // Ensure that the camera RTSP URL parameter is present in the request
                    final String url = request.getParameter(URL_PARAMETER_NAME);
                    if (url == null || url.isEmpty()) {
                        writeBadRequestResponse(response);
                        return;
                    }

                    // If there is already a stream with this name, stop it
                    instance.getMediaCasterStreams()
                        .getMediaCasterStreamItems()
                        .stream()
                        .map(MediaCasterStreamItem::getMediaCasterId)
                        .filter(streamName::equals)
                        .findAny()
                        .ifPresent(instance::stopMediaCasterStream);

                    // Set the RTSP URL for the stream name
                    AliasProvider.instance().setCameraURL(streamName, url);

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
