package com.tsuyoshihayashi.model;

import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import org.joda.time.DateTime;

/**
 * Object that stores recorded segment information to be sent to API
 *
 * @author Alexey Donov
 */
public final class SegmentInfo {
    private final DateTime segmentEndTime;
    private final long segmentDuration;
    private final int segmentNumber;
    private final String storagePath;
    private final String currentFile;

    public SegmentInfo(DateTime segmentEndTime, long segmentDuration, int segmentNumber, String storagePath, String currentFile) {
        super();

        this.segmentEndTime = segmentEndTime;
        this.segmentDuration = segmentDuration;
        this.segmentNumber = segmentNumber;
        this.storagePath = storagePath;
        this.currentFile = currentFile;
    }

    /**
     * Create an instance from Stream recorder object information
     *
     * @param recorder Stream recorder
     */
    public SegmentInfo(IStreamRecorder recorder) {
        this(new DateTime(), recorder.getSegmentDuration(), recorder.getSegmentNumber(), recorder.getAppInstance().getStreamStoragePath(), recorder.getCurrentFile());
    }

    public DateTime getSegmentEndTime() {
        return segmentEndTime;
    }

    public long getSegmentDuration() {
        return segmentDuration;
    }

    public int getSegmentNumber() {
        return segmentNumber;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getCurrentFile() {
        return currentFile;
    }
}
