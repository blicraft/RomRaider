package com.romraider.io.protocol.obd.iso15765;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.lang.reflect.Method;

import org.junit.Test;

import com.romraider.logger.ecu.comms.query.EcuQuery;
import com.romraider.logger.ecu.definition.LoggerData;
import com.romraider.util.HexUtil;

public class OBDLoggerProtocolTest {

    private static class StubQuery implements EcuQuery {
        private final String[] addresses;
        private final byte[] bytes;

        StubQuery(String... addresses) {
            this.addresses = addresses;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (String addr : addresses) {
                try {
                    bos.write(HexUtil.asBytes(addr));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            this.bytes = bos.toByteArray();
        }

        @Override
        public LoggerData getLoggerData() {
            return null;
        }

        @Override
        public double getResponse() {
            return 0;
        }

        @Override
        public String[] getAddresses() {
            return addresses;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String getHex() {
            return HexUtil.asHex(bytes);
        }

        @Override
        public void setResponse(byte[] bytes) {
        }
    }

    @Test
    public void testConvertToByteAddressesHandlesVariableLengths() throws Exception {
        OBDLoggerProtocol protocol = new OBDLoggerProtocol();
        Collection<EcuQuery> queries = Arrays.<EcuQuery>asList(
                new StubQuery("0x1", "0x2345"),
                new StubQuery("0x67"),
                new StubQuery("0x89AB"));
        Method m = OBDLoggerProtocol.class.getDeclaredMethod("convertToByteAddresses", Collection.class);
        m.setAccessible(true);
        byte[][] addresses = (byte[][]) m.invoke(protocol, queries);
        assertArrayEquals(new byte[]{0x01}, addresses[0]);
        assertArrayEquals(new byte[]{0x23, 0x45}, addresses[1]);
        assertArrayEquals(new byte[]{0x67}, addresses[2]);
        assertArrayEquals(new byte[]{(byte) 0x89, (byte) 0xAB}, addresses[3]);
    }
}
