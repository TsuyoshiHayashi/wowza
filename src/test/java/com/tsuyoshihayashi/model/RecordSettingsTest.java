package com.tsuyoshihayashi.model;

import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Test for the record settings object to ensure correct initialization from JSON
 *
 * @author Alexey Donov
 */
public class RecordSettingsTest extends TestCase {
    private static final JSONParser parser = new JSONParser();

    /**
     * Initialization from an empty JSON
     */
    public void testInsufficientFieldSet() {
        try {
            RecordSettings.fromJSON((JSONObject) parser.parse("{}"), "");
            fail("Arguments are missing");
        } catch (ParseException ignore) {
            fail("Must be parseable");
        } catch (IllegalArgumentException ignore) {
            // OK
        }
    }

    /**
     * Initialization from a JSON with minimal set of parameters
     */
    public void testMinimalFieldSet() {
        try {
            RecordSettings.fromJSON((JSONObject) parser.parse("{\"record_name\":\"\", \"limit\":0, \"hash\":\"hash\", \"hash2\":\"hash2\"}"), "");
        } catch (ParseException ignore) {
            fail("Must be parseable");
        } catch (IllegalArgumentException ignore) {
            fail("All arguments are there");
        }
    }

    /**
     * Test manual record start parameter
     */
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

    /**
     * Test automatic record start parameter
     */
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
