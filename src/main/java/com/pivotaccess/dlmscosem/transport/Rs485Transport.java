package com.pivotaccess.dlmscosem.transport;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * RS-485 serial transport backed by jSerialComm.
 *
 * <p>RS-485 is half-duplex — only one device transmits at a time. This class
 * handles the transmit/receive direction switching via the RTS (Request To
 * Send) pin when a manual RTS delay is configured. Many modern USB-to-RS485
 * adapters and RS485-capable UARTs handle direction switching automatically in
 * hardware (or via the kernel {@code TIOCSRS485} flag), in which case the RTS
 * delay can be left at zero.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * TransportConfig cfg = TransportConfig.rs485("/dev/ttyUSB0", 9600)
 *         .withReadTimeoutMs(5000)
 *         .withRs485RtsDelayMs(2);
 *
 * try (Transport t = new Rs485Transport(cfg)) {
 *     t.open();
 *     t.write(new byte[]{0x7e, ...});
 *     byte[] response = t.read(256, 3000);
 * }
 * }</pre>
 */
public class Rs485Transport implements Transport {

    private static final Logger log = LoggerFactory.getLogger(Rs485Transport.class);

    private final TransportConfig config;
    private SerialPort            port;

    public Rs485Transport(TransportConfig config) {
        if (config.getType() != TransportConfig.Type.RS485) {
            throw new IllegalArgumentException(
                    "TransportConfig type must be RS485, got: " + config.getType());
        }
        this.config = config;
    }

    // -------------------------------------------------------------------------
    // Transport
    // -------------------------------------------------------------------------

    @Override
    public void open() throws TransportException {
        if (isOpen()) {
            log.warn("Transport already open: {}", describe());
            return;
        }

        log.debug("Opening RS-485 port: {}", describe());

        port = SerialPort.getCommPort(config.getPortName());

        port.setBaudRate(config.getBaudRate());
        port.setNumDataBits(config.getDataBits());
        port.setNumStopBits(config.getStopBits());
        port.setParity(config.getParity());

        // Timeout mode: semi-blocking — returns when data available or timeout
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                config.getReadTimeoutMs(),
                0);

        if (!port.openPort()) {
            port = null;
            throw new TransportException(
                    "Failed to open serial port: " + config.getPortName());
        }

        // Flush any stale data in OS buffers
        port.flushIOBuffers();

        log.info("RS-485 port opened: {}", describe());
    }

    @Override
    public void close() {
        if (port != null && port.isOpen()) {
            port.closePort();
            log.info("RS-485 port closed: {}", describe());
        }
        port = null;
    }

    @Override
    public void write(byte[] data) throws TransportException {
        assertOpen();

        if (log.isTraceEnabled()) {
            log.trace("TX [{}] → {} bytes: {}", describe(), data.length, hex(data));
        }

        // Assert RTS (transmit direction) if delay is configured
        if (config.getRtsDelayMs() > 0) {
            port.setRTS();
            sleep(config.getRtsDelayMs());
        }

        int written = port.writeBytes(data, data.length);

        if (written != data.length) {
            throw new TransportException(String.format(
                    "Write incomplete on %s: expected %d bytes, wrote %d",
                    describe(), data.length, written));
        }

        // Wait for all bytes to physically leave the UART TX buffer
        // before releasing RTS (de-asserting back to receive)
        if (config.getRtsDelayMs() > 0) {
            // Drain: sleep proportional to bytes sent at current baud
            // bits per byte = 1 start + 8 data + 1 stop = 10
            int drainMs = (data.length * 10 * 1000) / config.getBaudRate() + 1;
            sleep(drainMs);
            port.clearRTS();
        }
    }

    @Override
    public byte[] read(int maxBytes, int timeoutMs) throws TransportException {
        assertOpen();

        // Override per-read timeout if different from the configured default
        if (timeoutMs != config.getReadTimeoutMs()) {
            port.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    timeoutMs,
                    0);
        }

        byte[] buffer = new byte[maxBytes];
        int    bytesRead = port.readBytes(buffer, maxBytes);

        if (bytesRead < 0) {
            throw new TransportException("Read error on: " + describe());
        }

        byte[] result = (bytesRead == maxBytes)
                ? buffer
                : Arrays.copyOf(buffer, bytesRead);

        if (log.isTraceEnabled() && bytesRead > 0) {
            log.trace("RX [{}] ← {} bytes: {}", describe(), bytesRead, hex(result));
        }

        return result;
    }

    @Override
    public boolean isOpen() {
        return port != null && port.isOpen();
    }

    @Override
    public String describe() {
        return config.toString();   // e.g. "/dev/ttyUSB0@9600"
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertOpen() throws TransportException {
        if (!isOpen()) {
            throw new TransportException("Transport is not open: " + describe());
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}