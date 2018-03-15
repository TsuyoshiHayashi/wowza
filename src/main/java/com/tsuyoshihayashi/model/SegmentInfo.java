package com.tsuyoshihayashi.model;

import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.joda.time.DateTime;

/**
 * Object that stores recorded segment information to be sent to API
 *
 * @author Alexey Donov
 */
@Data
@AllArgsConstructor
public final class SegmentInfo {
    private final DateTime segmentEndTime;
    private final long segmentDuration;
    private final int segmentNumber;
    private final String storagePath;
    private final String currentFile;

    /**
     * Create an instance from Stream recorder object information
     *
     * @param recorder Stream recorder
     */
    public SegmentInfo(IStreamRecorder recorder) {
        this(new DateTime(), recorder.getSegmentDuration(), recorder.getSegmentNumber(), recorder.getAppInstance().getStreamStoragePath(), recorder.getCurrentFile());
    }
}
