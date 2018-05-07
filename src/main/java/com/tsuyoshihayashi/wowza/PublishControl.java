package com.tsuyoshihayashi.wowza;

import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;

import java.util.Collection;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Object that handles requests about the list of active streams
 *
 * List of active streams
 * http://hostname:1936/publishctrl?a=list
 *
 * @author Alexey Donov
 */
public final class PublishControl extends Control {
    private static final String ACTION_LIST = "list";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(PublishControl.class);

    private @NotNull Collection<String> getStreamNames(@NotNull Collection<IMediaStream> streams) {
        return streams.stream()
            .map(IMediaStream::getName)
            .collect(toList());
    }

    private @NotNull JSONArray getJSONArray(@NotNull Collection<String> strings) {
        val result = new JSONArray();
        // noinspection unchecked
        result.addAll(strings);

        return result;
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

        try {
            switch (action) {
                case ACTION_LIST:
                    val result = Optional.ofNullable(host.getApplication("live"))
                        .map(application -> application.getAppInstance(ApplicationInstance.DEFAULT_APPINSTANCE_NAME))
                        .map(instance -> instance.getStreams().getStreams())
                        .map(this::getStreamNames)
                        .map(this::getJSONArray)
                        .map(JSONArray::toString)
                        .orElse(null);
                    writeResponse(response, 200, result, APPLICATION_JSON);
                    break;

                default:
                    writeBadRequestResponse(response);
            }
        } catch (Exception e) {
            writeResponse(response, 500, e.getMessage());
        }
    }
}
