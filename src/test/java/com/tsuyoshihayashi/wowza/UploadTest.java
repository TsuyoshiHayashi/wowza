package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.tsuyoshihayashi.model.SegmentInfo;
import junit.framework.TestCase;

import java.io.File;

/**
 * @author Alexey Donov
 */
public class UploadTest extends TestCase {
    private static final String FILE_NAME_FORMAT = "";
    private static final long LIMIT_MINUTES = 10;
    private static final String ORIGINAL_FILE_PATH = "";
    private static final String ORIGINAL_FILE_NAME = "";
    private static final String NEW_FILE_NAME = "";

    public void testGetRecordedFile() {
        final SegmentInfo segmentInfo = new SegmentInfo(10, 0, ORIGINAL_FILE_PATH, ORIGINAL_FILE_NAME);
        final File originalFile = new File(ORIGINAL_FILE_NAME);
        final File recordedFile = RecorderListener.getRecordedFile(segmentInfo);

        assertEquals(originalFile, recordedFile);
    }

    public void testCreateNewName() {
        final RecordSettings recordSettings = new RecordSettings(FILE_NAME_FORMAT, LIMIT_MINUTES, "", "", "", "");
        final SegmentInfo segmentInfo = new SegmentInfo(10, 0, ORIGINAL_FILE_PATH, ORIGINAL_FILE_NAME);
        final String newName = RecorderListener.createNewName(recordSettings, segmentInfo);

        assertEquals(NEW_FILE_NAME, newName);
    }

    public void testUploadFile() {

    }
}
