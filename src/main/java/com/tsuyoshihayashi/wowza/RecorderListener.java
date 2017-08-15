package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderActionNotifyBase;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.joda.time.DateTime;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;

/**
 * @author Alexey Donov
 */
final class RecorderListener extends StreamRecorderActionNotifyBase {
    private final WMSLogger logger = WMSLoggerFactory.getLogger(RecorderListener.class);

    private final Client client;

    private static final Map<String, RecordSettings> streamRecordSettings = new HashMap<>();

    RecorderListener() {
        client = ClientBuilder.newBuilder()
            .register(MultiPartFeature.class)
            .build();
    }

    @Override
    public void onStartRecorder(IStreamRecorder recorder) {
        final IMediaStream stream = recorder.getStream();
        if (stream == null) {
            logger.warn("No recorder stream");
            return;
        }

        final WMSProperties properties = stream.getProperties();
        final RecordSettings settings = (RecordSettings) properties.getProperty(RECORD_SETTINGS_KEY);

        streamRecordSettings.put(recorder.getStreamName(), settings);
    }

    @Override
    public void onSegmentEnd(IStreamRecorder recorder) {
        final RecordSettings settings = streamRecordSettings.get(recorder.getStreamName());

        final DateTime start = recorder.getStartTime();
        final DateTime end = new DateTime();

        final File oldFile = new File(recorder.getCurrentFile());
        final String newFileName = settings.getFileNameFormat()
            .replaceAll("N", String.format("%d", recorder.getSegmentNumber()))
            .replaceFirst("DD", String.format("%02d", start.getDayOfMonth()))
            .replaceFirst("HH", String.format("%02d", start.getHourOfDay()))
            .replaceFirst("II", String.format("%02d", start.getMinuteOfHour()))
            .replaceFirst("SS", String.format("%02d", start.getSecondOfMinute()))
            .replaceFirst("DD", String.format("%02d", end.getDayOfMonth()))
            .replaceFirst("HH", String.format("%02d", end.getHourOfDay()))
            .replaceFirst("II", String.format("%02d", end.getMinuteOfHour()))
            .replaceFirst("SS", String.format("%02d", end.getSecondOfMinute()));

        final File newFile = new File(recorder.getAppInstance().getStreamStoragePath(), newFileName);

        logger.info(String.format("Moving %s to %s", oldFile.getAbsolutePath(), newFile.getAbsolutePath()));

        if (oldFile.renameTo(newFile)) {
            final FormDataMultiPart form = new FormDataMultiPart();
            form.field("hash", settings.getHash());
            form.field("hash2", settings.getHash2());
            form.field("title", "");
            form.field("comment", "");
            form.bodyPart(new FileDataBodyPart("video_file", newFile));

            logger.info(String.format("Uploading %s to %s", newFile.getName(), settings.getUploadURL()));

            final String response = client.target(settings.getUploadURL())
                .request()
                .post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA_TYPE))
                .readEntity(String.class);
            logger.info(String.format("Upload response: %s", response));

            if (!newFile.delete()) {
                logger.warn(String.format("Couldn't delete %s", newFile.getAbsolutePath()));
            }
        } else {
            logger.warn("Couldn't move");
        }
    }
}
