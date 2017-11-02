package com.tsuyoshihayashi.model;

import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Alexey Donov
 */
public class RecordSettingsTest extends TestCase {
    private static final JSONParser parser = new JSONParser();

    public void testMinimalFieldSet() {
        try {
            RecordSettings.fromJSON((JSONObject) parser.parse("{\"record_name\":\"\", \"limit\":0, \"hash\":\"hash\", \"hash2\":\"hash2\"}"), "");
        } catch (ParseException ignore) {
            fail("Must be parseable");
        } catch (IllegalArgumentException ignore) {
            fail("All arguments are there");
        }
    }

    public void testManualStart() {
        try {
            RecordSettings rs;
            rs = RecordSettings.fromJSON((JSONObject) parser.parse("{\"record_name\":\"\", \"limit\":0, \"hash\":\"hash\", \"hash2\":\"hash2\", \"manual_start\":1}"), "");
            assertFalse(rs.isAutoRecord());
        } catch (ParseException ignore) {
            fail("Must be parseable");
        } catch (IllegalArgumentException ignore) {
            fail("All arguments are there");
        }
    }

    public void testAutoStart() {
        try {
            RecordSettings rs;
            rs = RecordSettings.fromJSON((JSONObject) parser.parse("{\"record_name\":\"\", \"limit\":0, \"hash\":\"hash\", \"hash2\":\"hash2\"}"), "");
            assertTrue("No manual_start means autostart", rs.isAutoRecord());
            rs = RecordSettings.fromJSON((JSONObject) parser.parse("{\"record_name\":\"\", \"limit\":0, \"hash\":\"hash\", \"hash2\":\"hash2\", \"manual_start\":0}"), "");
            assertTrue("manual_start == 0 means autostart", rs.isAutoRecord());
            rs = RecordSettings.fromJSON((JSONObject) parser.parse("{\"record_name\":\"\", \"limit\":0, \"hash\":\"hash\", \"hash2\":\"hash2\", \"manual_start\":\"false\"}"), "");
            assertTrue("manual_start == false means autostart", rs.isAutoRecord());
            rs = RecordSettings.fromJSON((JSONObject) parser.parse("{\"record_name\":\"\", \"limit\":0, \"hash\":\"hash\", \"hash2\":\"hash2\", \"manual_start\":\"true\"}"), "");
            assertTrue("manual_start == true means autostart", rs.isAutoRecord());
        } catch (ParseException ignore) {
            fail("Must be parseable");
        } catch (IllegalArgumentException ignore) {
            fail("All arguments are there");
        }
    }
}
