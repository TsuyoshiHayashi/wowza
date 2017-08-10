package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.livestreamrecord.manager.ILiveStreamRecordManager;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.MediaStreamActionNotifyBase;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Alexey Donov
 */
final class StreamListener extends MediaStreamActionNotifyBase {
    private static final String API_ENDPOINT_KEY = "apiEndpoint";
    private static final String API_STREAM_NAME_PARAMETER_NAME = "n";
    private static final String RECORD_SETTINGS_KEY = "RECORD_SETTINGS";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(StreamListener.class);
    private final JSONParser parser = new JSONParser();
    private final Client client = ClientBuilder.newClient();

    private final IApplicationInstance instance;
    private final String apiEndpoint;

    StreamListener(IApplicationInstance instance) {
        super();

        this.instance = instance;

        final IApplication application = instance.getApplication();
        final WMSProperties properties = application.getProperties();
        this.apiEndpoint = properties.getPropertyStr(API_ENDPOINT_KEY);
    }

    private RecordSettings getRecordSettings(IMediaStream stream) {
        try {
            final String responseText = client.target(apiEndpoint)
                .queryParam(API_STREAM_NAME_PARAMETER_NAME, stream.getName())
                .request()
                .get(String.class);

            final JSONObject response = (JSONObject) parser.parse(responseText);

            return new RecordSettings(response);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    // IMediaStreamActionNotify

    @Override
    public void onPublish(IMediaStream stream, String name, boolean record, boolean append) {
        completedFuture(stream)
            .thenApplyAsync(this::getRecordSettings)
            .thenAcceptAsync(settings -> {
                stream.getProperties().setProperty(RECORD_SETTINGS_KEY, settings);

                final ILiveStreamRecordManager manager = instance.getVHost().getLiveStreamRecordManager();
                final StreamRecorderParameters parameters = new StreamRecorderParameters(instance);
                parameters.fileFormat = IStreamRecorderConstants.FORMAT_MP4;
                parameters.segmentationType = IStreamRecorderConstants.SEGMENT_BY_DURATION;
                parameters.segmentDuration = settings.getLimit();
                parameters.startOnKeyFrame = true;
                parameters.recordData = true;
                parameters.outputPath = instance.getStreamStoragePath();
                // TODO: File name format

                manager.startRecording(instance, name, parameters);
            });
    }

    @Override
    public void onUnPublish(IMediaStream stream, String name, boolean record, boolean append) {
        final ILiveStreamRecordManager manager = instance.getVHost().getLiveStreamRecordManager();
        manager.stopRecording(instance, name);
    }
}
