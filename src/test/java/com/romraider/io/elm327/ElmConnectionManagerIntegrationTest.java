package com.romraider.io.elm327;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.romraider.io.connection.ConnectionProperties;
import com.romraider.io.connection.SerialConnectionProperties;
import com.romraider.io.elm327.ElmConnectionManager.ERROR_TYPE;

public class ElmConnectionManagerIntegrationTest {

    private static class StubElmConnectionManager extends ElmConnectionManager {
        private final Map<String,String> responses;
        StubElmConnectionManager(Map<String,String> responses) {
            super("test", new SerialConnectionProperties(9600,8,1,0,0,0));
            this.responses = responses;
        }
        @Override
        protected ElmConnection createConnection(String portName, int baudrate) {
            return null;
        }
        @Override
        public void send(String command) {
        }
        @Override
        public String sendAndWaitForChar(String command, int timeout, String c) {
            String resp = responses.get(command);
            return resp == null ? "" : resp;
        }
        @Override
        public void clearLine() {
        }
        @Override
        public void close() {
        }
    }

    @Test
    public void testCanProtocolSearchResponse() {
        Map<String,String> responses = new HashMap<String,String>();
        responses.put("AT PC", "OK");
        responses.put("AT Z", "ELM327 v1.5");
        responses.put("AT E0", "OK");
        responses.put("ATSH F1", "OK");
        responses.put("ATSP 6", "OK");
        responses.put("0100", "SEARCHING... 41 00 BE 3F B8 13");
        ElmConnectionManager m = new StubElmConnectionManager(responses);
        ERROR_TYPE err = m.resetAndInit("iso15765", "00", "F1");
        assertEquals(ERROR_TYPE.NO_ERROR, err);
        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(1,3,4,5,6,7,11,12,13,14,15,16,17,19,20,21,28,31,32));
        assertEquals(expected, m.getSupportedPids());
    }

    @Test
    public void testKwpProtocolDifferentFirmware() {
        Map<String,String> responses = new HashMap<String,String>();
        responses.put("AT PC", "OK");
        responses.put("AT Z", "ELM327 v2.1");
        responses.put("AT E0", "OK");
        responses.put("ATIIA 10", "OK");
        responses.put("ATSH 8210F1", "OK");
        responses.put("ATSP 4", "OK");
        responses.put("0100", "41 00 80 00 00 00");
        ElmConnectionManager m = new StubElmConnectionManager(responses);
        ERROR_TYPE err = m.resetAndInit("iso14230-4kwp-5", "10", "F1");
        assertEquals(ERROR_TYPE.NO_ERROR, err);
        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(1));
        assertEquals(expected, m.getSupportedPids());
    }

    @Test
    public void testNoDataResponse() {
        Map<String,String> responses = new HashMap<String,String>();
        responses.put("AT PC", "OK");
        responses.put("AT Z", "ELM327 v1.5");
        responses.put("AT E0", "OK");
        responses.put("ATSH F1", "OK");
        responses.put("ATSP 6", "OK");
        responses.put("0100", "SEARCHING... NO DATA");
        ElmConnectionManager m = new StubElmConnectionManager(responses);
        ERROR_TYPE err = m.resetAndInit("iso15765", "00", "F1");
        assertEquals(ERROR_TYPE.ECU_NOT_FOUND, err);
        assertTrue(m.getSupportedPids().isEmpty());
    }
}
