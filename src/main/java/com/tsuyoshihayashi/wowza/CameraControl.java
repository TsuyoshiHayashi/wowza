package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.AliasProvider;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.vhost.IVHost;

/**
 * @author Alexey Donov
 */
public final class CameraControl extends Control {
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String STREAM_PARAMETER_NAME = "name";
    private static final String IP_PARAMETER_NAME = "ip";
    private static final String TYPE_PARAMETER_NAME = "type";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(CameraControl.class);

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        try {
            if (!"GET".equals(request.getMethod())) {
                writeBadRequestResponse(response);
                return;
            }

            final String action = request.getParameter(ACTION_PARAMETER_NAME);
            if (action == null || action.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            final String stream = request.getParameter(STREAM_PARAMETER_NAME);
            if (stream == null || stream.isEmpty()) {
                writeBadRequestResponse(response);
                return;
            }

            final IApplication application = host.getApplication("live");
            final IApplicationInstance instance = application.getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);

            switch (action) {
                case ACTION_START:
                    final String ip = request.getParameter(IP_PARAMETER_NAME);
                    if (ip == null || ip.isEmpty()) {
                        writeBadRequestResponse(response);
                        return;
                    }
                    final String type = request.getParameter(TYPE_PARAMETER_NAME);
                    if (type == null || type.isEmpty()) {
                        writeBadRequestResponse(response);
                        return;
                    }

                    logger.info(String.format("Starting camera stream=%s ip=%s type=%s", stream, ip, type));

                    AliasProvider.instance().setCameraIP(stream, ip, type);
                    instance.startMediaCasterStream(stream, "rtp");
                    break;

                case ACTION_STOP:
                    logger.info(String.format("Stopping camera stream=%s", stream));

                    instance.stopMediaCasterStream(stream);
                    break;

                default:
                    writeBadRequestResponse(response);
                    return;
            }

            writeResponse(response, 200, "{\"ok\": true}", "application/json");
        } catch (Exception e) {
            writeResponse(response, 500, e.getMessage());
        }
    }
}
