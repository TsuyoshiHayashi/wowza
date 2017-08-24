package com.tsuyoshihayashi.model;

import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;

/**
 * @author Alexey Donov
 */
public final class SegmentInfo {
    private final long segmentDuration;
    private final int segmentNumber;
    private final String storagePath;
    private final String currentFile;

    public SegmentInfo(long segmentDuration, int segmentNumber, String storagePath, String currentFile) {
        super();

        this.segmentDuration = segmentDuration;
        this.segmentNumber = segmentNumber;
        this.storagePath = storagePath;
        this.currentFile = currentFile;
    }

    public SegmentInfo(IStreamRecorder recorder) {
        this(recorder.getSegmentDuration(), recorder.getSegmentNumber(), recorder.getAppInstance().getStreamStoragePath(), recorder.getCurrentFile());
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
