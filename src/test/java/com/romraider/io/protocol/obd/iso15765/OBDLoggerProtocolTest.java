package com.romraider.io.protocol.obd.iso15765;

import static com.romraider.util.HexUtil.asBytes;
import static com.romraider.util.HexUtil.asHex;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.romraider.logger.ecu.comms.query.EcuQuery;
import com.romraider.logger.ecu.definition.LoggerData;

public class OBDLoggerProtocolTest {

    private static class DummyQuery implements EcuQuery {
        private final String[] addresses;
        private final byte[] bytes;
        private final String hex;

        DummyQuery(String... addresses) {
            this.addresses = addresses;
            int len = 0;
            for (String addr : addresses) {
                len += asBytes(addr).length;
            }
            byte[] tmp = new byte[len];
            int pos = 0;
            for (String addr : addresses) {
                byte[] b = asBytes(addr);
                System.arraycopy(b, 0, tmp, pos, b.length);
                pos += b.length;
            }
            this.bytes = tmp;
            this.hex = asHex(tmp);
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
            return hex;
        }

        @Override
        public void setResponse(byte[] bytes) {
        }

        @Override
        public LoggerData getLoggerData() {
            return null;
        }

        @Override
        public double getResponse() {
            return 0;
        }
    }

    @Test
    public void convertsMultiByteAddresses() throws Exception {
        OBDLoggerProtocol protocol = new OBDLoggerProtocol();
        DummyQuery q1 = new DummyQuery("0x1234", "0x5678");
        DummyQuery q2 = new DummyQuery("0x9A");

        Collection<EcuQuery> queries = Arrays.asList(q1, q2);
        Method m = OBDLoggerProtocol.class.getDeclaredMethod("convertToByteAddresses", Collection.class);
        m.setAccessible(true);
        byte[][] result = (byte[][]) m.invoke(protocol, queries);

        assertEquals(3, result.length);
        assertArrayEquals(new byte[]{0x12, 0x34}, result[0]);
        assertArrayEquals(new byte[]{0x56, 0x78}, result[1]);
        assertArrayEquals(new byte[]{(byte) 0x9A}, result[2]);
    }
}
