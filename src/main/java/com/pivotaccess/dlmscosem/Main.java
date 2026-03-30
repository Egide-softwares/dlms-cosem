package com.pivotaccess.dlmscosem;
import com.pivotaccess.dlmscosem.apdu.AarqApdu;
import com.pivotaccess.dlmscosem.apdu.ApduException;
import com.pivotaccess.dlmscosem.cosem.CosemClient;
import com.pivotaccess.dlmscosem.cosem.CosemClientConfig;
import com.pivotaccess.dlmscosem.cosem.CosemClientException;
import com.pivotaccess.dlmscosem.cosem.ObisCode;
import com.pivotaccess.dlmscosem.cosem.objects.*;
import com.pivotaccess.dlmscosem.transport.TransportConfig;
import com.pivotaccess.dlmscosem.transport.TransportConfig.FramingMode;
import com.pivotaccess.dlmscosem.util.MeterAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("\nStarting DLMS/COSEM  (TCP + HDLC)...");
        CosemClientConfig configHDLC = CosemClientConfig.builder()
                .transport(TransportConfig.tcp("localhost", 4060, FramingMode.HDLC))
                .serverAddress(MeterAddress.hdlcServer(1))
                .clientAddress(MeterAddress.hdlcClient(16))
                .authentication(AarqApdu.AuthenticationLevel.NONE, null)
                .maxReceivePduSize(1024)
                .timeoutMs(5_000)
                .maxRetries(2)
                .build();
        try (CosemClient client = new CosemClient(configHDLC)) {
            readOrSetValues(client);
        } catch (CosemClientException ex) {
            log.error("CosemClientException (TCP + HDLC): {}", ex.getMessage());
        } catch (ApduException e) {
            log.error("ApduException (TCP + HDLC): {}", e.getMessage());
        }

        log.info("\nStarting DLMS/COSEM (TCP + WRAPPER)...");
        CosemClientConfig configWrapper = CosemClientConfig.builder()
                .transport(TransportConfig.tcp("localhost", 4070, FramingMode.WRAPPER))
                .serverAddress(MeterAddress.wrapperServer(1))
                .clientAddress(MeterAddress.wrapperClient(16))
                .authentication(AarqApdu.AuthenticationLevel.NONE, null)
                .maxReceivePduSize(1024)
                .timeoutMs(2_000)
                .build();
        try (CosemClient client = new CosemClient(configWrapper)) {
            readOrSetValues(client);
        } catch (CosemClientException ex) {
            log.error("CosemClientException (TCP + WRAPPER): {}", ex.getMessage());
        } catch (ApduException e) {
            log.error("ApduException (TCP + WRAPPER): {}", e.getMessage());
        } finally {
            log.info("\nDLMS/COSEM client finished.");
        }

        log.info("\nStarting DLMS/COSEM (SERIAL + HDLC)...");
        CosemClientConfig serialHdlc = CosemClientConfig.builder()
                .transport(TransportConfig.rs485("/dev/ttyUSB0", 9600))
                .serverAddress(MeterAddress.hdlcServer(1))
                .clientAddress(MeterAddress.hdlcClient(16))
                .authentication(AarqApdu.AuthenticationLevel.NONE, null)
                .maxReceivePduSize(1024)
                .timeoutMs(2_000)
                .build();
        try (CosemClient client = new CosemClient(serialHdlc)) {
            readOrSetValues(client);
        } catch (CosemClientException ex) {
            log.error("CosemClientException (SERIAL + HDLC): {}", ex.getMessage());
        } catch (ApduException e) {
            log.error("ApduException (SERIAL + HDLC): {}", e.getMessage());
        } finally {
            log.info("\nDLMS/COSEM client finished.");
        }
    }

    private static void readOrSetValues(CosemClient client) throws CosemClientException, ApduException {
        client.open();

        try {
            CosemDataObject serialNumber = client.getData(ObisCode.SERIAL_NUMBER);
            log.info("Serial Number: {}", serialNumber.getValue().getOctetStringAsText(StandardCharsets.UTF_8));
        } catch (CosemClientException ignored) {
            log.info("Serial Number: NA");
        }

        try {
            CosemDataObject logicalDeviceName = client.getData(ObisCode.of("0.0.42.0.0.255"));
            log.info("Logical Device Name: {}", logicalDeviceName.getValue().getOctetStringAsText(StandardCharsets.UTF_8));
        } catch (CosemClientException ignored) {
            log.info("Logical Device Name: NA");
        }

        try {
            CosemDataObject invocationCounter = client.getData(ObisCode.of("0.0.43.1.2.255"));
            log.info("Invocation Counter: {}", invocationCounter.getValue().getLong());
        } catch (CosemClientException ignored) {
            log.info("Invocation Counter: NA");
        }

        try {
            ClockObject clock = client.getClock();
            log.info("Current time: {}", clock.getTime().format(DateTimeFormatter.ISO_DATE_TIME));
        } catch (CosemClientException ignored) {
            log.info("Current time: NA");
        }

        try {
            RegisterObject activeEnergyImport = client.getRegister(ObisCode.ACTIVE_ENERGY_IMPORT);
            log.info("Active Energy Import: {} {}", activeEnergyImport.getPhysicalValue(), activeEnergyImport.getUnitSymbol());
        } catch (CosemClientException ignored) {
            log.info("Active Energy Import: NA");
        }

        try {
            RegisterObject activeEnergyExport = client.getRegister(ObisCode.ACTIVE_ENERGY_EXPORT);
            log.info("Active Energy Export: {} {}", activeEnergyExport.getPhysicalValue(), activeEnergyExport.getUnitSymbol());
        } catch (CosemClientException ignored) {
            log.info("Active Energy Export: NA");
        }

        try {
            RegisterObject voltageL1 = client.getRegister(ObisCode.VOLTAGE_L1);
            log.info("Voltage L1: {} {}", voltageL1.getPhysicalValue(), voltageL1.getUnitSymbol());
        } catch (CosemClientException ignored) {
            log.info("Voltage L1: NA");
        }

        try {
            RegisterObject voltageL2 = client.getRegister(ObisCode.VOLTAGE_L2);
            log.info("Voltage L2: {} {}", voltageL2.getPhysicalValue(), voltageL2.getUnitSymbol());
        } catch (CosemClientException ignored) {
            log.info("Voltage L2: NA");
        }

        try {
            RegisterObject voltageL3 = client.getRegister(ObisCode.VOLTAGE_L3);
            log.info("Voltage L3: {} {}", voltageL3.getPhysicalValue(), voltageL3.getUnitSymbol());
        } catch (CosemClientException ignored) {
            log.info("Voltage L3: NA");
        }

        client.close();
    }
}
