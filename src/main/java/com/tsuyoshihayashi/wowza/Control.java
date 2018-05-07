package com.tsuyoshihayashi.wowza;

import com.wowza.wms.http.HTTPProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.io.IOException;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Base class for the request handling subclasses for easier response generation
 *
 * @author Alexey Donov
 */
abstract class Control extends HTTPProvider2Base {
    /**
     * Action parameter is used in all request handlers
     */
    static final String ACTION_PARAMETER_NAME = "a";

    void logRequest(@NotNull IHTTPRequest request, @NotNull WMSLogger logger) {
        logger.info(String.format("Request registered: %s %s, params: %s", request.getMethod(), request.getPath(), request.getParameterMap()));
    }

    void writeResponse(@NotNull IHTTPResponse response, int code, @Nullable String body, @NotNull String contentType) {
        response.setResponseCode(code);
        response.setHeader("Content-Type", contentType);
        if (body != null) {
            try {
                response.getOutputStream().write(body.getBytes());
                response.getOutputStream().close();
            } catch (IOException ignore) {
                // No op
            }
        }
    }

    void writeResponse(@NotNull IHTTPResponse response, int code, @Nullable String body) {
        writeResponse(response, code, body, "text/plain");
    }

    void writeOkResponse(@NotNull IHTTPResponse response) {
        writeResponse(response, 200, new JSONObject(singletonMap("ok", true)).toJSONString(), APPLICATION_JSON);
    }

    void writeBadRequestResponse(@NotNull IHTTPResponse response) {
        writeResponse(response, 400, null);
    }
}
