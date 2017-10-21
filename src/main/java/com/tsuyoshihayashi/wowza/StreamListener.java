package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.MediaStreamActionNotifyBase;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static com.tsuyoshihayashi.model.RecordSettings.fromJSON;
import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Alexey Donov
 */
final class StreamListener extends MediaStreamActionNotifyBase {
    private static final String API_ENDPOINT_KEY = "apiEndpoint";
    private static final String UPLOAD_REFERER_KEY = "uploadReferer";
    private static final String API_STREAM_NAME_PARAMETER_NAME = "n";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(StreamListener.class);
    private final JSONParser parser = new JSONParser();
    private final Client client = ClientBuilder.newClient();

    private final IApplicationInstance instance;
    private final String apiEndpoint;
    private final String uploadReferer;

    StreamListener(IApplicationInstance instance) {
        super();

        this.instance = instance;

        final WMSProperties properties = instance.getProperties();

        this.apiEndpoint = properties.getPropertyStr(API_ENDPOINT_KEY);
        this.uploadReferer = properties.getPropertyStr(UPLOAD_REFERER_KEY, "");

        logger.info(String.format("API Endpoint: %s", apiEndpoint));
    }

    private RecordSettings getRecordSettings(IMediaStream stream) {
        try {
            final String responseText = client.target(apiEndpoint)
                .queryParam(API_STREAM_NAME_PARAMETER_NAME, stream.getName())
                .request()
                .get(String.class);

            logger.info(String.format("API Response: %s", responseText));

            final JSONObject response = (JSONObject) parser.parse(responseText);
            final RecordSettings settings = fromJSON(response, uploadReferer);

            logger.info(String.format("Record settings: autostart '%s', name '%s', split on %d minutes, upload to '%s'", settings.isAutoRecord(), settings.getFileNameFormat(), settings.getLimit(), settings.getUploadURL()));

            return settings;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // IMediaStreamActionNotify

    @Override
    public void onPublish(IMediaStream stream, String name, boolean record, boolean append) {
        completedFuture(stream)
            .thenApply(this::getRecordSettings)
            .thenAccept(settings -> {
                stream.getProperties().setProperty(RECORD_SETTINGS_KEY, settings);

                if (settings.isAutoRecord()) {
                    final StreamRecorderParameters parameters = new StreamRecorderParameters(instance);
                    parameters.fileFormat = IStreamRecorderConstants.FORMAT_MP4;
                    parameters.segmentationType = IStreamRecorderConstants.SEGMENT_BY_DURATION;
                    parameters.segmentDuration = settings.getLimit() * 60 * 1000;
                    parameters.startOnKeyFrame = true;
                    parameters.recordData = true;
                    parameters.outputPath = instance.getStreamStoragePath();

                    instance.getVHost().getLiveStreamRecordManager().startRecording(instance, name, parameters);
                }
            }).whenComplete((v, t) -> logger.error(t.getMessage()));
    }

    @Override
    public void onUnPublish(IMediaStream stream, String name, boolean record, boolean append) {
        instance.getVHost().getLiveStreamRecordManager().stopRecording(instance, name);
    }
}
