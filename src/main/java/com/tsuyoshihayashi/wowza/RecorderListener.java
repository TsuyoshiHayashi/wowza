package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.SegmentInfo;
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
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * @author Alexey Donov
 */
final class RecorderListener extends StreamRecorderActionNotifyBase {
    private static final WMSLogger logger = WMSLoggerFactory.getLogger(RecorderListener.class);
    private static final Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
    private static final Map<String, RecordSettings> streamRecordSettings = new HashMap<>();

    static @Nullable String uploadOverrideEndpoint = null;

    private static RecordSettings getRecordSettings(IStreamRecorder recorder) {
        return streamRecordSettings.get(recorder.getStreamName());
    }

    private static SegmentInfo getSegmentInfo(IStreamRecorder recorder) {
        return new SegmentInfo(recorder);
    }

    static File getRecordedFile(SegmentInfo segmentInfo) {
        return new File(segmentInfo.getCurrentFile());
    }

    static String createNewName(RecordSettings recordSettings, SegmentInfo segmentInfo) {
        final DateTime end = segmentInfo.getSegmentEndTime();
        final DateTime start = end.minus(segmentInfo.getSegmentDuration());

        final String newName = recordSettings.getFileNameFormat()
            .replaceAll("N", String.format("%d", segmentInfo.getSegmentNumber()))
            .replaceFirst("DD", String.format("%02d", start.getDayOfMonth()))
            .replaceFirst("HH", String.format("%02d", start.getHourOfDay()))
            .replaceFirst("II", String.format("%02d", start.getMinuteOfHour()))
            .replaceFirst("SS", String.format("%02d", start.getSecondOfMinute()))
            .replaceFirst("DD", String.format("%02d", end.getDayOfMonth()))
            .replaceFirst("HH", String.format("%02d", end.getHourOfDay()))
            .replaceFirst("II", String.format("%02d", end.getMinuteOfHour()))
            .replaceFirst("SS", String.format("%02d", end.getSecondOfMinute()));

        return String.format("%s/%s", segmentInfo.getStoragePath(), newName);
    }

    private static File renameFile(File oldFile, String newFileName) {
        final File newFile = new File(newFileName);

        if (!oldFile.renameTo(newFile)) {
            throw new RuntimeException("Could not move file");
        }

        return newFile;
    }

    private static String uploadFile(File file, RecordSettings settings) {
        final FormDataMultiPart form = new FormDataMultiPart();
        form.field("hash", settings.getHash());
        form.field("hash2", settings.getHash2());
        form.field("title", file.getName());
        form.field("comment", "");
        form.bodyPart(new FileDataBodyPart("video_file", file, new MediaType("video", "mp4")));

        String endpoint = Optional.ofNullable(uploadOverrideEndpoint).orElse(settings.getUploadURL());

        logger.info(String.format("Uploading segment %s to %s", file.getName(), endpoint));

        try {
            final String response = client.target(endpoint)
                .request()
                .header("Referer", settings.getReferer())
                .post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA_TYPE))
                .readEntity(String.class);
            if (!file.delete()) {
                logger.warn(String.format("Could not delete %s", file));
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    public void onStopRecorder(IStreamRecorder recorder) {
        streamRecordSettings.remove(recorder.getStreamName());
    }

    @Override
    public void onSegmentEnd(IStreamRecorder recorder) {
        final SegmentInfo segmentInfo = getSegmentInfo(recorder);
        final CompletableFuture<SegmentInfo> segmentInfoFuture = completedFuture(segmentInfo);

        final RecordSettings recordSettings = getRecordSettings(recorder);
        final CompletableFuture<RecordSettings> recordSettingsFuture = completedFuture(recordSettings);

        runAsync(() -> logger.info("Segment finished"));

        final CompletableFuture<File> oldFileFuture = segmentInfoFuture.thenApply(RecorderListener::getRecordedFile);
        final CompletableFuture<String> newFileNameFuture = recordSettingsFuture.thenCombine(segmentInfoFuture, RecorderListener::createNewName);
        final CompletableFuture<File> newFileFuture = oldFileFuture.thenCombine(newFileNameFuture, RecorderListener::renameFile);
        newFileFuture.thenCombineAsync(recordSettingsFuture, RecorderListener::uploadFile)
            .thenAcceptAsync(response -> logger.info(String.format("Upload response: %s", response)));
    }
}
