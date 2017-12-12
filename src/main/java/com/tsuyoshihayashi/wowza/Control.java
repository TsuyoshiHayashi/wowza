package com.tsuyoshihayashi.wowza;

import com.wowza.wms.http.HTTPProvider2Base;
import com.wowza.wms.http.IHTTPResponse;

import java.io.IOException;

/**
 * @author Alexey Donov
 */
abstract class Control extends HTTPProvider2Base {
    static final String ACTION_PARAMETER_NAME = "a";

    void writeResponse(IHTTPResponse response, int code, String body, String contentType) {
        response.setResponseCode(code);
        response.setHeader("Content-Type", contentType);
        if (body != null) {
            try {
                response.getOutputStream().write(body.getBytes());
                response.getOutputStream().close();
            } catch (IOException ignore) {

            }
        }
    }

    void writeResponse(IHTTPResponse response, int code, String body) {
        writeResponse(response, code, body, "text/plain");
    }

    void writeBadRequestResponse(IHTTPResponse response) {
        writeResponse(response, 400, null);
    }
}
