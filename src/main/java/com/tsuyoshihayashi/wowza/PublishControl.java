package com.tsuyoshihayashi.wowza;

import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;
import org.json.simple.JSONArray;

import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Object that handles requests about the list of active streams
 *
 * @author Alexey Donov
 */
public class PublishControl extends Control {
    private static final String ACTION_LIST = "list";

    private Collection<String> getStreamNames(Collection<IMediaStream> streams) {
        return streams.stream()
            .map(IMediaStream::getName)
            .collect(toList());
    }

    private JSONArray getJSONArray(Collection<String> strings) {
        final JSONArray result = new JSONArray();
        // noinspection unchecked
        result.addAll(strings);

        return result;
    }

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        // Ensure that this is a GET request
        if (!"GET".equals(request.getMethod())) {
            writeBadRequestResponse(response);
            return;
        }

        // Ensure that action parameter is present in the request
        final String action = request.getParameter(ACTION_PARAMETER_NAME);
        if (action == null || !singletonList(ACTION_LIST).contains(action)) {
            writeBadRequestResponse(response);
            return;
        }

        try {
            switch (action) {
                case ACTION_LIST:
                    final String result = Optional.ofNullable(host.getApplication("live"))
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
