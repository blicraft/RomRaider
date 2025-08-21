package com.romraider.io.protocol.ncs.iso15765;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import com.romraider.logger.ecu.definition.Module;
import com.romraider.logger.ecu.exception.InvalidResponseException;

public class NCSProtocolTest {

    private Module createModule() {
        return new Module(
                "ECU",
                new byte[] {0x00, 0x00, 0x07, (byte) 0xE8},
                "dummy",
                new byte[] {0x00, 0x00, 0x07, (byte) 0xE0},
                false);
    }

    @Test
    public void constructsResetRequest() {
        NCSProtocol protocol = new NCSProtocol();
        Module module = createModule();
        byte[] request = protocol.constructEcuResetRequest(module, 0x01);
        assertArrayEquals(new byte[] {0x00, 0x00, 0x07, (byte) 0xE0, 0x11, 0x01}, request);
    }

    @Test
    public void acceptsValidResetResponse() {
        NCSProtocol protocol = new NCSProtocol();
        Module module = createModule();
        protocol.constructEcuResetRequest(module, 0x01);
        byte[] response = new byte[] {0x00, 0x00, 0x07, (byte) 0xE8, 0x51, 0x01};
        protocol.checkValidEcuResetResponse(response);
    }

    @Test(expected = InvalidResponseException.class)
    public void rejectsInvalidResetResponse() {
        NCSProtocol protocol = new NCSProtocol();
        Module module = createModule();
        protocol.constructEcuResetRequest(module, 0x01);
        byte[] response = new byte[] {0x00, 0x00, 0x07, (byte) 0xE8, 0x51, 0x02};
        protocol.checkValidEcuResetResponse(response);
    }
}
