package com.tsuyoshihayashi.wowza;

import com.wowza.wms.livestreamrecord.manager.IStreamRecorder;
import com.wowza.wms.livestreamrecord.manager.LiveStreamRecordManagerActionNotifyBase;

/**
 * @author Alexey Donov
 */
final class RecordListener extends LiveStreamRecordManagerActionNotifyBase {
    @Override
    public void onSplitRecord(IStreamRecorder recorder) {

    }

    @Override
    public void onStopRecord(IStreamRecorder recorder) {

    }
}
