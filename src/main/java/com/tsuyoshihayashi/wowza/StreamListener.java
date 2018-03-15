package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.application.*;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.pushpublish.protocol.rtmp.PushPublishRTMP;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.MediaStreamActionNotifyBase;
import lombok.val;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import java.util.Optional;

import static com.tsuyoshihayashi.model.RecordSettings.fromJSON;
import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Object that listens to the events in the streams
 *
 * @author Alexey Donov
 */
final class StreamListener extends MediaStreamActionNotifyBase {
    private static final String API_ENDPOINT_KEY = "apiEndpoint";
    private static final String UPLOAD_REFERER_KEY = "uploadReferer";
    private static final String PUSH_HOST_KEY = "pushHost";
    private static final String PUSH_APP_KEY = "pushApp";
    private static final String PUBLISHER_PROPERTY_NAME = "publisher";
    private static final String API_STREAM_NAME_PARAMETER_NAME = "n";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(StreamListener.class);
    private final JSONParser parser = new JSONParser();
    private final Client client = ClientBuilder.newClient();

    private final IApplicationInstance instance;
    private final String apiEndpoint;
    private final String uploadReferer;
    private final String pushHost;
    private final String pushApp;

    StreamListener(IApplicationInstance instance) {
        super();

        this.instance = instance;

        val properties = instance.getProperties();

        this.apiEndpoint = properties.getPropertyStr(API_ENDPOINT_KEY);
        this.uploadReferer = properties.getPropertyStr(UPLOAD_REFERER_KEY, "");
        this.pushHost = properties.getPropertyStr(PUSH_HOST_KEY);
        this.pushApp = properties.getPropertyStr(PUSH_APP_KEY);

        logger.info(String.format("API Endpoint: %s", apiEndpoint));
    }

    /**
     * Ask API to return record settings for the stream
     *
     * @param stream Stream object
     * @return Record settings object
     */
    private RecordSettings getRecordSettings(IMediaStream stream) {
        try {
            val responseText = client.target(apiEndpoint)
                .queryParam(API_STREAM_NAME_PARAMETER_NAME, stream.getName())
                .request()
                .get(String.class);

            logger.info(String.format("API Response: %s", responseText));

            val response = (JSONObject) parser.parse(responseText);
            val settings = fromJSON(response, uploadReferer);

            logger.info(String.format("Record settings: autostart '%s', name '%s', split on %d minutes, upload to '%s'", settings.isAutoRecord(), settings.getFileNameFormat(), settings.getLimit(), settings.getUploadURL()));

            return settings;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // IMediaStreamActionNotify

    /**
     * When the stream is published, fetch the record settings from API and process according to them.
     * The stream is also pushed to the publish host.
     *
     * @param stream Stream object
     * @param name Stream name
     * @param record Is stream is being recorded (per Wowza settings)
     * @param append Is record is being appended to an existing file (per Wowza settings)
     */
    @Override
    public void onPublish(IMediaStream stream, String name, boolean record, boolean append) {
        completedFuture(stream)
            // Fetch the record settings
            .thenApply(this::getRecordSettings)
            .thenAccept(settings -> {
                // Store the settings in the stream object
                stream.getProperties().setProperty(RECORD_SETTINGS_KEY, settings);

                if (settings.isAutoRecord()) {
                    // If the stream recording should start immediately, do so
                    val parameters = new StreamRecorderParameters(instance);
                    parameters.fileFormat = IStreamRecorderConstants.FORMAT_MP4;
                    parameters.segmentationType = IStreamRecorderConstants.SEGMENT_BY_DURATION;
                    parameters.segmentDuration = settings.getLimit() * 60 * 1000;
                    parameters.startOnKeyFrame = true;
                    parameters.recordData = true;
                    parameters.outputPath = instance.getStreamStoragePath();

                    instance.getVHost().getLiveStreamRecordManager().startRecording(instance, name, parameters);
                }
            }).whenComplete((v, t) -> logger.error(t.getMessage()));

        // Pushing the stream to the publish host
        Optional.ofNullable(pushHost)
            .ifPresent(host -> {
                logger.info(String.format("Pushing %s stream to %s", stream.getName(), host));
                try {
                    val publisher = new PushPublishRTMP();
                    publisher.setAppInstance(instance);
                    publisher.setSrcStream(stream);
                    publisher.setHost(host);
                    publisher.setDstApplicationName(pushApp == null ? "live" : pushApp);
                    publisher.setDstAppInstanceName(ApplicationInstance.DEFAULT_APPINSTANCE_NAME);
                    publisher.setDstStreamName(stream.getName());
                    publisher.setConnectionFlashVerion(PushPublishRTMP.CURRENTFLASHVERSION);
                    publisher.setSendOriginalTimecodes(true);
                    publisher.setOriginalTimecodeThreshold(0x100000);
                    publisher.setSendFCPublish(true);
                    publisher.setSendReleaseStream(true);
                    publisher.setSendOnMetadata(true);
                    publisher.setDebugPackets(false);

                    publisher.connect();
                    stream.getProperties().setProperty(PUBLISHER_PROPERTY_NAME, publisher);
                } catch (LicensingException e) {
                    logger.error("No license for PushPublish");
                }
            });
    }

    /**
     * When the stream publishing stops, the recorder is stopped (if there was one)
     * and pushing to publish host is stopped.
     *
     * @param stream Stream object
     * @param name Stream name
     * @param record Is stream being recorded
     * @param append Is record being appended to an existing file
     */
    @Override
    public void onUnPublish(IMediaStream stream, String name, boolean record, boolean append) {
        instance.getVHost().getLiveStreamRecordManager().stopRecording(instance, name);

        Optional.ofNullable(stream.getProperties().getProperty(PUBLISHER_PROPERTY_NAME))
            .map(PushPublishRTMP.class::cast)
            .ifPresent(publisher -> {
                publisher.disconnect();
                stream.getProperties().remove(PUBLISHER_PROPERTY_NAME);
            });
    }
}
