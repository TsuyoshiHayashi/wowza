package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.SegmentInfo;
import com.tsuyoshihayashi.model.RecordSettings;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderActionNotifyBase;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import lombok.val;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.tsuyoshihayashi.wowza.StreamConstants.RECORD_SETTINGS_KEY;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Object that listens to events in stream recorder
 *
 * @author Alexey Donov
 */
final class RecorderListener extends StreamRecorderActionNotifyBase {
    private static final WMSLogger logger = WMSLoggerFactory.getLogger(RecorderListener.class);
    private static final Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
    private static final Map<String, RecordSettings> streamRecordSettings = new HashMap<>();

    static @Nullable String uploadOverrideEndpoint = null;

    /**
     * Get the record settings from a stream being recorded
     *
     * @param recorder Stream recorder
     * @return RecordSettings object
     */
    private static @Nullable RecordSettings getRecordSettings(@NotNull IStreamRecorder recorder) {
        return streamRecordSettings.get(recorder.getStreamName());
    }

    /**
     * Get the current segment information from the recorder
     *
     * @param recorder Stream recorder
     * @return SegmentInfo object
     */
    private static @NotNull SegmentInfo getSegmentInfo(@NotNull IStreamRecorder recorder) {
        return new SegmentInfo(recorder);
    }

    /**
     * Get the file object for the recorded segment
     *
     * @param segmentInfo Segment information
     * @return File object
     */
    static @NotNull File getRecordedFile(@NotNull SegmentInfo segmentInfo) {
        return new File(segmentInfo.getCurrentFile());
    }

    /**
     * Create a file name based on the rules received from API
     *
     * @param recordSettings Record settings
     * @param segmentInfo Current segment information
     * @return File name
     */
    static @NotNull String createNewName(@NotNull RecordSettings recordSettings, @NotNull SegmentInfo segmentInfo) {
        val end = segmentInfo.getSegmentEndTime();
        val start = end.minus(segmentInfo.getSegmentDuration());

        val newName = recordSettings.getFileNameFormat()
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

    /**
     * Renames the file from a temporary name to a final name
     *
     * @param oldFile Temporary file
     * @param newFileName Final file name
     * @return File object with a final name
     */
    private static @NotNull File renameFile(@NotNull File oldFile, @NotNull String newFileName) {
        val newFile = new File(newFileName);

        if (!oldFile.renameTo(newFile)) {
            throw new RuntimeException("Could not move file");
        }

        return newFile;
    }

    /**
     * Uploads a recorded file
     *
     * @param file File object
     * @param settings RecordSettings object
     * @return Response from API as a string
     */
    private static @NotNull String uploadFile(@NotNull File file, @NotNull RecordSettings settings) {
        if (settings.getUploadURL() == null) {
            return String.format("No upload URL for [%s], skipping upload", file);
        }

        val form = new FormDataMultiPart();
        form.field("hash", settings.getHash());
        form.field("hash2", settings.getHash2());
        form.field("title", Optional.ofNullable(settings.getTitle()).orElse(file.getName()));
        Optional.ofNullable(settings.getComment())
            .ifPresent(comment -> form.field("comment", comment));
        form.bodyPart(new FileDataBodyPart("video_file", file, new MediaType("video", "mp4")));

        val endpoint = Optional.ofNullable(uploadOverrideEndpoint).orElse(settings.getUploadURL());

        logger.info(String.format("Uploading segment %s to %s", file.getName(), endpoint));

        try {
            val response = client.target(endpoint)
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

    // StreamRecorderActionNotify

    /**
     * When the record is started, save the record settings locally
     *
     * @param recorder Stream recorder
     */
    @Override
    public void onStartRecorder(IStreamRecorder recorder) {
        val stream = recorder.getStream();
        if (stream == null) {
            logger.warn("No recorder stream");
            return;
        }

        val properties = stream.getProperties();
        val settings = (RecordSettings) properties.getProperty(RECORD_SETTINGS_KEY);

        streamRecordSettings.put(recorder.getStreamName(), settings);
    }

    /**
     * When the record is stopped, the record settings are no longer needed
     *
     * @param recorder Stream recorder
     */
    @Override
    public void onStopRecorder(IStreamRecorder recorder) {
        streamRecordSettings.remove(recorder.getStreamName());
    }

    /**
     * When a segment is finished recording, rename the file and upload it
     *
     * @param recorder Stream recorder
     */
    @Override
    public void onSegmentEnd(IStreamRecorder recorder) {
        // Fetch the segment information
        val segmentInfo = getSegmentInfo(recorder);
        val segmentInfoFuture = completedFuture(segmentInfo);

        // Fetch the record settings
        val recordSettings = getRecordSettings(recorder);
        val recordSettingsFuture = completedFuture(recordSettings);

        runAsync(() -> logger.info("Segment finished"));

        // Fetch the temporary file
        val oldFileFuture = segmentInfoFuture.thenApply(RecorderListener::getRecordedFile);

        // Generate final file name according to the rules
        val newFileNameFuture = recordSettingsFuture.thenCombine(segmentInfoFuture, RecorderListener::createNewName);

        // Rename the file
        val newFileFuture = oldFileFuture.thenCombine(newFileNameFuture, RecorderListener::renameFile);

        // Upload it and log the response
        newFileFuture.thenCombineAsync(recordSettingsFuture, RecorderListener::uploadFile)
            .thenAcceptAsync(response -> logger.info(String.format("Upload response: %s", response)));
    }
}
