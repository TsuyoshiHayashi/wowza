package com.tsuyoshihayashi.wowza;

import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.vhost.IVHost;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @author Alexey Donov
 */
public final class FileControl extends Control {
    private static final String ACTION_LIST = "list";
    private static final String ACTION_DELETE = "delete";
    private static final String FILE_PARAMETER_NAME = "f";

    private static final String CONTENT_ROOT = "/usr/local/WowzaStreamingEngine/content";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(FileControl.class);

    @SuppressWarnings("unchecked")
    private JSONObject fileData(File file) {
        final JSONObject result = new JSONObject();
        result.put("name", file.getName());
        result.put("size", file.length());
        result.put("date", file.lastModified() / 1000);

        return result;
    }

    @SuppressWarnings("unchecked")
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

            switch (action) {
                case ACTION_LIST:
                    final File root = new File(CONTENT_ROOT);
                    final JSONArray files = new JSONArray();
                    Optional.ofNullable(root.listFiles(File::isFile))
                        .ifPresent(fileArray -> Stream.of(fileArray)
                            .map(this::fileData)
                            .forEach(files::add));
                    writeResponse(response, 200, files.toJSONString(), APPLICATION_JSON);
                    return;

                case ACTION_DELETE:
                    final String fileName = request.getParameter(FILE_PARAMETER_NAME);
                    if (fileName == null || fileName.isEmpty()) {
                        writeBadRequestResponse(response);
                        return;
                    }
                    final File file = new File(CONTENT_ROOT.concat("/").concat(fileName));
                    if (file.exists()) {
                        logger.info(String.format("Deleting file %s by request", file.getName()));
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                    writeResponse(response, 200, "{\"ok\": true}", APPLICATION_JSON);
                    return;

                default:
                    writeBadRequestResponse(response);
                    break;
            }
        } catch (Exception e) {
            writeResponse(response, 500, e.getMessage());
        }
    }
}
