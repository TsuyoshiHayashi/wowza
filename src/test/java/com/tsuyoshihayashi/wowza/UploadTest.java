package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.RecordSettings;
import com.tsuyoshihayashi.model.SegmentInfo;
import junit.framework.TestCase;
import lombok.val;
import org.joda.time.DateTime;

import java.io.File;

/**
 * Testing recorded file name generation
 *
 * @author Alexey Donov
 */
public class UploadTest extends TestCase {
    private static final String FILE_NAME_FORMAT = "test!2017_08_25_13_37_16-N-DD_HH_II_SS-DD_HH_II_SS.mp4";
    private static final long LIMIT_MINUTES = 10;
    private static final String ORIGINAL_FILE_PATH = "/Users/alexey/Movies";
    private static final String ORIGINAL_FILE_NAME = "Blue-HD.mp4";

    public void testGetRecordedFile() {
        val now = new DateTime();
        val segmentInfo = new SegmentInfo(now, 10000, 0, ORIGINAL_FILE_PATH, ORIGINAL_FILE_PATH.concat("/").concat(ORIGINAL_FILE_NAME));
        val originalFile = new File(ORIGINAL_FILE_PATH, ORIGINAL_FILE_NAME);
        val recordedFile = RecorderListener.getRecordedFile(segmentInfo);

        assertEquals(originalFile, recordedFile);
    }

    public void testReplace() {
        val origin = "obs!2017_08_15_15_25_36-N-DD_HH_II_SS-DD_HH_II_SS.mp4";

        val newFileName = origin
            .replaceAll("N", "" + 1)
            .replaceFirst("DD", "" + 10)
            .replaceFirst("HH", "" + 20)
            .replaceFirst("II", "" + 30)
            .replaceFirst("SS", "" + 40)
            .replaceFirst("DD", "" + 50)
            .replaceFirst("HH", "" + 60)
            .replaceFirst("II", "" + 70)
            .replaceFirst("SS", "" + 80);

        assertEquals("obs!2017_08_15_15_25_36-1-10_20_30_40-50_60_70_80.mp4", newFileName);
    }

    public void testCreateNewName() {
        val end = new DateTime();
        val start = end.minus(10000);

        val expected = ORIGINAL_FILE_PATH.concat("/").concat(FILE_NAME_FORMAT
            .replaceAll("N", String.format("%d", 0))
            .replaceFirst("DD", String.format("%02d", start.getDayOfMonth()))
            .replaceFirst("HH", String.format("%02d", start.getHourOfDay()))
            .replaceFirst("II", String.format("%02d", start.getMinuteOfHour()))
            .replaceFirst("SS", String.format("%02d", start.getSecondOfMinute()))
            .replaceFirst("DD", String.format("%02d", end.getDayOfMonth()))
            .replaceFirst("HH", String.format("%02d", end.getHourOfDay()))
            .replaceFirst("II", String.format("%02d", end.getMinuteOfHour()))
            .replaceFirst("SS", String.format("%02d", end.getSecondOfMinute())));

        val recordSettings = new RecordSettings(FILE_NAME_FORMAT, LIMIT_MINUTES, false, "", "", "", "", "", "");
        val segmentInfo = new SegmentInfo(end, 10000, 0, ORIGINAL_FILE_PATH, ORIGINAL_FILE_PATH.concat("/").concat(ORIGINAL_FILE_NAME));
        val actual = RecorderListener.createNewName(recordSettings, segmentInfo);

        assertEquals(expected, actual);
    }
}
