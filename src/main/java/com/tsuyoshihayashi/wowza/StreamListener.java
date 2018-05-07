package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.api.RecordSettingsEndpoint;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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

    private final @NotNull WMSLogger logger = WMSLoggerFactory.getLogger(StreamListener.class);

    private final @NotNull IApplicationInstance instance;
    private final @Nullable String pushHost;
    private final @Nullable String pushApp;

    private final @NotNull RecordSettingsEndpoint recordSettingsEndpoint;

    StreamListener(@NotNull IApplicationInstance instance) {
        super();

        this.instance = instance;

        val instanceProperties = instance.getProperties();
        this.pushHost = instanceProperties.getPropertyStr(PUSH_HOST_KEY);
        this.pushApp = instanceProperties.getPropertyStr(PUSH_APP_KEY);

        val host = instance.getVHost();
        val hostProperties = host.getProperties();

        val apiEndpoint = hostProperties.getPropertyStr(API_ENDPOINT_KEY);
        val uploadReferer = hostProperties.getPropertyStr(UPLOAD_REFERER_KEY, "");
        recordSettingsEndpoint = new RecordSettingsEndpoint(apiEndpoint, uploadReferer);

        logger.info(String.format("API Endpoint: %s", apiEndpoint));
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
            .thenApply(recordSettingsEndpoint::getRecordSettings)
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
                    publisher.setDstApplicationName(Optional.ofNullable(pushApp).orElse("live"));
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
