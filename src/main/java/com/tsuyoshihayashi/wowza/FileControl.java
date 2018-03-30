package com.tsuyoshihayashi.wowza;

import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.vhost.IVHost;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Object that handles requests about files present in the content directory
 *
 * Listing files
 * http://hostname:1935/filectrl?a=list
 *
 * Manually deleting a file
 * http://hostname:1935/filectrl?a=delete&f=filename.mp4
 *
 * @author Alexey Donov
 */
public final class FileControl extends Control {
    private static final String ACTION_LIST = "list";
    private static final String ACTION_DELETE = "delete";
    private static final String FILE_PARAMETER_NAME = "f";

    private static final String CONTENT_ROOT = "/usr/local/WowzaStreamingEngine/content";

    private final @NotNull WMSLogger logger = WMSLoggerFactory.getLogger(FileControl.class);

    /**
     * Create a single file response entry
     *
     * @param file File in the content directory
     * @return JSON object with name, size and date information
     */
    @SuppressWarnings("unchecked")
    private @NotNull JSONObject fileData(@NotNull File file) {
        val result = new JSONObject();
        result.put("name", file.getName());
        result.put("size", file.length());
        result.put("date", file.lastModified() / 1000);

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onHTTPRequest(IVHost host, IHTTPRequest request, IHTTPResponse response) {
        try {
            // Ensure this is a GET request
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

            switch (action) {
                case ACTION_LIST:
                    // List of the files in the content directory
                    val root = new File(CONTENT_ROOT);
                    val files = new JSONArray();

                    // Find all the files, create a JSON entry for each one and combine them into an array
                    Optional.ofNullable(root.listFiles(File::isFile))
                        .ifPresent(fileArray -> Stream.of(fileArray)
                            .map(this::fileData)
                            .forEach(files::add));

                    // Send the array to the client
                    writeResponse(response, 200, files.toJSONString(), APPLICATION_JSON);
                    return;

                case ACTION_DELETE:
                    // Delete a single file
                    // Ensure that file name paramter is present in the request
                    val fileName = request.getParameter(FILE_PARAMETER_NAME);
                    if (fileName == null || fileName.isEmpty()) {
                        writeBadRequestResponse(response);
                        return;
                    }

                    // Delete the file
                    val file = new File(CONTENT_ROOT.concat("/").concat(fileName));
                    if (file.exists()) {
                        logger.info(String.format("Deleting file %s by request", file.getName()));
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }

                    writeOkResponse(response);
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
