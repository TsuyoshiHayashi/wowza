package com.tsuyoshihayashi.wowza;

import com.wowza.wms.http.HTTPProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.vhost.IVHost;

import java.io.IOException;

/**
 * @author Alexey Donov
 */
public final class FileControl extends HTTPProvider2Base {
    private static final String ACTION_PARAMETER_NAME = "a";
    private static final String ACTION_LIST = "list";
    private static final String ACTION_DELETE = "delete";

    private void writeResponse(IHTTPResponse response, int code, String body) {
        response.setResponseCode(code);
        if (body != null) {
            try {
                response.getOutputStream().write(body.getBytes());
                response.getOutputStream().close();
            } catch (IOException ignore) { }
        }
    }

    private void writeResponse(IHTTPResponse response, int code) {
        writeResponse(response, code, null);
    }

    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        if (!"GET".equals(request.getMethod())) {
            writeResponse(response, 400);
            return;
        }

        final String action = request.getParameter(ACTION_PARAMETER_NAME);
        if (action == null || action.isEmpty()) {
            writeResponse(response, 400);
            return;
        }

        switch (action) {
            case ACTION_LIST:
                // TODO: Collect files and make json array
                break;

            case ACTION_DELETE:
                // TODO: Delete single file
                break;

            default:
                response.setResponseCode(400);
                break;
        }

        writeResponse(response, 200, "{\"ok\": true}");
    }
}
