package com.tsuyoshihayashi.wowza;

import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.LiveStreamRecordManagerActionNotifyBase;
import org.jetbrains.annotations.NotNull;

/**
 * Object that adds a record listener to new recorders
 *
 * @author Alexey Donov
 */
final class RecordManagerListener extends LiveStreamRecordManagerActionNotifyBase {
    private final @NotNull RecorderListener recorderListener = new RecorderListener();

    @Override
    public void onCreateRecord(IStreamRecorder recorder) {
        recorder.addListener(recorderListener);
    }
}
